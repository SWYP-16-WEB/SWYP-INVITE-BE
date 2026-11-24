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

### 로컬 DB 초기화

테이블은 서버가 뜰 때 Flyway가 `src/main/resources/db/migration`의 SQL로 만든다. 출시 전에는 `V1__init.sql`을 고칠 수 있는데, 이미 적용한 로컬 DB에서는 내용이 바뀐 것을 알아채고 서버가 뜨지 않는다. 그때는 로컬 DB를 지우고 다시 띄운다. 로컬 데이터는 사라진다.

```bash
docker compose --profile app down
```

## 빌드와 테스트

```bash
./gradlew build
```

테스트는 Testcontainers로 MySQL을 띄우므로 Docker가 실행 중이어야 한다.

코드 모양이 포매터와 다르면 빌드가 실패한다. `./gradlew spotlessApply`로 맞춘다.

## 카카오 로그인

로그인은 브라우저를 `GET /api/v1/auth/login/kakao`로 보내면 시작된다. 끝나면 서버가 프론트엔드의 `/login/callback`으로, 실패하면 `/login/failed?error=KAKAO_AUTH_FAILED`로 보낸다.

카카오 키가 없어도 서버는 뜬다. 카카오 로그인만 실패한다.

실제로 로그인해 보려면 카카오 개발자 사이트에서 앱을 만들고 `.env`에 값을 넣는다.

```
KAKAO_CLIENT_ID=앱의 REST API 키
KAKAO_CLIENT_SECRET=앱의 Client Secret
FRONTEND_URL=http://localhost:8080
```

카카오 앱 설정

- 카카오 로그인을 켠다.
- 동의 항목에서 닉네임을 필수 동의로 둔다.
- Client Secret을 켠다.
- 돌아올 주소(Redirect URI)에 `{FRONTEND_URL}/api/v1/auth/callback/kakao`를 등록한다. 주소마다 따로 등록한다.

프론트엔드 없이 볼 때는 `FRONTEND_URL`을 서버 주소(`http://localhost:8080`)로 두고 브라우저에서 `http://localhost:8080/api/v1/auth/login/kakao`를 연다. 로그인이 끝나면 `/login/callback`으로 이동하고 404 응답이 보인다(그 화면은 프론트엔드의 것이다). 세션 쿠키는 내려와 있다.

프론트엔드와 함께 볼 때는 `FRONTEND_URL`을 프론트엔드 주소로 둔다. 비워 두면 `http://localhost:3000`이다.

로그인이 실패하면 서버 로그에 `카카오 로그인 실패`와 이유가 남는다.

## 배포 환경 변수

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db>
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
PII_ENCRYPTION_KEY=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
FRONTEND_URL=
```

`PII_ENCRYPTION_KEY`는 32바이트를 Base64로 적은 값이다. 없으면 서버가 뜨지 않는다. 로컬에서 쓰던 값을 운영에 쓰지 않는다.

`FRONTEND_URL`은 사용자가 브라우저에서 보는 프론트엔드 주소다(끝에 `/`를 붙이지 않는다). 카카오가 돌아올 주소와 로그인 뒤 이동할 화면의 주소를 이 값으로 만든다.

## 문서

- [Git 컨벤션](docs/git-convention.md)
- [코드 구조](docs/architecture.md)
