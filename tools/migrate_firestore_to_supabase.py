"""
One-off migration utility to copy Firestore order data into Supabase.

Usage:
    python tools/migrate_firestore_to_supabase.py \
        --firestore-credentials serviceAccount.json \
        --firestore-project street-feast \
        --supabase-url https://your-project.supabase.co \
        --supabase-service-key <service_role_key> \
        --store-id default
"""

import argparse
import datetime as dt
import json
import pathlib
import sys
from typing import Any, Dict, List

import firebase_admin
from firebase_admin import credentials, firestore
import requests


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Migrate order data from Firestore to Supabase.")
    parser.add_argument("--firestore-credentials", required=True, help="Path to Firebase service account JSON.")
    parser.add_argument("--firestore-project", required=True, help="Firebase project ID.")
    parser.add_argument("--supabase-url", required=True, help="Supabase project URL.")
    parser.add_argument("--supabase-service-key", required=True, help="Supabase service role key.")
    parser.add_argument("--store-id", required=True, help="Store ID to migrate.")
    parser.add_argument("--since", help="Only migrate orders updated after RFC3339 timestamp.")
    parser.add_argument("--dry-run", action="store_true", help="Print payloads without writing to Supabase.")
    return parser.parse_args()


def init_firestore(creds_path: str, project_id: str):
    app = firebase_admin.get_app() if firebase_admin._apps else None
    if app is None:
        cred = credentials.Certificate(creds_path)
        firebase_admin.initialize_app(cred, {"projectId": project_id})
    return firestore.client()


def fetch_orders(db, store_id: str, since: str | None = None):
    query = (
        db.collection("stores")
        .document(store_id)
        .collection("orders")
    )
    if since:
        try:
            parsed = dt.datetime.fromisoformat(since.replace("Z", "+00:00"))
            query = query.where("updatedAt", ">=", parsed)
        except ValueError:
            raise SystemExit(f"Invalid --since timestamp: {since}")
    docs = query.stream()
    orders = []
    for doc in docs:
        data = doc.to_dict()
        data["id"] = doc.id
        orders.append(data)
    return orders


def fetch_items(db, store_id: str, order_id: str):
    items_ref = (
        db.collection("stores")
        .document(store_id)
        .collection("orders")
        .document(order_id)
        .collection("orderItems")
    )
    return [
        dict(item.to_dict(), id=item.id)
        for item in items_ref.stream()
    ]


def map_order(order: Dict[str, Any], store_id: str) -> Dict[str, Any]:
    return {
        "id": order["id"],
        "store_id": store_id,
        "status": order.get("status", "Created"),
        "number": order.get("orderNumber"),
        "customer": order.get("customer"),
        "total": order.get("total"),
        "source": order.get("source"),
        "notes": order.get("notes"),
        "created_at": order.get("createdAt"),
        "updated_at": order.get("updatedAt"),
    }


def map_item(order_id: str, item: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "id": item["id"],
        "order_id": order_id,
        "name": item.get("nameSnapshot") or item.get("name"),
        "size": item.get("size"),
        "veg_flag": item.get("vegFlagSnapshot"),
        "quantity": item.get("qty", 1),
    }


def upsert_order(supabase_url: str, service_key: str, payload: Dict[str, Any], items: List[Dict[str, Any]]):
    rpc_payload = {
        "p_store_id": payload["store_id"],
        "p_order": payload,
        "p_items": items,
    }
    response = requests.post(
        f"{supabase_url}/rest/v1/rpc/orders_upsert",
        headers={
            "apikey": service_key,
            "Authorization": f"Bearer {service_key}",
            "Content-Type": "application/json",
            "Prefer": "return=representation",
        },
        data=json.dumps(rpc_payload),
        timeout=30,
    )
    if not response.ok:
        raise RuntimeError(f"Failed to upsert order {payload['id']}: {response.status_code} {response.text}")
    return response.json()


def main():
    args = parse_args()
    creds_path = pathlib.Path(args.firestore_credentials).expanduser()
    if not creds_path.exists():
        raise SystemExit(f"Service account file not found: {creds_path}")

    db = init_firestore(str(creds_path), args.firestore_project)
    orders = fetch_orders(db, args.store_id, args.since)
    print(f"Fetched {len(orders)} orders from Firestore")

    for order in orders:
        order_payload = map_order(order, args.store_id)
        items = fetch_items(db, args.store_id, order["id"])
        item_payloads = [map_item(order["id"], item) for item in items]

        if args.dry_run:
            print(json.dumps({"order": order_payload, "items": item_payloads}, indent=2, default=str))
        else:
            upsert_order(args.supabase_url, args.supabase_service_key, order_payload, item_payloads)
            print(f"Migrated order {order['id']}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Migration failed: {exc}", file=sys.stderr)
        sys.exit(1)


