# 코드 구조

## 패키지

기능별로 나누고, 각 기능 안은 계층별로 나눈다.

```
com.swyp.invite
├── event
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
├── user
├── checklist
├── rsvp
├── reminder
└── global
```

- `user`, `checklist`, `rsvp`, `reminder`도 `event`와 같은 하위 패키지를 쓴다. 하위 패키지는 필요할 때 만든다.
- 여러 기능이 같이 쓰는 코드는 `global`에 둔다.
- 호출은 controller → service → repository 방향으로만 한다.
- 테스트 코드도 같은 구조를 따른다. 테스트는 대상 클래스와 같은 패키지에 두고, 여러 기능의 테스트가 같이 쓰는 코드는 `global`에 둔다.

## 주석

- 코드와 설정 파일에 주석을 달지 않는다. 테스트 코드도 같다.

## 엔티티

- 세터를 두지 않는다. 값을 바꾸는 메서드는 그 기능의 담당이 엔티티에 더한다.
- 필수, 길이, 유니크 같은 제약은 마이그레이션 SQL에만 적고 엔티티에 다시 적지 않는다.
- 관계는 자식에서 부모로 가는 `@ManyToOne(fetch = FetchType.LAZY)`만 둔다. 연쇄 삭제는 DB의 외래 키가 한다.
- enum에는 `@Enumerated(EnumType.STRING)`과 `@JdbcTypeCode(SqlTypes.VARCHAR)`를 함께 붙인다.
- 개인정보 컬럼에는 `@Convert(converter = PiiEncryptionConverter.class)`를 붙인다.

## 에러 응답

- 실패는 `ApiException(ErrorCode)`를 던져 알린다. 응답은 `global.error.GlobalExceptionHandler` 한 곳에서 `{ "code", "message", "errors" }` 모양으로 만든다.
- 입력값은 요청 DTO에 Bean Validation을 붙이고 컨트롤러 파라미터에 `@Valid`를 붙여 검사한다. 걸린 필드는 `errors`에 담겨 나간다. 그 밖의 실패에서는 `errors`가 빈 배열이다.
- 에러 코드와 HTTP 상태의 기준은 `docs/backend.md` 05절의 에러 코드 표다. 표를 먼저 고치고 `ErrorCode`를 맞춘다. 표와 다르면 `ErrorCodeTest`가 실패한다.

## 인증과 권한

- 로그인 없이 부르는 API만 `global.security.SecurityConfig`의 공개 목록에 적는다. 적지 않은 경로는 로그인하지 않으면 401(`UNAUTHENTICATED`)이 된다. 기준은 `docs/backend.md` 05절 API 명세 표의 인증·권한 열이다.
- 쿠키로 회원을 확인하면서 값을 바꾸는 요청(POST·PUT·PATCH·DELETE)은 CSRF 토큰이 있어야 한다. 없거나 틀리면 403(`CSRF_TOKEN_INVALID`)이 된다. 쿠키로 회원을 확인하지 않는 공개 API는 `SecurityConfig`의 `NO_COOKIE_APIS`에 적어 토큰 없이 받는다.
- 컨트롤러 테스트(`@WebMvcTest`)에는 `@Import(SecurityConfig.class)`를 붙인다. 붙이지 않으면 스프링의 기본 설정이 걸려 모든 요청이 401이 된다. 값을 바꾸는 요청에는 `.with(csrf())`를 붙인다.

## DB 스키마

- 테이블은 `src/main/resources/db/migration`의 Flyway SQL로만 바꾼다. 엔티티는 서버가 뜰 때 검사만 받는다(`ddl-auto: validate`).
- 컬럼의 기준은 `docs/backend.md` 04절의 테이블 표다. 표를 먼저 고치고 SQL과 엔티티를 맞춘다. 표의 컬럼 이름과 실제 컬럼이 다르면 `SchemaTest`가 실패한다.
- 출시 전에는 `V1__init.sql`을 고친다. 고친 뒤에는 로컬 DB를 지우고 다시 띄운다(README의 「로컬 DB 초기화」).
- 검사가 잡지 못하는 필수, 유니크, 길이, 연쇄 삭제는 각 엔티티의 테스트(`EventTest` 등)처럼 `EntityTestSupport`를 상속해 실제 스키마를 상대로 테스트한다.
