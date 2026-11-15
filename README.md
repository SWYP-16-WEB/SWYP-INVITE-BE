# SWYP-INVITE-BE

Java 21, Spring Boot 4, MySQL 8.4, Swagger(springdoc)

## 실행 방법

사전 준비: JDK 21, Docker

```bash
./gradlew bootRun
```

- MySQL 컨테이너는 `compose.yaml`로 자동 기동되고 접속 정보도 자동으로 주입된다.
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 빌드와 테스트

```bash
./gradlew build
```

테스트는 Testcontainers로 MySQL을 띄우므로 Docker가 실행 중이어야 한다.

## 배포 환경 변수

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db>
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
```

## 문서

- [Git 컨벤션](docs/git-convention.md)
