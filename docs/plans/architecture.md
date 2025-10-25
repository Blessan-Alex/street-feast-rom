### ROMS – Technical Architecture (Mermaid-ready)

Paste into a Mermaid editor to export PNG.

## Context Diagram
```mermaid
flowchart LR
    Admin[Electron Admin] <--> Firestore[(Firebase Firestore)]
    Chef[Android Chef] <--> Firestore
    Waiter[Android Waiter] <--> Firestore
    Functions[Cloud Functions] <--> Firestore
    FCM[Firebase Cloud Messaging] <---> Chef
    FCM <---> Waiter
    Admin <--> Auth[Firebase Auth]
    Chef <--> Auth
    Waiter <--> Auth
    Storage[(Firebase Storage)] --- Admin
```

## Component Diagram
```mermaid
flowchart TB
    subgraph Electron Admin
      AUI[Admin UI]
      ASDK[Firebase JS SDK]
    end
    subgraph Android Chef
      CUI[Chef UI]
      CFSDK[Firebase Android SDK]
    end
    subgraph Android Waiter
      WUI[Waiter UI]
      WFSDK[Firebase Android SDK]
    end
    ASDK <--> Firestore[(Firestore)]
    CFSDK <--> Firestore
    WFSDK <--> Firestore
    ASDK <--> Auth[Auth]
    CFSDK <--> Auth
    WFSDK <--> Auth
    Functions[Cloud Functions] <--> Firestore
    Functions --> FCM[FCM]
    AUI --> Storage[(Storage)]
```

## Simplified ERD (logical)
```mermaid
erDiagram
    USERS ||--o{ ORDERS : creates
    ORDERS ||--|{ ORDER_ITEMS : contains
    USERS {
      string uid
      string email
      string role
      string name
      boolean active
    }
    MENU_ITEMS {
      string id
      string name
      string category
      number price
      boolean available
    }
    ORDERS {
      string orderId
      string mode
      string tableNumber
      string status
      number total
      timestamp createdAt
      timestamp updatedAt
      timestamp deliveredAt
      timestamp closedAt
      timestamp cancelledAt
      boolean archived
    }
    ORDER_ITEMS {
      string orderId
      string itemId
      string name
      number price
      number quantity
    }
```

## Status Transition Sequence
```mermaid
sequenceDiagram
    participant Admin as Admin (Electron)
    participant Chef as Chef (Android)
    participant Waiter as Waiter (Android)
    participant FS as Firestore
    participant FN as Functions
    participant FCM as FCM

    Admin->>FS: Create order (Created)
    FS-->>Chef: Listen (Created)
    FN-->>FCM: Push to Chef (Created)
    Chef->>FS: Update status Accepted
    FS-->>Admin: Snapshot (Accepted)
    Chef->>FS: Update status Prepared
    FS-->>Waiter: Snapshot (Prepared)
    FN-->>FCM: Push to Waiter (Prepared)
    Waiter->>FS: Update status Delivered
    FS-->>Admin: Snapshot (Delivered)
    Admin->>FS: Close OR Add More Items
    alt Close
        FS-->>FN: Closed with closedAt
        FN->>FS: Archive after 30m
    else Add More
        FS-->>Chef: Snapshot (Accepted)
        FN-->>FCM: Push to Chef & Waiter (Reopened)
    end
```

## Cancel Flow Sequence
```mermaid
sequenceDiagram
    participant Admin as Admin (Electron)
    participant FS as Firestore
    participant Chef as Chef (Android)
    participant Waiter as Waiter (Android)
    participant FN as Functions

    Admin->>FS: Update status Cancelled (cancelledAt)
    FS-->>Chef: Snapshot hide (not archived but excluded)
    FS-->>Waiter: Snapshot hide (not archived but excluded)
    FN->>FS: Archive after 30m
```

## Auto-Archive Job (Functions)
```mermaid
flowchart LR
    A[Scheduler every 5m] --> B[Query Closed/Cancelled older than 30m]
    B --> C[Set archived=true]
    C --> D[Hidden from all role queries]
```


