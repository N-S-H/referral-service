# Referral Service

## Overview
Referral Service exposes REST APIs to:
- Generate (idempotently) a single persistent referral code per authenticated user.
- Submit usage of a referral code with source and status tracking.
- Enforce maximum active submissions per user.
- Search referral codes and referral submissions with pagination & filtering.
- Provide structured error responses aligned with Backbase error conventions.

## Architecture & Stack
| Aspect | Details |
|--------|---------|
| Language | Java 17+ (Maven) |
| Framework | Spring Boot |
| Persistence | JPA / Hibernate (RDBMS) |
| DB Migration | (Add Flyway/Liquibase if present) |
| Serialization | Jackson (JavaTimeModule) |
| Testing | JUnit 5, MockMvc, Testcontainers |
| Security | Backbase SecurityContextUtil (stubbed/mocked in tests) |
| Packaging | Fat JAR / Docker-ready |
| Build | `mvn clean verify` |

## Package Structure (Indicative)
| Package | Responsibility |
|---------|---------------|
| `com.backbase.referral.rest` | REST controllers / request handling |
| `com.backbase.referral.entity` | JPA entities |
| `com.backbase.referral.repository` | Spring Data repositories |
| `com.backbase.referral.utils` | Constants, error codes |
| `com.backbase.referral.client.api.v1.model` | Generated API (client) models |
| `.../test/...` | Unit & integration tests |

## Domain Concepts
| Term | Description |
|------|-------------|
| Referral Code | Unique immutable code bound to a user |
| Submission | An attempt by a (submitted) user to use another user’s referral code |
| Active Submission | A submission in a state counted toward the active limit |
| Status Constraints | Prevent duplicate or excessive active submissions |

## Endpoints
| Method | Path | Purpose | Notes |
|--------|------|---------|-------|
| GET | `/client-api/v1/referral/generation` | Generate or fetch existing referral code | Idempotent per user |
| POST | `/client-api/v1/referral/submission` | Submit usage of a referral code | Enforces max active limit |
| GET | `/client-api/v1/referral/search` | Search referral codes | Filterable |
| GET | `/client-api/v1/referral/submission/search` | Search referral submissions | Pagination & filters |

## Error Handling
Uses Backbase style exceptions (e.g. `BadRequestException`) with message keys such as:
- `MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED`

Returned payloads should map code / message / parameters (extend as needed).

## Build & Run
```bash
mvn clean verify
mvn spring-boot:run
```
## Run In Local With Existing Configuration
```bash
remove the scope mysql-connector-j dependency 
mvn spring-boot:run -DSIG_SECRET_KEY=JWTSecretKeyDontUseInProduction!
DB_PORT=3306 
DB_HOST=localhost 
DB_NAME=referral_service 
DB_USERNAME=root 
DB_PASSWORD=secret
```

## Testing
```bash
mvn test          # Unit tests
mvn verify        # Includes integration tests (Testcontainers if configured)
```

## Deployment (Example)
```bash
mvn -DskipTests package
docker build -t referral-service:local .
docker run -p 8080:8080 referral-service:local
```

## Observed Behavioral Rules
| Rule | Rationale |
|------|-----------|
| One referral code per user | Simplifies idempotency & sharing |
| Reject submissions exceeding active quota | Abuse & spam prevention |
| Filterable searches with pagination | Performance & UX |
| Optional QR generation flag | Distribution channels flexibility |

## Configuration (Only `backbase.referral.*`)
| Property Key | Description                                                                                                   | Type    | Default                           |
|--------------|---------------------------------------------------------------------------------------------------------------|---------|-----------------------------------|
| `backbase.referral.userActiveSubmissionLimit` | The total number of active referral submissions by a user                                                     | int     | 1                                 |
| `backbase.referral.referralExpiryEnabled` | If the referral code can expire                                                                               | boolean | false                             |
| `backbase.referral.referralExpiryInHours` | The number of hours for which a referral code can be valid (required only if referralExpiryEnabled is true)   | int     | 0                                 |
| `backbase.referral.referralStatusRefreshCronExpression` | Refresh referral code status at specified intervals if referralExpiryEnabled is true                          | string  | "0 */5 * * * *" (Every 5 minutes) |
| `backbase.referral.reGenerationUponExpiryEnabled` | If the referral code for a user needs to be re-generated if the previous one is expired                       | boolean | false                             |
| `backbase.referral.submissionExpiryEnabled` | If the submission can expire                                                                                  | boolean | false                             |
| `backbase.referral.submissionExpiryInHours` | The number of hours for which a submissions can be valid (required only if submissionExpiryEnabled is true)   | int     | 0                                 |
| `backbase.referral.submissionStatusRefreshCronExpression` | Refresh submission status at specified intervals if submissionExpiryEnabled is true                           | string  | "0 */5 * * * *" (Every 5 minutes) |
| `backbase.referral.submissionExpiryUponReferralExpiryEnabled` | If the submission should expire upon referral code expiry                                                     | boolean | false                             |
| `backbase.referral.unknownReferralCodeSubmissionEnabled` | If the user is allowed to submit referral code that is not generated by the system                            | boolean | true                              |
| `backbase.referral.selfReferralAllowed` | If the user is allowed to submit self-referral code                                                           | boolean | false                             |
| `backbase.referral.referralCodeLengthWithoutPrefix` | The total characters to be present in referral code excluding prefix                                          | int     | 7                                 |
| `backbase.referral.referralCodePrefix` | The Prefix to be added for referral codes generated                                                           | string  | EMPTY                             |
| `backbase.referral.referralCodePatternType` | The referral code pattern type to be used while generating the code: ALPHANUMERIC, NUMERIC, ALPHABETIC, REGEX | string  | ALPHAMUMERIC                      |
| `backbase.referral.referralCodeRegexPattern` | The valid regex pattern to generate the referral code (required if pattern type is REGEX)                     | string  | EMPTY                             |
| `backbase.referral.referralCodeQREnabled` | If we need a QR String that contain referral should also be needed in response                                | boolean | true                              |
| `backbase.referral.referralCodeGenerationMaxAttempts` | The maximum number of attempts required to generate referral code                                             | int     | 100                               |



## Example Scenario
1. User A requests generation -> receives stable code.
2. User B submits A's code -> counted as active submission.
3. Additional submission exceeding quota -> rejected with `MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED`.

## Testing Focus
| Test Concern | Purpose |
|--------------|---------|
| Idempotent generation | Same user gets same code |
| Active limit | Enforces `backbase.referral.max-active-submissions` |
| Search filters | Date range, code fragment, status |
| Error responses | Proper keys & HTTP status |

## Operational Checklist
| Item | Status |
|------|--------|
| Health endpoint | Add Actuator if not present |
| Readiness probe | Expose `/actuator/health` |
| Metrics | Pending |
| Tracing | Pending |
| CI pipeline | Add build + test + scan |
