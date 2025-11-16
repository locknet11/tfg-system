# Alerts Module - Documentation

## Overview

The Alerts module provides automated email notifications for critical security events in the TFG System. Alert configurations are scoped by organization and project, allowing fine-grained control over who receives notifications for specific events.

## Features

- **Email Notifications**: Send automated emails when security events occur
- **Configurable Conditions**: Choose which events trigger alerts (vulnerability detection, remediation status, scan completion)
- **Scoped Access**: Alerts are automatically scoped to the current organization and project context
- **Enable/Disable**: Toggle alerts on/off without deleting the configuration
- **Last Triggered Tracking**: View when an alert was last sent

## Architecture

### Backend Components

#### Model Layer
- **`AlertConfiguration`**: Entity representing an alert configuration
  - Location: `api/src/main/java/com/spulido/tfg/domain/alerts/model/`
  - Fields: `id`, `sendTo` (email), `conditions`, `enabled`, `lastTriggeredAt`, `organizationId`, `projectId`
  - Implements `ScopedEntity` for automatic organization/project filtering

- **`WhenCondition`**: Enum defining trigger conditions
  - `ON_VULNERABILITY_DETECTED`: Any vulnerability found
  - `ON_HIGH_SEVERITY_VULNERABILITY`: High/Critical severity only
  - `ON_REMEDIATION_SUCCESS`: Successful patch application
  - `ON_REMEDIATION_FAILURE`: Failed patch application
  - `ON_SCAN_COMPLETED`: Scan finished (with or without findings)

- **`AlertEvent`**: Event model for triggering alerts
  - Fields: `type`, `severity`, `payload`, `timestamp`, `organizationId`, `projectId`

#### Repository Layer
- **`AlertConfigurationRepository`**: MongoDB repository with scoped queries
  - Location: `api/src/main/java/com/spulido/tfg/domain/alerts/db/`
  - Uses `ProjectContext` for automatic org/project filtering
  - Methods: `findAllScoped`, `findByIdScoped`, `findAllActiveScoped`

#### Service Layer
- **`AlertConfigurationService`**: CRUD operations for alert configurations
  - Location: `api/src/main/java/com/spulido/tfg/domain/alerts/services/`
  - Methods: `create`, `update`, `get`, `list`, `delete`
  - Validates email format and condition requirements

- **`AlertTriggerService`**: Event processing and email sending
  - Location: `api/src/main/java/com/spulido/tfg/domain/alerts/services/impl/`
  - `@Async` method: `checkAndTrigger(AlertEvent)`
  - Matches event conditions against active alert configs
  - Sends emails via `JavaMailSender`
  - Updates `lastTriggeredAt` timestamp

#### Controller Layer
- **`AlertConfigurationController`**: REST API endpoints
  - Location: `api/src/main/java/com/spulido/tfg/domain/alerts/controller/`
  - Base path: `/api/alerts`
  - Requires authentication (`@PreAuthorize("isAuthenticated()")`)

### Frontend Components

#### Data Layer
- **Models**: `AlertConfiguration`, `WhenCondition` enum
  - Location: `ui/src/app/pages/alerts/data-access/alerts.model.ts`
  - Helper functions: `whenConditionLabel()`, `getAllWhenConditions()`

- **Service**: `AlertsService`
  - Location: `ui/src/app/pages/alerts/data-access/alerts.service.ts`
  - HTTP methods: `list()`, `get()`, `create()`, `update()`, `delete()`

#### UI Components
- **`AlertsListComponent`**: Main list view with table
  - Location: `ui/src/app/pages/alerts/feature/alerts-list/`
  - Features: Pagination, enable/disable toggle, edit/delete actions
  - Uses PrimeNG Table, Paginator, InputSwitch, Tag components

- **`CreateAlertModalComponent`**: Modal for creating new alerts
  - Location: `ui/src/app/pages/alerts/feature/modals/create-alert-modal/`
  - Reactive form with email and multi-select conditions
  - Validation: Email format, at least one condition required

- **`EditAlertModalComponent`**: Modal for editing existing alerts
  - Location: `ui/src/app/pages/alerts/feature/modals/edit-alert-modal/`
  - Same form structure as create modal, pre-filled with current values

#### Routing
- Route: `/alerts`
- Lazy-loaded via `alerts.routes.ts`
- Menu item already configured in `menu.component.ts`

## API Endpoints

