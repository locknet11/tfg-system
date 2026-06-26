# Research: Autonomous Remediation Flow

**Date**: 2026-06-26  
**Feature**: 010-remediation-flow  
**Status**: Complete

---

## 1. Remediation Strategy Knowledge Base

### Decision
**Use a hybrid approach: local JSON/YAML knowledge base with fallback to external APIs**

### Rationale
1. **Performance**: Local lookups are instant vs. API calls (100-500ms latency)
2. **Reliability**: Works offline or when external APIs are down
3. **Control**: Security team can curate and validate remediation strategies
4. **Extensibility**: Can add API fallbacks later (Ubuntu Security API, NVD)

### Implementation Approach
- Store strategies in `api/src/main/resources/remediation/strategies.json`
- Structure: `{ "cve_id": "CVE-2023-12345", "package": "openssh-server", "min_version": "9.3p2", "action": "APT_UPGRADE", "requires_reboot": false }`
- Load at startup into `RemediationStrategyRepository` (in-memory cache)
- Fallback: If CVE not found in local KB, mark as `UNKNOWN_STRATEGY` and log for manual review

### Alternatives Considered

**Option A: Pure external API integration (Ubuntu Security API, NVD)**
- Rejected: Latency, rate limits, offline scenarios, less control over quality

**Option B: Machine learning to predict remediation actions**
- Rejected: Too complex for MVP, requires training data, unpredictable results

**Option C: Hardcoded Java switch statements**
- Rejected: Not maintainable, requires recompilation for new CVEs

---

## 2. Remediation Type Detection

### Decision
**Use package metadata and reboot requirement flags to classify remediation types**

### Rationale
1. **Type A (Service-level)**: Package doesn't require reboot (most apt packages)
2. **Type B (Reboot-required)**: Package metadata indicates reboot needed (kernel modules, libc)
3. **Type C (Kernel update)**: Package name matches kernel patterns (`linux-image-*`, `linux-headers-*`)

### Implementation Approach

```java
public enum RemediationType {
    SERVICE_UPDATE,      // Type A: restart service
    REBOOT_REQUIRED,     // Type B: needs reboot
    KERNEL_UPDATE,       // Type C: kernel, skip
    UNKNOWN              // Cannot determine
}
```

**Detection logic**:
```java
// Pattern matching for kernel packages
private static final Pattern KERNEL_PACKAGE_PATTERN = 
    Pattern.compile("^linux-(image|headers|modules)-.*");

public RemediationType classify(String packageName, boolean requiresReboot) {
    if (KERNEL_PACKAGE_PATTERN.matcher(packageName).matches()) {
        return RemediationType.KERNEL_UPDATE;
    }
    if (requiresReboot) {
        return RemediationType.REBOOT_REQUIRED;
    }
    return RemediationType.SERVICE_UPDATE;
}
```

### Alternatives Considered

**Option A: Query package manager for reboot requirement**
- Rejected: Not all package managers expose this reliably

**Option B: Check if service is systemd-managed**
- Rejected: Too narrow, doesn't cover all cases

---

## 3. Post-Remediation Verification

### Decision
**Multi-layer verification: package version check + service status + vulnerability re-scan**

### Rationale
1. **Package version**: Confirms the upgrade happened
2. **Service status**: Ensures service restarted and running
3. **Vulnerability re-scan**: Confirms CVE no longer present

### Implementation Approach

```java
public class VerificationResult {
    private boolean packageVersionUpdated;
    private boolean serviceRunning;
    private boolean cveResolved;
    private List<String> verificationLogs;
    
    public boolean isSuccess() {
        return packageVersionUpdated && serviceRunning && cveResolved;
    }
}
```

**Verification steps**:
1. Query package version: `dpkg -l {package} | grep ^ii`
2. Check service status: `systemctl is-active {service}`
3. Re-scan with vulnerability lookup service
4. Compare before/after versions and CVE list

### Alternatives Considered

**Option A: Only check package version**
- Rejected: Doesn't verify service actually restarted or CVE resolved

**Option B: Only re-scan vulnerabilities**
- Rejected: Slow (requires full scan), doesn't provide granular feedback

---

## 4. Rollback Strategy

### Decision
**No automatic rollback for MVP. Log detailed state for manual recovery.**

