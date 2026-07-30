# Phase 8: Real-time Features - Testing Guide

## Prerequisites

Ensure all infrastructure is running:

```bash
# Start PostgreSQL, Redis, Kafka, Zookeeper, and MinIO
docker-compose up -d

# Verify services are healthy
docker-compose ps
```

## Backend Testing

### 1. Start the Backend

```bash
cd backend
./gradlew bootRun
```

The application should start successfully. Look for these log entries:
- `Started BackendApplication in X seconds`
- Kafka topic creation: `document-events` with 3 partitions
- No errors about WebSocket or Kafka configuration

### 2. Test Document Upload → Kafka Event → Notification Flow

#### Step 1: Register a user

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "password123",
    "displayName": "Alice"
  }'
```

Save the `accessToken` from the response.

#### Step 2: Upload a document

```bash
export TOKEN="your-access-token-here"

curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.txt" \
  -F "folderId="
```

**Expected Backend Logs:**

```
INFO  DocumentEventPublisher - Published document.uploaded for document <uuid>
INFO  DocumentEventNotificationConsumer - Processing document.uploaded event
INFO  AuditEventConsumer - Recording audit entry for document.uploaded
```

If you don't see these logs, check:
- Kafka broker is running: `docker-compose ps kafka`
- Consumer groups are registered: `docker exec -it <kafka-container> kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list`

#### Step 3: Check notifications were created

```bash
curl -X GET http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**

```json
[
  {
    "id": "...",
    "type": "UPLOAD_COMPLETE",
    "payloadJson": "{\"filename\":\"test.txt\",\"sizeBytes\":123}",
    "read": false,
    "createdAt": "2026-07-31T..."
  }
]
```

#### Step 4: Check unread count

```bash
curl -X GET http://localhost:8080/api/v1/notifications/unread-count \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**

```json
{
  "count": 1
}
```

#### Step 5: Mark notification as read

```bash
export NOTIFICATION_ID="uuid-from-step-3"

curl -X PATCH http://localhost:8080/api/v1/notifications/$NOTIFICATION_ID/read \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:** HTTP 204 No Content

Verify unread count is now 0:

```bash
curl -X GET http://localhost:8080/api/v1/notifications/unread-count \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Test Audit Log

Query the database directly to verify audit entries:

```bash
docker exec -it docshare-postgres psql -U docshare -d docshare
```

```sql
SELECT 
  actor_id, 
  action, 
  target_type, 
  target_id, 
  occurred_at,
  result,
  metadata_json
FROM audit_log 
ORDER BY occurred_at DESC 
LIMIT 5;
```

**Expected Results:**

You should see entries for:
- `document.uploaded` with metadata containing filename and sizeBytes
- `document.deleted` if you deleted any documents

### 4. Test WebSocket Connection (Manual with wscat)

Install `wscat` if you don't have it:

```bash
npm install -g wscat
```

Connect to the WebSocket endpoint:

```bash
wscat -c "ws://localhost:8080/ws?token=$TOKEN" -s stomp
```

Once connected, send the STOMP CONNECT frame:

```
CONNECT
accept-version:1.1,1.2
heart-beat:10000,10000

^@
```

(Note: `^@` is the null byte terminator - press Ctrl+@ or copy/paste the actual null character)

Subscribe to notifications:

```
SUBSCRIBE
id:sub-0
destination:/user/queue/notifications

^@
```

**Now test real-time push:**

In another terminal, upload a document:

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test2.txt" \
  -F "folderId="
```

**Expected WebSocket Message:**

You should immediately receive a STOMP MESSAGE frame in the wscat window:

```
MESSAGE
destination:/user/queue/notifications
content-type:application/json
subscription:sub-0
message-id:...

{"id":"...","type":"UPLOAD_COMPLETE","payloadJson":"{\"filename\":\"test2.txt\",\"sizeBytes\":...}","read":false,"createdAt":"..."}
```

### 5. Test Kafka Consumer Independence

#### Test: Notification consumer down doesn't block uploads

Stop the application (Ctrl+C), then start it with notification consumer disabled:

```bash
# In application.yml, comment out the @KafkaListener in DocumentEventNotificationConsumer
# Or just note that uploads work even if consumers lag
```

Upload a document - it should succeed immediately regardless of consumer state.

Verify events are queued in Kafka:

```bash
docker exec -it <kafka-container> kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic document-events \
  --from-beginning
```

You should see all `document.uploaded` and `document.deleted` events as JSON.

### 6. Test Document Delete → Audit Only (No Notification)

Delete a document:

```bash
curl -X DELETE http://localhost:8080/api/v1/documents/<document-id> \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:**
- Audit log has a `document.deleted` entry
- NO new notification is created (deleting your own file isn't news to you)
- Kafka received the event (check with kafka-console-consumer)

## Integration Test

Run the NotificationServiceIT test:

```bash
cd backend
./gradlew test --tests "NotificationServiceIT"
```

**Expected:** All 3 tests pass:
- `notify_persistsNotification()`
- `listForUser_returnsNotificationsNewestFirst()`
- `unreadCount_countsUnreadOnly()`

If tests fail with Testcontainers errors, ensure Docker is running and you have permission to access the Docker socket.

## Frontend Testing (Once WebSocket client is implemented)

### WebSocket Connection from Browser

In your browser's dev console:

```javascript
const token = 'your-access-token';
const socket = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

socket.onopen = () => console.log('Connected');
socket.onmessage = (event) => console.log('Received:', event.data);
socket.onerror = (error) => console.error('Error:', error);
socket.onclose = () => console.log('Disconnected');
```

Upload a document via the API or UI, and watch for the real-time notification message in the console.

## Troubleshooting

### No Kafka events published

**Symptoms:** Upload succeeds but no log entry from DocumentEventPublisher

**Check:**
1. Kafka is running: `docker-compose ps kafka`
2. Topic exists: `docker exec -it <kafka> kafka-topics.sh --list --bootstrap-server localhost:9092`
3. No errors in application logs related to Kafka serialization

**Fix:**
```bash
# Restart Kafka and Zookeeper
docker-compose restart zookeeper kafka
```

### No notifications created

**Symptoms:** Event is published but notification table is empty

**Check:**
1. Consumer group is registered: `docker exec -it <kafka> kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list`
   - Should see `notification-service` group
2. Consumer lag: `docker exec -it <kafka> kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-service`
   - If LAG is increasing, events are piling up unprocessed

**Fix:**
- Check DocumentEventNotificationConsumer for exceptions in logs
- Verify PostgreSQL connection is healthy
- Restart the application

### WebSocket connection fails

**Symptoms:** wscat connection rejected or closes immediately

**Check:**
1. JWT token is valid (not expired)
2. Token is in query parameter: `?token=...`
3. SecurityConfig has `/ws/**` in PUBLIC_PATHS
4. Application logs show `Registered STOMP endpoints` on startup

**Fix:**
```bash
# Get a fresh token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123"}'

# Use the new token immediately
```

### Audit log empty

**Symptoms:** Events published but audit_log table is empty

**Check:**
1. Consumer group `audit-service` exists and has no lag
2. No exceptions in AuditEventConsumer logs

**Fix:**
- Both notification-service and audit-service consumer groups should exist independently
- Verify with: `docker exec -it <kafka> kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group audit-service`

## Verification Checklist

- [ ] Backend starts without errors
- [ ] Document upload publishes Kafka event (check logs)
- [ ] Notification consumer creates notification (GET /notifications)
- [ ] Audit consumer creates audit entry (check database)
- [ ] WebSocket handshake succeeds with JWT
- [ ] WebSocket receives real-time notification on upload
- [ ] Unread count increments/decrements correctly
- [ ] Mark as read updates notification state
- [ ] Document delete creates audit entry but no notification
- [ ] Multiple uploads create multiple notifications
- [ ] NotificationServiceIT tests pass

## Performance Notes

- **Event publishing:** Fire-and-forget, does not block upload response
- **Consumer independence:** Notification and Audit consumers run in separate groups
- **Ordering guarantee:** Events for the same document land in the same partition (keyed by documentId)
- **WebSocket scalability:** Each user session maintains one WebSocket connection; consider using a message broker (RabbitMQ/Redis Pub/Sub) in production for horizontal scaling

## Next Steps

After verifying Phase 8 works:
1. Implement frontend WebSocket client (SockJS + STOMP.js)
2. Add notification badge UI component
3. Wire up real-time updates to notification bell icon
4. Add remaining notification types (share, comment, quota) when those features exist
