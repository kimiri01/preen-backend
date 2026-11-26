# preen-backend 작업 규칙

숙박 플랫폼 preen의 백엔드다. Spring Boot 4 + Java 21 + Spring Data JPA + MySQL + Flyway + Redis + Spring Security(JWT)를 쓴다.
기본 패키지는 `com.preen.preen_backend`이고, 프론트엔드는 `../preen-frontend`(React)다.

## 작업 전후에 반드시 지킬 것

- 요청받은 범위만 수정한다. 요청받지 않은 기존 코드는 리팩터링하거나 이름을 바꾸지 않는다. 고치면 좋을 부분을 발견하면 수정하지 말고 보고한다.
- `common/` 아래 공통 코드(응답·예외·보안·설정)와 디렉터리 구조를 바꿔야 하면 먼저 사용자에게 확인을 받는다.
- 작업이 끝나면 아래 명령을 실행해서 통과하는지 확인하고 결과를 알려준다.

```bash
./gradlew test               # 기본 테스트 (DB가 필요한 integration 태그는 제외)
./gradlew integrationTest    # 로컬 MySQL이 켜져 있을 때만. DB 관련 변경 시 실행
```

## 1. 패키지 구조

```text
com.preen.preen_backend
├── common/        여러 기능이 쓰는 코드 (config, entity, exception, response, security, util)
└── <기능>/        accommodation, auth, ...
    ├── controller/   HTTP 요청을 받고 Service를 호출해 ApiResponse로 반환
    ├── service/      업무 규칙, 검증, 트랜잭션
    ├── repository/   Spring Data JPA
    ├── entity/       JPA Entity (기존 accommodation은 domain/을 쓴다. 그 기능 안에서는 기존 폴더를 따른다)
    ├── dto/          요청·응답 record
    ├── exception/    <기능>ErrorCode enum
    ├── config/       기능 전용 설정, SecurityRuleContributor 구현체
    └── event/        메일 같은 부가 작업의 Event·Listener
```

- `common`은 기능 패키지를 import하지 않는다.
- 기능끼리 직접 의존은 최소화한다. 다른 기능의 Entity가 필요하면 Entity 대신 ID(`Long`)를 넘긴다.

## 2. 이름 규칙

- 메서드 이름에 동작과 대상을 드러낸다: `getDetail`, `createAccommodation`, `cancelReservation`
- 조회 전용 Service는 `~QueryService`, 생성·수정·삭제 Service는 `~CommandService`로 짓는다 (예: `AccommodationDetailQueryService`, `HostAccommodationCommandService`).
- DTO: `~Request`, `~Response`. 여러 조건을 묶은 `if`문은 의미를 드러내는 `boolean` 변수로 뺀다.

```java
// 예시 코드 (Reservation, ReservationErrorCode는 새 기능을 가정한 이름)
boolean isOwner = reservation.getUserId().equals(user.userId());
if (!isOwner) {
    throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
}
```

## 3. Controller와 응답

- 성공 응답은 항상 `common.response.ApiResponse.ok(data)`로 감싼다. 프론트는 `ApiResponse<T>` 형식(`success`, `code`, `message`, `data`)을 기대한다.
- 목록(페이지) 응답은 `PageResponse.of(page, mapper)`를 `ApiResponse`에 담는다.
- 상태 코드만 바꾸면 `@ResponseStatus`, 헤더·쿠키가 필요하면 `ResponseEntity<ApiResponse<T>>`를 쓴다. 본문이 없는 성공은 `ResponseEntity<Void>`로 `204 No Content`를 반환한다.
- Controller에는 업무 로직을 넣지 않는다. Service 호출과 응답 변환만 한다.

```java
@GetMapping("/{accommodationId:\\d+}")
public ApiResponse<AccommodationDetailResponse> getDetail(@PathVariable("accommodationId") Long accommodationId) {
    return ApiResponse.ok(accommodationDetailQueryService.getDetail(accommodationId));
}

@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<HostAccommodationCreateResponse> create(@Valid @RequestBody HostAccommodationCreateRequest request, ...) {
    ...
}
```

## 4. 예외 처리

- 업무 오류는 `common.exception.BusinessException`에 `ErrorCode`를 담아 던진다. `GlobalExceptionHandler`가 `ApiResponse.failure(...)`로 바꿔 준다.
- 공통 오류는 `CommonErrorCode`(`INVALID_INPUT`, `TOO_MANY_REQUESTS`, `INTERNAL_ERROR`), 기능 오류는 `<기능>/exception/<기능>ErrorCode` enum에 정의한다. 본보기: `auth/exception/AuthErrorCode`.
- 오류 코드에는 HTTP 상태와 사용자에게 보여 줄 한국어 메시지를 함께 둔다.
- 새 코드에서 `ResponseStatusException`, `RuntimeException`, `IllegalArgumentException`을 직접 던지지 않는다. Controller에서 try-catch로 오류 응답을 직접 만들지 않는다.

```java
// ErrorCode 정의
public enum ReservationErrorCode implements ErrorCode {
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "선택한 날짜에 예약할 수 없는 객실입니다.");

    private final HttpStatus status;
    private final String message;
    ReservationErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    public HttpStatus status() { return status; }
    public String message() { return message; }
}

// ✅ 처리할 수 없는 조건을 먼저 확인하고, 그 아래에 정상 흐름
Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

// ❌
throw new RuntimeException("예약 없음");
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 없음");
```

## 5. DTO와 Entity

