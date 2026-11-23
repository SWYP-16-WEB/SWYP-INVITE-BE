# SWYP-INVITE-BE

Java 21, Spring Boot 4, MySQL 8.4, Swagger(springdoc)

## 실행 방법

사전 준비: JDK 21, Docker

처음 한 번, 개인정보 암호화 키를 넣은 `.env`를 만든다. 없으면 서버가 뜨지 않는다.

```bash
[ -f .env ] || echo "PII_ENCRYPTION_KEY=$(openssl rand -base64 32)" > .env
```

- `.env`는 git에 올라가지 않는다. 필요한 변수 이름은 `.env.example`에 있다.
- 값은 `이름=값` 형식으로 적고 따옴표와 공백을 넣지 않는다.

```bash
./gradlew bootRun
```

- MySQL 컨테이너는 `compose.yaml`로 자동 기동되고 접속 정보도 자동으로 주입된다.
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### Docker로 서버까지 띄우기

JDK 없이 Docker만으로 서버와 MySQL을 함께 띄운다. 로컬 실행용이며 배포 방식은 아직 정하지 않았다.

```bash
docker compose --profile app up --build
```

- 끝낼 때는 `Ctrl+C` 또는 `docker compose --profile app stop`. `down`은 MySQL 컨테이너까지 지워 로컬 DB 데이터가 사라진다.
- `./gradlew bootRun`과 같은 8080 포트를 쓰므로 둘을 동시에 띄울 수 없다.

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
PII_ENCRYPTION_KEY=
```

`PII_ENCRYPTION_KEY`는 32바이트를 Base64로 적은 값이다. 없으면 서버가 뜨지 않는다. 로컬에서 쓰던 값을 운영에 쓰지 않는다.

## 문서

- [Git 컨벤션](docs/git-convention.md)