### Rationale
1. **Complexity**: Rollback requires snapshot/restore, package downgrades, service state management
2. **Risk**: Automatic rollback could cause more issues (cascading failures)
3. **MVP scope**: Focus on remediation, not recovery
4. **Future enhancement**: Can add snapshot/restore in v2

### Implementation Approach

**Before remediation**:
- Log current package version
- Log service status
- Log running processes
- Store in `RemediationRecord.preRemediationState`

**If remediation fails**:
- Mark as `FAILED` with detailed error
- Preserve pre-remediation state in record
- Alert operator for manual intervention
- Provide rollback commands in error message (e.g., `apt install {package}={old_version}`)

### Alternatives Considered

**Option A: Automatic package downgrade**
- Rejected: Complex, can break dependencies, risky

**Option B: Filesystem snapshots (LVM, Btrfs)**
- Rejected: Requires specific filesystem, out of scope for MVP

**Option C: Docker/container rollback**
- Rejected: Not all targets are containerized

---

## 5. Remediation Record Data Model

### Decision
**Extend existing `BaseEntity` and implement `ScopedEntity` for multi-tenancy**

### Rationale
1. **Consistency**: Matches existing entities (Target, Agent, etc.)
2. **Multi-tenancy**: Automatic org/project scoping via `ScopedEntity`
3. **Auditability**: `createdAt`, `updatedAt` from `BaseEntity`

### Key Fields

```java
@Document(collection = "remediation_records")
public class RemediationRecord extends BaseEntity implements ScopedEntity {
    
    // Relationships
    private String vulnerabilityRecordId;  // FK -> ServiceVulnerabilityRecord
    private String cveId;                   // CVE-2023-12345
    private String targetId;                // FK -> Target
    private String agentId;                 // FK -> Agent (executing agent)
    
    // Remediation details
    private RemediationType type;           // SERVICE_UPDATE, REBOOT_REQUIRED, etc.
    private RemediationStatus status;       // PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED
    private String packageName;             // openssh-server
    private String packageVersionBefore;    // 8.9p1
    private String packageVersionAfter;     // 9.3p2
    private String actionDescription;       // "Upgraded openssh-server to 9.3p2"
    
    // Execution logs
    private List<String> preRemediationLogs;   // State before fix
    private List<String> executionLogs;        // Commands executed
    private List<String> verificationLogs;     // Post-fix verification
    
    // State snapshots
    private RemediationState preRemediationState;  // Package version, service status
    private RemediationState postRemediationState; // After fix
    
    // Timestamps
    private Instant startedAt;
    private Instant completedAt;
    
    // Error handling
    private String errorMessage;
    private String rollbackCommands;  // Manual rollback instructions
    
    // Multi-tenancy
    private String organizationId;
    private String projectId;
}
```

### Alternatives Considered

**Option A: Embed remediation records in ServiceVulnerabilityRecord**
- Rejected: Violates separation of concerns, makes queries complex

**Option B: Separate collection without BaseEntity**
- Rejected: Loses audit fields, inconsistent with other entities

---

## 6. Agent-Side Remediation Execution

### Decision
**Reuse existing `StepHandler` pattern with new `RemediationStepHandler`**

### Rationale
1. **Consistency**: Matches existing pattern for `EXPLOITATION_KNOWLEDGE`, `EXECUTE_EXPLOIT`
2. **Extensibility**: Easy to add new remediation strategies
3. **Context propagation**: Can access vulnerability data from previous steps

### Implementation Approach

```java
public class RemediationStepHandler implements StepHandler {
    
    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context) {
        // 1. Get vulnerability data from EXPLOITATION_KNOWLEDGE step
        StepResult exploitResult = context.get(StepAction.EXPLOITATION_KNOWLEDGE);
        List<Vulnerability> vulnerabilities = parseVulnerabilities(exploitResult);
        
        // 2. For each CVE, request remediation strategy from API
        for (Vulnerability vuln : vulnerabilities) {
            RemediationStrategy strategy = requestStrategy(vuln.getCveId());
            
            // 3. Execute remediation based on type
            switch (strategy.getType()) {
                case SERVICE_UPDATE:
                    executeServiceUpdate(strategy);
                    break;
                case REBOOT_REQUIRED:
                    markRebootRequired(strategy);
                    break;
                case KERNEL_UPDATE:
                    skipKernelUpdate(strategy);
                    break;
            }
        }
        
        // 4. Report results back to API
        return StepResult.success("Remediation completed");
    }
}
```

