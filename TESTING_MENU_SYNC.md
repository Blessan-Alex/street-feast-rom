# Menu incremental sync testing checklist

- Cold start offline: app shows cached menu; no crashes; no network calls.
- Cold start online: single fetch; metadata updated; no repeated fetch within TTL.
- Realtime inserts/updates: category/item/frequent_items changes appear without full refetch.
- Realtime deletions: removed items/categories disappear; frequent items reorder correctly.
- Burst events: no ANR; fallback fetch debounced to one refresh per burst.
- Error paths: realtime failure triggers a single fallback refresh; app remains functional.



