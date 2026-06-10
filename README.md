# Astrax Test Messenger by AI

MVP-мессенджер: Android-клиент на Kotlin/Jetpack Compose и backend на Kotlin/Ktor.

## Скачать APK

- [Astrax_1.0.apk](https://github.com/DenisVyukhin/astrax-test-messenger-by-ai/releases/download/v1.0.0/Astrax_1.0.apk)

Сборка предназначена для публичного тестирования с развернутым backend. Для своего сервера нужно пересобрать Android с нужным `astrax.baseUrl` в `local.properties`.

## Стек

**Android**

- Kotlin
- Jetpack Compose
- Material 3
- Ktor Client
- Kotlinx Serialization
- WebSocket-клиент
- SharedPreferences для хранения токена

**Backend**

- Kotlin
- Ktor Server + Netty
- REST API + WebSocket
- JWT-авторизация
- SQLite
- Exposed SQL DSL
- HikariCP
- Kotlinx Serialization
- Logback

**Инфраструктура**

- Gradle Kotlin DSL
- nginx + systemd для VPS-деплоя
- GitHub Releases для APK

## Защита

| От чего | Как сделано |
|---------|-------------|
| Неавторизованный доступ к API | Закрытые REST endpoints находятся внутри `authenticate("auth")`; без валидного JWT сервер возвращает ошибку авторизации. |
| Неавторизованный WebSocket | При подключении к `/ws/chats/{id}` сервер проверяет `Authorization: Bearer ...`; без валидного токена соединение закрывается. |
| Доступ к чужим чатам | Перед чтением сообщений, отправкой, удалением и изменением настроек вызывается проверка членства пользователя в чате (`requireMember`). |
| SQL-инъекции | Запросы к базе написаны через Exposed SQL DSL, а не через ручную склейку SQL-строк. Логины и поисковые запросы дополнительно проходят regex-валидацию. |
| Брутфорс логина/регистрации | Для `/auth/login` и `/auth/register` есть rate limiter: ограничение количества запросов с одного IP за окно времени. |
| Спам сообщениями | Отправка сообщений ограничена rate limiter на пользователя. |
| Утечка паролей | Пароли не хранятся в открытом виде; используется bcrypt-хеширование с cost `12`. |
| Подбор/подмена токена | JWT подписывается HMAC256, содержит issuer/audience, user id и срок жизни. Секрет задается через `ASTRAX_JWT_SECRET`. |
| Некорректные входные данные | Логин, пароль, поисковый запрос и текст сообщения валидируются на сервере по длине и формату. |
| Лишние CORS-домены | Разрешенные host'ы берутся из `ASTRAX_CORS_HOSTS`; для локальной разработки разрешены только локальные адреса. |

## Локальный запуск

### 1. Поднять backend

```bash
export ASTRAX_JWT_SECRET="replace-with-long-random-secret"
./gradlew :backend:run
```

Сервер стартует на:

```text
http://0.0.0.0:8080
```

Проверка:

```bash
curl http://localhost:8080/health
```

По умолчанию SQLite-файл создается как `astrax.db` в корне проекта.

### 2. Запустить Android в эмуляторе

Для Android Emulator backend на host-машине доступен по адресу `10.0.2.2`, поэтому можно оставить default:

```properties
astrax.baseUrl=http://10.0.2.2:8080
```

Сборка debug APK:

```bash
./gradlew :android:assembleDebug
```

APK будет здесь:

```text
android/build/outputs/apk/debug/android-debug.apk
```

### 3. Запустить Android на физическом телефоне

Телефон и компьютер должны быть в одной Wi-Fi сети. В `local.properties` укажите IP компьютера:

```properties
astrax.baseUrl=http://192.168.1.79:8080
```

Затем пересоберите APK:

```bash
./gradlew :android:assembleDebug
```

### 4. Release-сборка под свой сервер

```properties
astrax.baseUrl=https://astrax.example.com
```

```bash
./gradlew :android:assembleRelease
```

## Структура проекта

```text
Astrax/
  android/          # Android-приложение
  backend/          # Ktor REST + WebSocket server
  deploy/           # nginx/systemd/VPS deployment scripts
```

## Переменные окружения backend

| Переменная | Описание | Пример |
|------------|----------|--------|
| `ASTRAX_JWT_SECRET` | JWT-секрет для подписи токенов | `random-32-plus-chars` |
| `ASTRAX_DB_URL` | JDBC URL SQLite-базы | `jdbc:sqlite:/opt/astrax/data/astrax.db` |
| `ASTRAX_CORS_HOSTS` | Разрешенные CORS host'ы через запятую | `astrax.example.com` |