### Alternatives Considered

**Option A: Execute remediation in `TaskExecutionService` directly**
- Rejected: Violates single responsibility, harder to test

**Option B: Create separate remediation worker thread**
- Rejected: Over-engineering, existing worker pool is sufficient

---

## 7. Library Recommendations

### Decision
**Use established libraries to reduce boilerplate and improve reliability**

### Recommended Libraries

#### 1. **Apache Commons Lang 3** (already in use)
- **Purpose**: String manipulation, null-safe operations, number parsing
- **Usage**: `StringUtils.isBlank()`, `ObjectUtils.defaultIfNull()`, `NumberUtils.toInt()`
- **Rationale**: Reduces null checks, handles edge cases

#### 2. **Guava** (consider adding)
- **Purpose**: Collections, caching, Optional utilities
- **Usage**: `ImmutableList`, `Cache<String, RemediationStrategy>`, `Optional.fromNullable()`
- **Rationale**: Better than Java 8 Optional, thread-safe collections
- **Note**: If not already in project, use Java 8+ built-ins instead

#### 3. **SemVer4j** or **java-semver** (for version comparison)
- **Purpose**: Parse and compare package versions (e.g., "8.9p1" vs "9.3p2")
- **Usage**: `Version.valueOf("8.9p1").isLowerThan(Version.valueOf("9.3p2"))`
- **Rationale**: Handles complex version strings (Debian package versions include epochs, revisions)
- **Alternative**: Use `DebianVersion` library specifically for Debian package versions

#### 4. **JSch** (already in use for SSH)
- **Purpose**: SSH command execution
- **Usage**: Execute remediation commands on remote targets
- **Rationale**: Already integrated, battle-tested

#### 5. **Jackson** (already in use)
- **Purpose**: JSON serialization for strategies.json
- **Usage**: Load remediation strategies from JSON file
- **Rationale**: Already in project, consistent with other JSON handling

### Alternatives Considered

**Option A: Write custom version comparison logic**
- Rejected: Error-prone, doesn't handle edge cases (epochs, revisions, pre-releases)

**Option B: Use shell commands for version comparison**
- Rejected: Fragile, platform-dependent, hard to test

---

## 8. API Design for Remediation

### Decision
**RESTful endpoints following existing patterns in `AgentCommunicationController`**

### Endpoints

#### Agent-Facing API (authenticated via API key)

```
POST /api/agent/comm/remediation/strategy
Request: { "cveId": "CVE-2023-12345", "packageName": "openssh-server", "currentVersion": "8.9p1" }
Response: { "type": "SERVICE_UPDATE", "action": "APT_UPGRADE", "targetVersion": "9.3p2", "commands": [...] }

POST /api/agent/comm/remediation/report
Request: { "remediationRecordId": "...", "status": "SUCCESS", "logs": [...], "packageVersionAfter": "9.3p2" }
Response: { "success": true }
```

#### User-Facing API (authenticated via session)

```
GET /api/remediations?targetId=...&status=...&page=0&size=20
Response: { "content": [...], "totalElements": 42, "totalPages": 3 }

GET /api/remediations/{id}
Response: { "id": "...", "cveId": "...", "status": "SUCCESS", ... }

POST /api/remediations
Request: { "targetId": "...", "cveId": "...", "strategy": "AUTO" | "MANUAL" }
Response: { "remediationId": "...", "status": "PENDING" }
```

### Rationale
1. **Consistency**: Matches existing API patterns
2. **Separation**: Agent API vs. user API (different auth mechanisms)
3. **Pagination**: Follows Spring Data conventions

### Alternatives Considered

**Option A: GraphQL for remediation queries**
- Rejected: Over-engineering, existing API is REST

**Option B: WebSocket for real-time remediation progress**
- Rejected: Complexity, polling is sufficient for MVP

---

## 9. UI/UX Design

### Decision
**Extend existing vulnerabilities page with remediation actions and add remediation history page**

### UI Components

#### 1. **Vulnerabilities Page Enhancement**
- Add "Remediate" button per CVE row
- Show remediation status badge (PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED)
- Modal confirmation before remediation (with strategy preview)