- DTO는 Java `record`로 만든다. Entity를 Controller 응답으로 그대로 반환하지 않는다.
- Entity → DTO 변환은 DTO 안의 정적 메서드 `of()`로 만든다 (기존 `from()`은 그대로 둔다).
- 요청 DTO에는 `jakarta.validation` 어노테이션(`@NotBlank`, `@NotNull` 등)을 붙이고, Controller에서 `@Valid`로 검증한다.
- Entity의 생성 시각은 `BaseCreatedEntity`, 생성·수정 시각은 `BaseTimeEntity`를 상속한다.
- 의존성 주입은 `private final` 필드 + Lombok `@RequiredArgsConstructor`로 한다.

```java
public record ReservationResponse(Long id, String accommodationName, LocalDate checkIn) {
    public static ReservationResponse of(Reservation reservation, String accommodationName) {
        return new ReservationResponse(reservation.getId(), accommodationName, reservation.getCheckIn());
    }
}
```

## 6. 인증과 인가

- 모든 API는 기본적으로 로그인이 필요하다 (`SecurityConfig`의 `anyRequest().authenticated()`).
- 로그인 없이 열 API는 `SecurityConfig`를 고치지 말고, 기능의 `config/`에 `SecurityRuleContributor`를 구현한 `@Component`를 만든다. 본보기: `accommodation/config/AccommodationSecurityRules`.

```java
@Component
public class ReviewSecurityRules implements SecurityRuleContributor {
    @Override
    public String[] publicGetPaths() {
        return new String[] {"/api/reviews/**"};
    }
}
```

- 로그인 사용자 정보는 `@AuthenticationPrincipal AuthenticatedUser user`로 받는다. `user.userId()`, `user.role()`(`UserRole.GUEST/HOST/ADMIN`)을 쓴다.
- 요청 본문·헤더·쿼리로 받은 회원 ID는 믿지 않는다. 항상 `AuthenticatedUser`의 ID를 쓴다.
- 본인 리소스만 다루는 기능은 Service에서 요청 사용자 ID와 리소스 소유자 ID를 비교한다.
- HOST·ADMIN 전용 기능은 `user.role()`을 확인해서 권한이 없으면 `BusinessException`을 던진다. (전용 어노테이션은 아직 없다.)
- JWT·CORS·CSRF 설정은 `common/security`를 따른다. 요청 횟수 제한은 `common.util.RateLimiter`의 `check(key, limit)`을 쓴다.

## 7. 설정과 비밀값

- 설정 파일은 `src/main/resources/application.properties`다.
- 기능 설정은 `app.<기능명>.*` 키로 두고 `@ConfigurationProperties("app.<기능명>")` 클래스로 읽는다. 본보기: `auth/config/AuthProperties`.
- DB 비밀번호, JWT 키, 메일 계정 같은 비밀값은 코드나 `application.properties`에 적지 않는다. `${환경변수}`로 읽고 실제 값은 `.env`에 넣는다. 새 환경변수를 추가하면 `.env.example`에 이름과 설명을 추가한다(값은 비운다).
- `.env`는 Git에 올리지 않는다(이미 `.gitignore`에 있다).

## 8. DB 변경 (Flyway)

- `spring.jpa.hibernate.ddl-auto=validate`다. 테이블 구조는 `src/main/resources/db/migration`의 SQL 파일로만 바꾼다. Entity만 바꾸면 서버가 켜지지 않는다.
- 새 파일 이름은 `V<날짜>.<시분>__<설명>.sql` 형식이다. 예: `V20261126.1530__add_reservation_memo.sql`
- 이미 develop에 올라간 SQL 파일은 절대 수정하지 않는다. 바꿀 내용은 `ALTER TABLE`이 들어간 새 파일로 추가한다.
- Entity의 컬럼 이름·타입을 SQL과 맞춘다. 자세한 규칙은 `docs/db-migration-guide.md`를 따른다.

## 9. 기존 유틸리티

- 이메일을 저장·비교하기 전에 `EmailUtil.normalize(email)`을 쓴다.
- 랜덤 토큰은 `TokenUtil.randomToken()`, 인증번호는 `TokenUtil.verificationCode()`를 쓴다. 직접 만들지 않는다.

## 10. 코드 스타일

- 들여쓰기는 수정하는 파일의 기존 방식(탭 또는 공백 4칸)을 따른다.
- 주석은 한국어로, 클래스와 공개 메서드에 무엇을 하는지 한 줄로 단다.
- 새 Service·기능에는 `src/test/java`에 같은 패키지 구조로 테스트를 추가한다. DB가 필요한 테스트에는 `@Tag("integration")`을 붙인다.

## 11. Git

- `main`(배포)과 `develop`(통합)에 직접 커밋하지 않는다. `develop`에서 `feature/<작업-내용>` 브랜치를 만든다. 예: `feature/reservation-api`
- 커밋 제목은 `type:제목` 형식. 본문에는 무엇을 왜 바꿨는지 적는다.
  type: `feat` `fix` `docs` `style` `refactor` `test` `build` `ci` `perf` `chore`

```text
feat:예약 생성 API 추가

객실 재고를 확인한 뒤 예약을 생성하고, 재고가 없으면 ROOM_NOT_AVAILABLE 오류를 반환한다.
```

- PR 본문 순서: 1. 작업 내용 2. 변경 사항 3. 확인 포인트(확인한 방법·결과) 4. 관련 이슈·API 명세. 리뷰어 2명 승인 후 머지한다.
- 커밋·푸시·PR 생성은 사용자가 요청했을 때만 한다.
