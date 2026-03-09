# bank-app

Это микросервисный проект с аутентификацией и авторизацией. Проект состоит из 5 микросервисов:
bank-ui(frontend),cash-service(backend сервиса наличных), account-service(backend сервиса аккаунтов пользователей)
,transfer-service(backend сервиса переводов между счетами),notification-service(backend сервиса уведомлений).

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/schema.png?raw=true)

## bank-ui

- Данный проект используется как frontend-часть банковского приложения на базе шаблонизатора Thymeleaf.
- Это приложение позволяет пользователю через UI-интерфейс взаимодлействовать с остальными микросервисами(backend).
- Взаимодействие с account-service через api-gateway - для просмотра и изменения личных данных пользователя.
- Взаимодействие с cash-service через api-gateway - для снятия или пополнения своего счета в банке.
- Взаимодействие с transfer-service через api-gateway - для переводов между своим счетом и другим.

### Безопасность

Система безопасности включает в себя:

- Сервер Авторизации(OAUTH2.0)-Keycloak
- Аутентификация - предварительно зарегестрированный пользователь банка использует форму входа внешнего сервера
  авторизации 'Keycloak',
  чтобы получить доступ к защищенным рерурсам приложения.
- Авторизация - пользователь имеет роль, которая определяет его привелегии. На текущий момент, у пользователя может быть
  роль 'USER'(стандартная) и привелегии ('ACCOUNT_WRITE, 'TRANSFER_WRITE','CASH_WRITE') для выполнения соотв. операций.
- Access token + token id - содержит информацию определенную в 'scope', в том числе и роли. Учитывая что используется '
  authorization code flow'
  передается также и token id (OIDC), который содержит идентификацию пользователя.
  Токен передается как bearer-токен с запросом при взаимодействии с другими сервисами через 'api-gateway'.

### Пример логина и просмотра информации о счете

Как только сы попытаемся зайти на стартовую страницу приложения то нас сразу перенаправит на страницу логина.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/keycloak_login.png?raw=true)

Далее, после ввода логина и пароля мы попадаем на главную стрницу нашего банковоского приложения

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/start_page.png?raw=true)

## account-service

- Данный проект используется как backend-часть банковского приложения описывающее предметную область счёта пользователя.
- По сути это один из основных микросервисов , ведь с ним взаимодействует не только 'bank-ui', но и 'cash-service','
  transfer-service','notification-service'.
- Содержит ендпоинты для получения инфы о авторизованном аккаунте, для изменения личных данных этого аккаунта.
- Также содержит ендпоинты для атомарного изменения баланса пользователя(увеличения и уменьшения).Используются другими
  микросервисами.

### Безопасность

Система безопасности включает в себя:

- Для авторизации используется OAUTH2.0 и 'client credentails flow' без вмешивания пользвоателя в процесс(
  service-service).
- Access token - содержит информацию определенную в 'scope', в том числе и роли.
- Роли на текущий момент: 'SERVICE'-роль сервиса(не пользователя) и разрешения 'NOTIFICATION-WRITE'.
- Токен передается как bearer-токен с запросом при взаимодействии с другими сервисами напрямую без 'api-gateway'.

### Пример обновления информации владельца

После аутентификации мы можем изменить данные счета взаимодейстивуя с 'account-service'. К примеру добавим отчество.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/account_updated.png?raw=true)

Видно, что информация обновилась успешно. Но что если мы ошибемся и изменим дату рождения (менее 18 лет).

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/account_not_updated_age.png?raw=true)

Видим сообщение системы валидации о недопустимости возраста.

Кстати , если скажем сервис по каким то причным будет недоступен, то пользвоатель увидит сообщение об этом.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/error_account_service_priority.png?raw=true)

Аналогичные сообщения об ошибках есть и при взаимодействии с другими микросервисами.

## cash-service

- Данный проект используется как backend-часть банковского приложения описывающее предметную область снятия(внесения)
  наличных..
- В зависимости от действия(снять или положить), создается бизнес транзакция, вызывается соответсующий ендпоинт  '
  account-service'
  и баланс меняется, после чего статус транзакции тоже обновляется. Ественнно могут быть проблемы , например при снятии
  недостаточно средств.
  Тогда тразакция сохраняется со статусом фейла и причиной.
