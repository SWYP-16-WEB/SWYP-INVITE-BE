# SWYP-INVITE-BE

Java 21, Spring Boot 4, MySQL, Gradle 프로젝트.

## 작업 규칙

- 브랜치 생성, 커밋, PR 작성 전에 `docs/git-convention.md`를 읽고 따른다.
- `docs/backend.md`를 고칠 때는 `docs/backend-intake-protocol.md`와 `docs/swyp-team-rules.md`를 따른다. 절 구조를 확인할 때만 `docs/backend-template.md`를 본다.
- 코드를 추가하기 전에 `docs/architecture.md`를 읽고 따른다.

## 명령어

- 빌드와 테스트: `./gradlew build` (Docker 필요)
- 코드 모양 맞추기: `./gradlew spotlessApply`
- 로컬 실행: `./gradlew bootRun` (MySQL 컨테이너 자동 기동)
