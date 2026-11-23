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

## 엔티티

- 세터를 두지 않는다. 값을 바꾸는 메서드는 그 기능의 담당이 엔티티에 더한다.
- 필수, 길이, 유니크 같은 제약은 마이그레이션 SQL에만 적고 엔티티에 다시 적지 않는다.
- 관계는 자식에서 부모로 가는 `@ManyToOne(fetch = FetchType.LAZY)`만 둔다. 연쇄 삭제는 DB의 외래 키가 한다.
- enum에는 `@Enumerated(EnumType.STRING)`과 `@JdbcTypeCode(SqlTypes.VARCHAR)`를 함께 붙인다.
- 개인정보 컬럼에는 `@Convert(converter = PiiEncryptionConverter.class)`를 붙인다.

## DB 스키마

- 테이블은 `src/main/resources/db/migration`의 Flyway SQL로만 바꾼다. 엔티티는 서버가 뜰 때 검사만 받는다(`ddl-auto: validate`).
- 컬럼의 기준은 `docs/backend.md` 04절의 테이블 표다. 표를 먼저 고치고 SQL과 엔티티를 맞춘다.
- 출시 전에는 `V1__init.sql`을 고친다. 고친 뒤에는 로컬 DB를 지우고 다시 띄운다(README의 「로컬 DB 초기화」).
- 검사가 잡지 못하는 필수, 유니크, 길이, 연쇄 삭제는 `EntityMappingTest`처럼 실제 스키마를 상대로 테스트한다.