### List Alert Configurations
```http
GET /api/alerts?page=0&size=10
Authorization: Bearer {token}
X-Organization-Id: {orgId}
X-Project-Id: {projectId}

Response 200:
{
  "content": [
    {
      "id": "abc123",
      "sendTo": "security@example.com",
      "conditions": ["ON_HIGH_SEVERITY_VULNERABILITY", "ON_SCAN_COMPLETED"],
      "enabled": true,
      "lastTriggeredAt": "2025-11-16T10:30:00Z",
      "createdAt": "2025-11-15T08:00:00",
      "updatedAt": "2025-11-16T09:15:00"
    }
  ],
  "totalElements": 1
}
```

### Get Single Alert Configuration
```http
GET /api/alerts/{id}
Authorization: Bearer {token}
X-Organization-Id: {orgId}
X-Project-Id: {projectId}

Response 200:
{
  "id": "abc123",
  "sendTo": "security@example.com",
  "conditions": ["ON_HIGH_SEVERITY_VULNERABILITY"],
  "enabled": true,
  "lastTriggeredAt": null,
  "createdAt": "2025-11-15T08:00:00",
  "updatedAt": "2025-11-15T08:00:00"
}
```

### Create Alert Configuration
```http
POST /api/alerts
Authorization: Bearer {token}
X-Organization-Id: {orgId}
X-Project-Id: {projectId}
Content-Type: application/json

{
  "sendTo": "alerts@example.com",
  "conditions": ["ON_VULNERABILITY_DETECTED", "ON_REMEDIATION_FAILURE"],
  "enabled": true
}

Response 201:
Location: /api/alerts/xyz789
{
  "id": "xyz789",
  "sendTo": "alerts@example.com",
  "conditions": ["ON_VULNERABILITY_DETECTED", "ON_REMEDIATION_FAILURE"],
  "enabled": true,
  "lastTriggeredAt": null,
  "createdAt": "2025-11-16T11:00:00",
  "updatedAt": "2025-11-16T11:00:00"
}
```

### Update Alert Configuration
```http
PUT /api/alerts/{id}
Authorization: Bearer {token}
X-Organization-Id: {orgId}
X-Project-Id: {projectId}
Content-Type: application/json

{
  "sendTo": "alerts@example.com",
  "conditions": ["ON_HIGH_SEVERITY_VULNERABILITY"],
  "enabled": false
}

Response 200:
{
  "id": "xyz789",
  "sendTo": "alerts@example.com",
  "conditions": ["ON_HIGH_SEVERITY_VULNERABILITY"],
  "enabled": false,
  "lastTriggeredAt": "2025-11-16T10:30:00Z",
  "createdAt": "2025-11-15T08:00:00",
  "updatedAt": "2025-11-16T12:00:00"
}
```

### Delete Alert Configuration
```http
DELETE /api/alerts/{id}
Authorization: Bearer {token}
X-Organization-Id: {orgId}
X-Project-Id: {projectId}

Response 204 No Content
```

## Configuration

### SMTP Settings (Backend)

Add the following environment variables for email functionality:

```bash
# Required
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Optional (defaults shown)
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_SMTP_STARTTLS_REQUIRED=false
```

**Application Properties** (`api/src/main/resources/application.properties`):
```properties
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS:true}
spring.mail.properties.mail.smtp.starttls.required=${MAIL_SMTP_STARTTLS_REQUIRED:false}
```

### MongoDB Indexes

The module automatically creates a compound index on `organizationId` and `projectId` for efficient scoped queries:

```javascript
db.alertConfigurations.createIndex(
  { "organizationId": 1, "projectId": 1 },
  { name: "alert_scope_idx" }
)
```

## Integration with Event System

To trigger alerts from your code (e.g., when processing scan results or remediations):

```java
@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final AlertTriggerService alertTriggerService;
    
    public void processVulnerabilityReport(Report report) {
        // ... process report ...
        
        // Trigger alerts for high severity vulnerabilities
        if (report.getSeverity().equals("HIGH") || report.getSeverity().equals("CRITICAL")) {
            AlertEvent event = AlertEvent.builder()
                .type(AlertEvent.AlertEventType.VULNERABILITY_DETECTED)
                .severity(report.getSeverity())
                .organizationId(report.getOrganizationId())
                .projectId(report.getProjectId())
                .timestamp(Instant.now())
                .payload(Map.of(
                    "cveId", report.getCveId(),
                    "description", report.getDescription(),
                    "targetIp", report.getTargetIp()
                ))
                .build();
            
            alertTriggerService.checkAndTrigger(event);
        }
    }
}
```

