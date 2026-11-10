# ums

## Тесты

```bash
./mvnw test
```

База для тестов поднимается сама, Docker не нужен.

## Сервис

```bash
docker compose up -d
./mvnw spring-boot:run
```

MySQL: `localhost:3306`, база `ums`, пользователь и пароль `ums`.
Администратор: `admin` / `admin12345`.
