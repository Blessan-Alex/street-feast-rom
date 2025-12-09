# Bug Report Template

Use this template to report bugs found during Phase 7 testing.

## Bug Report

**Bug ID:** [BUG-XXX]  
**Reported By:** [Name]  
**Reported Date:** [YYYY-MM-DD]  
**Status:** [Open / In Progress / Resolved / Closed]  
**Priority:** [P0 - Critical / P1 - High / P2 - Medium / P3 - Low]  
**Severity:** [Critical / High / Medium / Low]

---

## Summary

**Title:** [Brief description of the bug]

**Component:** [Table Management / Menu Sync / Order Flow / Order Editing / Notifications / Integration / Performance / Error Handling]

**Test Case:** [Test ID from test execution guide, e.g., "Test 7.2.1"]

---

## Description

[Detailed description of the bug, what happened, and what was expected]

---

## Steps to Reproduce

1. [Step 1]
2. [Step 2]
3. [Step 3]
4. [Step 4]

**Reproducibility:** [Always / Sometimes / Rarely / Once]

---

## Expected Behavior

[What should have happened]

---

## Actual Behavior

[What actually happened]

---

## Environment

**Platform:** [Android / Admin Dashboard / Database / Edge Function]

**Version:** [Version number]

**Device/OS:** [Device model and OS version, if applicable]

**Browser:** [Browser and version, if applicable]

**Network:** [WiFi / Mobile Data / Offline]

---

## Screenshots/Logs

**Screenshots:**
- [Link to screenshot 1]
- [Link to screenshot 2]

**Error Messages:**
```
[Error message text]
```

**Logs:**
```
[Relevant log entries]
```

**Database State:**
```sql
-- Relevant queries and results
```

---

## Impact

**User Impact:** [Description of how this affects users]

**Business Impact:** [Description of business impact]

**Affected Features:**
- [Feature 1]
- [Feature 2]

---

## Workaround

[If available, describe any workaround]

---

## Additional Information

**Related Issues:** [Link to related bugs or issues]

**Notes:** [Any additional context or information]

---

## Resolution

**Resolved By:** [Name]  
**Resolved Date:** [YYYY-MM-DD]  
**Resolution:** [Description of fix]

**Fixed In:** [Version/Commit]

**Verification:** [How the fix was verified]

---

## Example Bug Reports

### Example 1: Critical - Table Availability Not Updated

**Bug ID:** BUG-001  
**Priority:** P0 - Critical  
**Severity:** Critical  
**Component:** Table Management  
**Test Case:** Test 7.2.3

**Summary:** Table remains marked as occupied after order is delivered

**Description:**
When an order on table 1 is delivered, the table should become available for new orders. However, the table remains in the occupied list even after the order status changes to "Delivered".

**Steps to Reproduce:**
1. Create DINE_IN order on table 1
2. Verify table 1 is occupied
3. Deliver the order
4. Check `get_occupied_tables()` query
5. Table 1 still appears in results

**Expected Behavior:**
Table 1 should not appear in `get_occupied_tables()` after order is delivered.

**Actual Behavior:**
Table 1 still appears in occupied tables list.

**Database Query:**
```sql
SELECT * FROM get_occupied_tables('store-uuid');
-- Returns: [1, 2, 3, 4] (should not include 1 if order is delivered)
```

**Impact:**
Users cannot create new orders on delivered tables, blocking table reuse.

---

### Example 2: High - Notification Not Received

**Bug ID:** BUG-002  
**Priority:** P1 - High  
**Severity:** High  
**Component:** Notifications  
**Test Case:** Test 7.6.1

**Summary:** Waiter does not receive push notification when order is prepared

**Description:**
When chef marks an order as "Prepared", the waiter should receive a OneSignal push notification. However, the notification is not delivered to the waiter device.

**Steps to Reproduce:**
1. Chef marks order as "Prepared"
2. Wait for notification on waiter device
3. Notification never arrives

**Expected Behavior:**
Waiter receives push notification within 2 seconds of order being prepared.

**Actual Behavior:**
No notification received.

**Environment:**
- Android App Version: 1.0.0
- OneSignal App ID: [App ID]
- Device: [Device model]

**Logs:**
```
[OneSignal logs showing notification not sent]
```

**Impact:**
Waiters are not alerted when orders are ready, causing delivery delays.

---

### Example 3: Medium - Menu Sync Delay

**Bug ID:** BUG-003  
**Priority:** P2 - Medium  
**Severity:** Medium  
**Component:** Menu Synchronization  
**Test Case:** Test 7.3.2

**Summary:** Menu updates take longer than expected to sync to waiter app

**Description:**
When admin updates menu items, the waiter app takes 5-8 seconds to reflect changes, exceeding the target of 2 seconds.

**Steps to Reproduce:**
1. Admin updates menu item name
2. Measure time until waiter app shows update
3. Latency is 5-8 seconds

**Expected Behavior:**
Waiter app should show menu updates within 2 seconds.

**Actual Behavior:**
Menu updates appear after 5-8 seconds.

**Performance Metrics:**
- Average latency: 6.5 seconds
- Target: < 2 seconds
- Status: FAIL

**Impact:**
Waiters may see outdated menu information, potentially causing order errors.

---

## Bug Priority Guidelines

**P0 - Critical:**
- System crashes or data loss
- Complete feature failure
- Security vulnerabilities
- Blocks all testing

**P1 - High:**
- Major feature broken
- Significant user impact
- Workaround available but difficult
- Affects core functionality

**P2 - Medium:**
- Feature partially broken
- Moderate user impact
- Workaround available
- Non-critical functionality

**P3 - Low:**
- Minor issues
- Low user impact
- Easy workaround
- Cosmetic or edge cases

---

## Bug Lifecycle

1. **Open:** Bug reported, awaiting triage
2. **In Progress:** Bug assigned, being worked on
3. **Resolved:** Fix implemented, awaiting verification
4. **Closed:** Bug verified fixed and closed

---

## Reporting Guidelines

1. **Be Specific:** Provide exact steps and expected vs actual results
2. **Include Evidence:** Screenshots, logs, error messages
3. **Test Reproducibility:** Try to reproduce multiple times
4. **Document Environment:** Include all relevant environment details
5. **Assess Impact:** Clearly describe user and business impact
6. **Suggest Priority:** Based on impact and severity guidelines

---

## Bug Tracking

Use this format for bug IDs: `BUG-XXX` where XXX is a sequential number.

Maintain a master bug list with:
- Bug ID
- Title
- Status
- Priority
- Assigned To
- Resolution