- Также этот микросервис отправляет уведомления(outbox transaction) в 'notification-service'.

### Безопасность

Система безопасности включает в себя:

- Раздел безопасности тут аналогичен 'account-service', отличаются роли и привелегии.
- Роли: 'SERVICE'-роль сервиса(не пользователя) и разрешения 'ACCOUNT-WRITE','NOTIFICATION-WRITE'.
- Токен также передается как bearer-токен с запросом при взаимодействии с другими сервисами напрямую без 'api-gateway'.

#### Пример пополнения собственного счета

К примеру мы решили положить деньги на свой счет.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/cash_put.png?raw=true)

Видим что операция пополнения прошла успешно, было 10к стало 20к.

## transfer-service

- Данный проект используется как backend-часть банковского приложения описывающее предметную область переводов между
  счетами.
- При переводе создается бизнес транзакция, вызывается ендпоинт снятия денег 'account-service' со счета владельца,
  а замем енпоинт пополнения другого счета 'account-service'. В зависимости от результата выставляется статус
  транзакции.
  Может не хватать средств на счете или скажем целевой счет может не существовать, тогда списанные со счета владельца
  деньги надо вернуть.
  Для этого предусмотрена компенсация.Деьги возращаются и статус транзакции меняется.
- Также этот микросервис отправляет уведомления(outbox transaction) в 'notification-service'.

### Безопасность

Система безопасности включает в себя:

- Раздел безопасности тут аналогичен 'account-service','cash-service' отличаются роли и привелегии.
- Роли: 'SERVICE'-роль сервиса(не пользователя) и разрешения 'ACCOUNT-WRITE','NOTIFICATION-WRITE'.
- Токен также передается как bearer-токен с запросом при взаимодействии с другими сервисами напрямую без 'api-gateway'.

#### Пример перевода со своего счёта на другой

К примеру мы решили перевести пользователю 'Sergeev Sergey' 10k

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/transfer_sucess.png?raw=true)

Видим что операция перевода средств прошла успешно и у владельца опять стало 10k.

Опять же если мы захотим перевести очень большие средства, которых у нас нет на счету, то выйдет ошибка о недостатке
средств.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/InfulussinetFunds.png?raw=true)

А если у нас выполнится снятие средств со счета владельца, а при зачислении произойдёт какая-либо ошибка, то произойдет
компенсация.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/error_compensated.png?raw=true)

А пользователь увидит соовтествующее сообщение.

Посмотрим как выглядят транзакции переводов изнутри при разных исходах(в том числе и компенсации)

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/transfer_transaction_with_compen.png?raw=true)

## Планировщик отправки уведомлений 'account-service', 'cash-service', 'transfer-service'

- В данных сервисах присутсвует планировщик, который по расписанию находит отложенные задачи отправки уведомлений и
  отправляет их.

На прмиере 'transfer-service'

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/transfer_outbox.png?raw=true)

Видим выполненные задачи отправки(обработки) уведомлений. Аналогично и для других микросервисов.

## notification-service

- Данный микросервис получает(обрабатывает) входящие уведомления о событиях в других микросервисах.
- События обновления аккаунта, успешного снятия или списания или перевода.
- Сейчас сервис просто пишет в лог, но можно расширить функционал.

### Безопасность

Система безопасности включает в себя:

- Раздел безопасности тут аналогичен 'account-service','cash-service' в плане инфраструкуры.
- Однако этот сервис(серрвер ресурсов) только проверяет переданный от другого сервиса токен доступа через keycloak.

### Пример события которые записались в лог

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/notifications_log.png?raw=true)

Видим разные события от разных микросервисов.

## api-gateway, consul, keycloak

- keycloak - Сервер Авторизации(OAUTH2.0), который позволяет управлять процессом авторизации(аутентификации).
  Для работы нужно настроить пространоство, создать необходимые роли, создать клиентов(микросиервисы) и пользователя для
  ui.

  ![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/keycloak_realm.png?raw=true)

- consul - используется как 'сервер обнаружения служб', позволяет микросервисам не знать о нахождении друг друга(айпи),
  а просто обращаться по имени. Также используется как 'сервер конфигураций' для централизованного хранения конфига.

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/config.png?raw=true)

- api-gateway - шлюз , единаыя точка , маршрутизатор запросов, через него проивходит взаимодействие пользователя с
  микросервисами.




