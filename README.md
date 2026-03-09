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
- По сути это один из основных микросервисов , ведь с ним взаимодействует не только 'bank-ui', но и 'cash-service','transfer-service','notification-service'.
- Содержит ендпоинты для получения инфы о авторизованном аккаунте, для изменения личных данных этого аккаунта.
- Также содержит ендпоинты для атомарного изменения баланса пользователя(увеличения и уменьшения).Используются другими микросервисами.

### Безопасность

Система безопасности включает в себя:

- Сервер Авторизации(OAUTH2.0)-Keycloak
- Для авторизации используется OAUTH2.0 и 'client credentails flow' без вмешивания пользвоателя в процесс(service-service).
- Access token  - содержит информацию определенную в 'scope', в том числе и роли.
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
- Данный проект используется как backend-часть банковского приложения описывающее предметную область снятия(внесения) наличных..
- В зависимости от действия(снять или положить), создается бизнес транзакция, вызывается соответсующий ендпоинт  'account-service' 
  и баланс меняется, после чего статус транзакции тоже обновляется. Ественнно могут быть проблемы , например при снятии недостаточно средств.
  Тогда тразакция сохраняется со статусом фейла и причиной.
- Также этот микросервис отправляет уведомления(outbox transaction) в 'notification-service'.

### Безопасность

Система безопасности включает в себя:

- Раздел безопасно тут аналогичен 'account-service', отличаются роли и привелегии.
- Роли: 'SERVICE'-роль сервиса(не пользователя) и разрешения 'ACCOUNT-WRITE','NOTIFICATION-WRITE'.
- Токен также передается как bearer-токен с запросом при взаимодействии с другими сервисами напрямую без 'api-gateway'.

#### Пример пополнения собственного счета

К примеру мы решили положить деньги на свой счет,

![alt text](https://github.com/AlexLakers/ParserJsonCsvToXml/tree/master/WinFormsCsvJsonXml/App_Data/pictures/micro_pict/cash_put.png?raw=true)