## Testing

### Unit Tests (Backend)

Example test for `AlertConfigurationService`:

```java
@SpringBootTest
class AlertConfigurationServiceTest {
    
    @Autowired
    private AlertConfigurationService service;
    
    @Autowired
    private AlertConfigurationRepository repository;
    
    @Test
    void testCreateAlertConfiguration() {
        AlertConfiguration config = new AlertConfiguration()
            .setSendTo("test@example.com")
            .setConditions(List.of(WhenCondition.ON_VULNERABILITY_DETECTED))
            .setEnabled(true);
        
        AlertConfiguration created = service.createAlertConfiguration(config);
        
        assertNotNull(created.getId());
        assertEquals("test@example.com", created.getSendTo());
        assertTrue(created.isEnabled());
    }
    
    @Test
    void testValidationFailsWithInvalidEmail() {
        AlertConfiguration config = new AlertConfiguration()
            .setSendTo("invalid-email")
            .setConditions(List.of(WhenCondition.ON_SCAN_COMPLETED));
        
        assertThrows(AlertException.class, () -> {
            service.createAlertConfiguration(config);
        });
    }
}
```

### Integration Tests (Frontend)

Example test for `AlertsListComponent`:

```typescript
describe('AlertsListComponent', () => {
  let component: AlertsListComponent;
  let fixture: ComponentFixture<AlertsListComponent>;
  let alertsService: jasmine.SpyObj<AlertsService>;

  beforeEach(async () => {
    const alertsServiceSpy = jasmine.createSpyObj('AlertsService', [
      'list',
      'delete',
      'update',
    ]);

    await TestBed.configureTestingModule({
      imports: [AlertsListComponent],
      providers: [{ provide: AlertsService, useValue: alertsServiceSpy }],
    }).compileComponents();

    alertsService = TestBed.inject(
      AlertsService
    ) as jasmine.SpyObj<AlertsService>;
    fixture = TestBed.createComponent(AlertsListComponent);
    component = fixture.componentInstance;
  });

  it('should load alerts on init', () => {
    const mockAlerts = {
      content: [
        {
          id: '1',
          sendTo: 'test@example.com',
          conditions: [WhenCondition.ON_VULNERABILITY_DETECTED],
          enabled: true,
          createdAt: '2025-11-16T10:00:00',
          updatedAt: '2025-11-16T10:00:00',
        },
      ],
      totalElements: 1,
    };
    alertsService.list.and.returnValue(of(mockAlerts));

    component.ngOnInit();

    expect(alertsService.list).toHaveBeenCalled();
    expect(component.alertsSig().length).toBe(1);
  });
});
```

## Troubleshooting

### Emails Not Sending

1. **Check SMTP configuration**: Verify `MAIL_HOST`, `MAIL_USERNAME`, and `MAIL_PASSWORD` are set correctly
2. **Enable less secure apps** (Gmail): For Gmail, you may need to use an [App Password](https://support.google.com/accounts/answer/185833)
3. **Check firewall**: Ensure port 587 (or 465 for SSL) is not blocked
4. **Review logs**: Look for errors in application logs: `grep "Failed to send alert" api/logs/spring.log`

### Alerts Not Triggering

1. **Verify alert is enabled**: Check `enabled` field is `true`
2. **Check conditions match**: Ensure event type and severity match configured conditions
3. **Verify context scoping**: Ensure `organizationId` and `projectId` in event match alert config scope
4. **Check async execution**: Verify `@EnableAsync` is present in `CommonConfig`

### Permission Issues

1. **Authentication required**: All endpoints require `Authorization` header with valid JWT
2. **Scoping headers required**: `X-Organization-Id` and `X-Project-Id` headers must be present
3. **Cross-organization access**: Users cannot access alerts from other organizations (enforced by scoped queries)

## Future Enhancements

- [ ] Webhooks in addition to email
- [ ] SMS notifications via Twilio integration
- [ ] Alert rate limiting to prevent spam
- [ ] Alert templates with customizable message formats
- [ ] Alert history/audit log
- [ ] Slack/Teams integration
- [ ] Machine learning for intelligent alert prioritization
- [ ] Multi-language email templates

## References

- Spring Boot Mail: https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email
- MongoDB Compound Indexes: https://www.mongodb.com/docs/manual/core/index-compound/
- Angular Reactive Forms: https://angular.io/guide/reactive-forms
- PrimeNG Components: https://primeng.org/