#### 2. **Remediation History Page** (`/remediations`)
- Table with columns: Target, CVE, Package, Status, Agent, Started, Completed
- Filters: status, target, date range, remediation type
- Click row → detail page

#### 3. **Remediation Detail Page** (`/remediations/{id}`)
- Full remediation record with expandable log sections
- Timeline: PENDING → IN_PROGRESS → SUCCESS/FAILED
- Pre/post state comparison
- Error message (if failed) with rollback commands

#### 4. **Dashboard Widget**
- Remediation counts by status (pie chart or stat cards)
- MTTR (mean time to remediate)
- Recent activity feed (last 5 remediations)

### Implementation Approach
- Use existing Angular patterns: standalone components, signal inputs/outputs
- Reuse PrimeNG components: `p-table`, `p-button`, `p-dialog`, `p-tag`
- Follow existing routing pattern: lazy-loaded feature module

### Alternatives Considered

**Option A: Real-time progress updates via WebSocket**
- Rejected: Complexity, polling every 5s is sufficient

**Option B: Inline remediation in vulnerability detail page**
- Rejected: Clutters the page, better as separate history

---

## 10. Error Handling and Null Safety

### Decision
**Use Java 8+ Optional, validation annotations, and defensive programming**

### Implementation Approach

#### Null Safety
```java
// Use Optional for nullable returns
public Optional<RemediationStrategy> findStrategy(String cveId) {
    return Optional.ofNullable(strategyMap.get(cveId));
}

// Use Objects.requireNonNull for constructor injection
public RemediationService(RemediationRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
}

// Use StringUtils for string checks
if (StringUtils.isBlank(cveId)) {
    throw new IllegalArgumentException("cveId must not be blank");
}
```

#### Validation
```java
// Use jakarta.validation annotations on DTOs
public class RemediationRequest {
    @NotBlank(message = "cveId must not be blank")
    @Pattern(regexp = "CVE-\\d{4}-\\d+", message = "Invalid CVE ID format")
    private String cveId;
    
    @NotBlank(message = "targetId must not be blank")
    private String targetId;
}

// Use @Valid in controllers
@PostMapping("/remediations")
public ResponseEntity<RemediationResponse> createRemediation(
    @RequestBody @Valid RemediationRequest request) {
    // ...
}
```

#### Defensive Programming
```java
// Check preconditions
public void executeRemediation(RemediationRecord record) {
    if (record.getStatus() != RemediationStatus.PENDING) {
        throw new IllegalStateException("Remediation must be in PENDING status");
    }
    
    // Use default values for nullable fields
    String packageName = ObjectUtils.defaultIfNull(
        record.getPackageName(), "unknown-package");
}
```

### Rationale
1. **Prevents NullPointerExceptions**: Explicit null handling
2. **Fail-fast**: Catches errors early with clear messages
3. **Consistency**: Matches existing validation patterns in project

### Alternatives Considered

**Option A: Use Guava Optional instead of Java 8 Optional**
- Rejected: Java 8 Optional is sufficient, reduces dependencies

**Option B: Custom validation framework**
- Rejected: Reinventing the wheel, jakarta.validation is standard

---

## 11. Constants and Magic Values

### Decision
**Use enums and constants classes to eliminate magic strings and numbers**

### Implementation Approach

#### Enums for Status and Types
```java
public enum RemediationStatus {
    PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED, PENDING_REBOOT
}

public enum RemediationType {
    SERVICE_UPDATE, REBOOT_REQUIRED, KERNEL_UPDATE, UNKNOWN
}

public enum RemediationAction {
    APT_UPGRADE, APT_INSTALL, SYSTEMCTL_RESTART, CONFIG_UPDATE
}
```

#### Constants Class
```java
public final class RemediationConstants {
    private RemediationConstants() {} // Prevent instantiation
    
    // Timeouts
    public static final int REMEDIATION_TIMEOUT_SECONDS = 300;
    public static final int VERIFICATION_TIMEOUT_SECONDS = 60;
    
    // Retry configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MS = 5000L;
    
    // Log messages
    public static final String LOG_REMEDIATION_STARTED = "Remediation started for CVE: {}";
    public static final String LOG_REMEDIATION_COMPLETED = "Remediation completed successfully";
    public static final String LOG_REMEDIATION_FAILED = "Remediation failed: {}";
    
    // Error messages
    public static final String ERROR_STRATEGY_NOT_FOUND = "No remediation strategy found for CVE: {}";
    public static final String ERROR_PACKAGE_NOT_FOUND = "Package not found: {}";
    public static final String ERROR_SERVICE_NOT_RUNNING = "Service not running after remediation: {}";
}
```

