# LastBot by excellent

Android-приложение (Kotlin + Jetpack Compose) для управления самокатом: unlock / lock / find / headlight.

- Вход через Telegram-бота по паролю (deep-link).
- Профиль (имя, @username, ID, аватарка) подтягивается из Telegram.
- Настройки: 5 тем (Orange, Ocean, Matrix, Violet, Light) и переключатель анимаций.
- BLE — заглушка: подключение имитируется, любая кнопка отвечает `BLE: handshake timeout`.

## Структура

```
app/                    — Android-приложение
  src/main/java/com/scooter/control/
    MainActivity.kt     — навигация + обработка deep-link scooterapp://auth?token=...
    ui/LoginScreen.kt    — экран входа (открывает Telegram-бота)
    ui/HomeScreen.kt     — управление + карточка BLE
    ui/ProfileScreen.kt  — профиль из Telegram
    ui/SettingsScreen.kt — темы и анимации
    data/ScooterRepository.kt — ЗАГЛУШКА BLE (замени на реальный BLE/API)
    data/SessionStore.kt — токен и профиль (DataStore)
    data/ProfileApi.kt   — загрузка профиля/аватарки с сервера бота
    data/SettingsStore.kt — тема и анимации
telegram_bot/bot.py     — Telegram-бот: спрашивает пароль, выдаёт deep-link
.github/workflows/build.yml — сборка debug APK в GitHub Actions
```

## BLE (заглушка)

Команды unlock/lock/find/headlight сейчас **ничего никуда не отправляют**.
`ScooterRepository` имитирует поиск и подключение по BLE, после чего любая
команда отвечает `BLE: handshake timeout`. Когда определишься с API на своей VPS, замени
тело методов `unlock()/lock()/find()` на реальные HTTP-запросы (например, через
Retrofit или Ktor Client) — сигнатуры менять не нужно.

## Telegram-бот (вход по ключу)

1. Создай бота через @BotFather, получи токен.
2. На VPS (x86_64):
   ```bash
   cd telegram_bot
   pip install -r requirements.txt
   export BOT_TOKEN=твой_токен_бота
   export BOT_PASSWORD=пароль_для_входа
   python bot.py
   ```
3. В `LoginScreen.kt` константа `TELEGRAM_BOT_URL` указывает на `https://t.me/jhabablastbot`.
   Для кнопки входа и загрузки профиля нужен https-адрес: `export PUBLIC_BASE_URL=https://myscooter.mooo.com`
   (reverse proxy на порт 8080). Бот отдаёт `/api/profile` и `/api/avatar`.
4. Пользователь пишет `/start` в бота → вводит пароль → получает кнопку
   «Войти в приложение» → она открывает `scooterapp://auth?token=...` →
   приложение логинит пользователя автоматически.

Для продакшена: замени хранение токенов в памяти бота (`_issued_tokens`) на
Redis/БД, и добавь на VPS эндпоинт, который твоё приложение будет дергать,
чтобы проверить валидность токена, а не просто доверять самому факту его
получения.

## Сборка через GitHub Actions

Workflow `.github/workflows/build.yml` запускается при пуше в `main` или
вручную (`workflow_dispatch`). Он:

1. Поднимает JDK 17.
2. Генерирует Gradle wrapper (`gradle wrapper --gradle-version 8.7`).
3. Собирает `assembleDebug`.
4. Загружает получившийся `.apk` как артефакт сборки — скачать его можно
   со страницы прогона workflow в GitHub (вкладка Actions → выбранный run →
   Artifacts → `lastbot-debug-apk`).

Чтобы это заработало, запушь этот проект в свой GitHub-репозиторий — ничего
дополнительно настраивать не нужно, секреты не требуются (пока бэкенд —
заглушка).

## Локальная сборка

Нужны Android Studio / Android SDK + JDK 17.

```bash
gradle wrapper --gradle-version 8.7   # один раз, если нет gradlew
./gradlew assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.
