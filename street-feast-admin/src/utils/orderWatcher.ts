import { collection, onSnapshot, FirestoreError } from "firebase/firestore";
import { db, storeId } from "./firebase";

export function watchOrders(storeIdParam: string = storeId) {
  const ref = collection(db, "stores", storeIdParam, "orders");
  return onSnapshot(
    ref,
    (snap) => {
      snap.docChanges().forEach((dc) => {
        try {
          const data = dc.doc.data();
          if (dc.type === "added") {
            console.log(`[OrderWatcher] New order detected: ${data.orderNumber || data.id || 'N/A'}`);
            window.electronAPI?.notify({ 
              title: "New order", 
              body: `Order #${data.orderNumber || data.id || 'N/A'}` 
            });
          }
          if (dc.type === "modified" && data.status === "Prepared") {
            console.log(`[OrderWatcher] Order prepared: ${data.orderNumber || data.id || 'N/A'}`);
            window.electronAPI?.notify({ 
              title: "Order prepared", 
              body: `Order #${data.orderNumber || data.id || 'N/A'}` 
            });
          }
        } catch (error) {
          console.error("[OrderWatcher] Error processing document change:", error);
        }
      });
    },
    (error: FirestoreError) => {
      console.error("[OrderWatcher] Firestore listener error:", error.code, error.message);
      // Firestore will automatically retry on transient errors
      // Don't crash the app, allow Firestore SDK to handle reconnection
    }
  );
}

