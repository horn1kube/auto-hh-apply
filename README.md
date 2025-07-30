# Auto Vacancy Reply (Vibecoding project)

Автоматическое приложение для откликов на вакансии HH.ru с веб-панелью управления.

## Описание

Это Spring Boot приложение автоматически:
- Загружает страницу подходящих вакансий с HH.ru
- Парсит все vacancy_id из HTML
- Отправляет отклики на новые вакансии через multipart/form-data
- Ведёт учёт откликов в SQLite базе данных
- Предоставляет веб-панель для управления

## Технологии

- **Java 21** - основной язык
- **Spring Boot 3** - веб-фреймворк
- **Gradle (Kotlin DSL)** - система сборки
- **SQLite** - база данных
- **Thymeleaf** - шаблонизатор
- **Jsoup** - парсинг HTML
- **Java HttpClient** - HTTP клиент с HTTP/2

## Быстрый старт

### 1. Клонирование и сборка

```bash
git clone <repository>
cd AutoVacancyReply
./gradlew build
```

### 2. Настройка .env

Скопируйте `env.example` в `.env` и настройте:

```bash
cp env.example .env
```

**ВНИМАНИЕ**: Файл `.env` содержит конфиденциальные данные и не должен попадать в git. Он уже добавлен в `.gitignore`.

Отредактируйте `.env` файл:

```env
# HH.ru Configuration
HH_BASE_URL=https://hh.ru
HH_RESUME_ID=ff2bb3eaff0c3774720039ed1f74766f434f79
HH_COOKIES=__ddg1_=...; hhuid=...; _xsrf=...; ...

# Поиск вакансий
HH_SEARCH_URL=https://hh.ru/search/vacancy?resume=${HH_RESUME_ID}&from=resumelist

# Отклик на вакансии
HH_APPLY_URL_TEMPLATE=https://hh.ru/applicant/vacancy_response/popup
HH_APPLY_BODY_TEMPLATE=resume_hash=${HH_RESUME_ID};vacancy_id={vacancyId};letterRequired=false;lux=true;ignore_postponed=true

# Настройки сети
USER_AGENT=Mozilla/5.0 (X11; Linux x86_64) Java21-AutoApply/1.0
HTTP_CONNECT_TIMEOUT_MS=10000
HTTP_READ_TIMEOUT_MS=20000
RATE_LIMIT_MIN_DELAY_MS=500
RATE_LIMIT_MAX_DELAY_MS=4000

# База данных
DB_PATH=data/app.db

# Режим работы
APPLY_DRY_RUN=false
```

### 3. Получение данных для .env

#### Cookie для HH.ru:
1. Откройте браузер и зайдите на hh.ru
2. Откройте DevTools (F12)
3. Перейдите на вкладку Network
4. Сделайте любой запрос к hh.ru
5. Скопируйте значение заголовка `Cookie` из запроса

#### Resume Hash:
1. Зайдите на страницу "Подходящие вакансии" на hh.ru
2. В URL будет параметр `resume=<hash>` - это и есть ваш resume_hash
3. Или найдите в запросах к popup эндпоинту

### 4. Запуск

```bash
./gradlew bootRun
```

Приложение будет доступно по адресу: http://localhost:8080

## Использование

### Веб-панель

1. Откройте http://localhost:8080
2. Нажмите кнопку "Запустить сейчас"
3. Следите за результатами в реальном времени

### Режим тестирования

Установите `APPLY_DRY_RUN=true` в `.env` для тестирования без отправки откликов.

## Архитектура

```
app/
├── Application.java              # Точка входа Spring Boot
├── config/
│   └── Env.java                 # Загрузка конфигурации из .env
├── http/
│   └── HttpClientFactory.java   # Фабрика HTTP клиентов
├── hh/
│   ├── HhClient.java            # Клиент для HH.ru API
│   └── parser/
│       └── SearchPageParser.java # Парсер HTML страниц
├── model/
│   ├── ApplyLog.java            # Модель логов
│   ├── ApplyResult.java         # Результат отклика
│   └── ApplySummary.java        # Сводка операции
├── service/
│   └── ApplyService.java        # Бизнес-логика
├── store/
│   └── SqliteStore.java         # Работа с БД
├── util/
│   ├── CookieUtils.java         # Утилиты для работы с cookies
│   └── Multipart.java           # Сборка multipart запросов
└── web/
    └── DashboardController.java  # Веб-контроллер
```

## Функциональность

### Поиск вакансий
- GET запрос к странице подходящих вакансий
- Парсинг HTML через Jsoup
- Извлечение vacancy_id по регулярному выражению
- Удаление дубликатов

### Анти-дубликаты
- Таблица `applied` в SQLite
- Проверка перед отправкой отклика
- Логирование всех операций

### CSRF защита
- Извлечение `_xsrf` из cookies
- Заголовок `X-XSRFToken` для всех запросов
- Дополнительные заголовки для имитации браузера

### Отклик на вакансии
- POST запрос с multipart/form-data
- Автоматическая подстановка значений
- Обработка ошибок с повторными попытками
- Случайные задержки между запросами

### Телеметрия (опционально)
- POST запрос для регистрации взаимодействия
- Не критично для основной функциональности

## Логирование

Логи сохраняются в:
- Консоль
- Файл `logs/app.log`

Форматы логов:
- `FOUND vacancyId=...` - найдена вакансия
- `SKIP duplicate=...` - пропущена (уже откликались)
- `APPLY OK vacancyId=...` - успешный отклик
- `APPLY FAIL vacancyId=... code=...` - ошибка отклика

## Безопасность

- Не используйте в продакшене без дополнительной защиты
- Храните .env файл в безопасном месте
- Регулярно обновляйте cookies
- Соблюдайте rate limiting

## Ограничения

- Только HTTP запросы (без Selenium/Playwright)
- Не обходит капчу
- Требует валидные cookies
- Соблюдает вежливые задержки

## Разработка

### Сборка
```bash
./gradlew build
```

### Тесты
```bash
./gradlew test
```

### Запуск в режиме разработки
```bash
./gradlew bootRun
```

## Устранение неполадок

### Ошибка "Illegal character in query"
Эта ошибка возникает, если в `.env` файле не настроены переменные или они содержат неправильные значения.

**Решение:**
1. Убедитесь, что файл `.env` существует и содержит все необходимые переменные
2. Проверьте, что `HH_RESUME_ID` и `HH_COOKIES` заполнены корректными значениями
3. Убедитесь, что `HH_SEARCH_URL` содержит правильный URL с подстановкой `${HH_RESUME_ID}`

### Ошибка "CSRF token not found"
Возникает, если в cookies отсутствует `_xsrf` токен.

**Решение:**
1. Обновите cookies в `.env` файле
2. Убедитесь, что вы залогинены в HH.ru
3. Скопируйте актуальные cookies из браузера

### Ошибка "Search page fetch failed"
Возникает при проблемах с доступом к HH.ru.

**Решение:**
1. Проверьте интернет-соединение
2. Убедитесь, что cookies не устарели
3. Попробуйте запустить в режиме тестирования (`APPLY_DRY_RUN=true`)

## Лицензия

MIT License 