### Rationale
1. **Type safety**: Enums prevent typos
2. **Maintainability**: Change values in one place
3. **Discoverability**: IDE autocomplete for constants
4. **Documentation**: Self-documenting code

### Alternatives Considered

**Option A: Use string literals directly**
- Rejected: Error-prone, hard to refactor

**Option B: Use properties file for all constants**
- Rejected: Overkill for compile-time constants

---

## 12. Testing Strategy

### Decision
**Unit tests for services, integration tests for repositories, contract tests for API**

### Implementation Approach

#### Unit Tests (JUnit 5 + Mockito)
```java
@ExtendWith(MockitoExtension.class)
class RemediationServiceTest {
    
    @Mock
    private RemediationRepository repository;
    
    @InjectMocks
    private RemediationService service;
    
    @Test
    void shouldCreateRemediationRecord() {
        // Given
        RemediationRequest request = new RemediationRequest("CVE-2023-12345", "target-123");
        when(repository.save(any())).thenReturn(new RemediationRecord());
        
        // When
        RemediationRecord result = service.createRemediation(request);
        
        // Then
        assertNotNull(result);
        verify(repository).save(any());
    }
}
```

#### Integration Tests (@SpringBootTest)
```java
@SpringBootTest
class RemediationRepositoryIntegrationTest {
    
    @Autowired
    private RemediationRepository repository;
    
    @Test
    void shouldFindByStatus() {
        // Given
        repository.save(new RemediationRecord(..., RemediationStatus.SUCCESS));
        
        // When
        List<RemediationRecord> results = repository.findByStatus(RemediationStatus.SUCCESS);
        
        // Then
        assertThat(results).hasSize(1);
    }
}
```

#### Contract Tests (MockMvc)
```java
@WebMvcTest(RemediationController.class)
class RemediationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RemediationService service;
    
    @Test
    void shouldReturnRemediationList() throws Exception {
        // Given
        when(service.findAll(any())).thenReturn(Page.empty());
        
        // When/Then
        mockMvc.perform(get("/api/remediations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
```

### Rationale
1. **Fast feedback**: Unit tests run in milliseconds
2. **Isolation**: Mocks prevent flaky tests
3. **Coverage**: Tests all layers (controller, service, repository)

### Alternatives Considered

**Option A: End-to-end tests only**
- Rejected: Slow, brittle, hard to debug

**Option B: Use Testcontainers for all tests**
- Rejected: Overkill for unit tests, use for integration tests only

---

## Summary of Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Knowledge base | Hybrid (local JSON + API fallback) | Performance, reliability, control |
| Type detection | Package metadata + pattern matching | Simple, accurate |
| Verification | Multi-layer (version + service + re-scan) | Comprehensive |
| Rollback | None for MVP, log for manual recovery | Scope, complexity |
| Data model | Extend BaseEntity + ScopedEntity | Consistency, multi-tenancy |
| Agent execution | StepHandler pattern | Consistency, extensibility |
| Libraries | Apache Commons, SemVer4j, Jackson | Reduce boilerplate, reliability |
| API design | RESTful, separate agent/user endpoints | Consistency, separation of concerns |
| UI/UX | Extend vulnerabilities page + history page | Familiarity, clarity |
| Null safety | Optional, validation, defensive programming | Prevent NPEs, fail-fast |
| Constants | Enums + constants class | Type safety, maintainability |
| Testing | Unit + integration + contract | Fast feedback, coverage |

---

## Next Steps

1. **Phase 1**: Create `data-model.md` with detailed entity structures
2. **Phase 1**: Create `contracts/api-contracts.md` with endpoint specifications
3. **Phase 1**: Create `contracts/agent-contracts.md` with agent-API protocol
4. **Phase 1**: Create `quickstart.md` with implementation guide
5. **Phase 2**: Generate `tasks.md` with actionable implementation tasks
