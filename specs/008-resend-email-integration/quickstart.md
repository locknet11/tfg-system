# Quickstart: Resend Email Integration

**Feature**: 008-resend-email-integration

## Prerequisites

1. A Resend account with a verified domain and API key
2. Java 17+ and Maven installed
3. Existing TFG System API project running

## Setup

### 1. Configure Resend Credentials

Add the following to your `application.properties` or set as environment variables:

```properties
resend.api-key=${RESEND_API_KEY:re_your_api_key_here}
resend.from-address=${RESEND_FROM_ADDRESS:alerts@yourdomain.com}
```

**Important**: The `from-address` must be a verified domain in your Resend account.

### 2. Remove SMTP Configuration

Remove all `spring.mail.*` properties from `application.properties`:

```properties
# REMOVE these lines:
spring.mail.host=...
spring.mail.port=...
spring.mail.username=...
spring.mail.password=...
spring.mail.properties.mail.smtp.auth=...
spring.mail.properties.mail.smtp.starttls.enable=...
spring.mail.properties.mail.smtp.starttls.required=...
```

### 3. Verify

Start the application and trigger a test alert event. Check the logs for:

```
Alert sent to user@example.com for event type VULNERABILITY_DETECTED
```

Or on failure:

```
Failed to send alert to user@example.com: <error message>
```

## Testing

1. Create an alert configuration via the API with your email as the recipient
2. Trigger a matching security event (e.g., run a scan that finds a vulnerability)
3. Check your email inbox for the alert notification

## Troubleshooting

| Symptom                          | Cause                        | Fix                                           |
|----------------------------------|------------------------------|-----------------------------------------------|
| `401` error in logs              | Invalid API key              | Verify `RESEND_API_KEY` environment variable  |
| `400` error in logs              | Invalid sender/recipient     | Verify `RESEND_FROM_ADDRESS` is a verified domain in Resend |
| `429` error in logs              | Rate limit exceeded          | Reduce alert frequency or contact Resend support |
| No emails sent, no errors        | Missing configuration        | Check `resend.api-key` and `resend.from-address` are set |
