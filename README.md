# bank-app

Это микросервисный проект с аутентификацией и авторизацией. Проект состоит из 5 микросервисов:
bank-ui(frontend),cash-service(backend сервиса наличных), account-service(backend сервиса аккаунтов пользователей)
,transfer-service(backend сервиса переводов между счетами),notification-service(backend сервиса уведомлений).

![Схема микросервисного проекта](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/schema.png)

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

![логин](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/keycloak_login.png)


Далее, после ввода логина и пароля мы попадаем на главную стрницу нашего банковоского приложения

![главная](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/start_page.png)

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

![апдейт аккаунта](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/account_updated.png)

Видно, что информация обновилась успешно. Но что если мы ошибемся и изменим дату рождения (менее 18 лет).

![фел апдейтаккаунта](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/100.png)


Видим сообщение системы валидации о недопустимости возраста.

Кстати , если скажем сервис по каким то причным будет недоступен, то пользвоатель увидит сообщение об этом.

![ошибка сервиса аккаунта](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/error_account_service_priority.png)


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

![сервис кешположить](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/cash_put.png)

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

![сервис переводов](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/transfer_sucess.png)


Видим что операция перевода средств прошла успешно и у владельца опять стало 10k.

Опять же если мы захотим перевести очень большие средства, которых у нас нет на счету, то выйдет ошибка о недостатке
средств.

![недостаточно средств](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/InfulussinetFunds.png)


А если у нас выполнится снятие средств со счета владельца, а при зачислении произойдёт какая-либо ошибка, то произойдет
компенсация.

![компенсация](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/error_compensated.png)


А пользователь увидит соовтествующее сообщение.

Посмотрим как выглядят транзакции переводов изнутри при разных исходах(в том числе и компенсации)

![транзакции переводов](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/transfer_transaction_with_compen.png)

## Планировщик отправки уведомлений 'account-service', 'cash-service', 'transfer-service'

- В данных сервисах присутсвует планировщик, который по расписанию находит отложенные задачи отправки уведомлений и
  отправляет их.

На прмиере 'transfer-service'

![задачи оутбокс](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/transfer_outbox.png)

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

![сервис уведомлений](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/notifications_log.png)

Видим разные события от разных микросервисов.

## api-gateway, consul, keycloak

- keycloak - Сервер Авторизации(OAUTH2.0), который позволяет управлять процессом авторизации(аутентификации).
  Для работы нужно настроить пространоство, создать необходимые роли, создать клиентов(микросиервисы) и пользователя для
  ui.

![сервис автооизации](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/keycloak_realm.png)

- consul - используется как 'сервер обнаружения служб', позволяет микросервисам не знать о нахождении друг друга(айпи),
  а просто обращаться по имени. Также используется как 'сервер конфигураций' для централизованного хранения конфига.
  В проекте вы найдете в папке 'consul' свойства которые могут быть перенесены как обшие для всех проектов в консул.
![сервис конфиг](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/config.png)

- api-gateway - шлюз , единаыя точка , маршрутизатор запросов, через него проивходит взаимодействие пользователя с
  микросервисами.

## Сборка

### Docker
 Сборка осуществляется с помощью  инфраструктуры Docker с использованием Docker compose.

- В корневой папке микросервисного проекта вы найдете файл для сборки:  ```compose.yaml```
  В нем описаны все службы(контейнеры), которые необходимы для работы приложения в целом.
- В папке каждого модуля располагаются файлы сборки образа:```bank-ui/Dockerfile```,```account-service/Dockerfile```....
  Для сборки и запуска приложения(всех сервисов) просто запустите из корня проекта следующую команду.

  ```
  docker compose up -d
  ```

- В результате поднимятся 3 базы данных Postgres, фронтенд , микросервисы аккаунтов, наличных и переводов. Также поднимется
Consul(конфиг сервер и реест сервисов) r и сервер авторизации Keycloak во внетренне сети докера.

### ⚠️ Важное примечание

- Обратите внимание , вам надо создать файл ```.env``` с перемнными, которые описаны в ```.env.example```

```
docker compose up -d
```

### Kubernetes
Также вы можете развернуть микросервисное приложение используя ```Kubernetes``` и пакетный менеджер ```Helm``` для упрощения описания сервисов.
В качестве кластера будет использовано стандарное решение от разработчиков Kubernetes - ```Minikube```.
Микросервисное приложение будет разворачиваться как зонтичный чарт с подчартами.

- Предварительно нужно установить ```Minikube```, ```kubectl```.
- Запускаем кластер
  ```
  minikube start
  ```

- Собираем Docker-images для каждого сервиса внутри кластера Minikube. Из папки проекта.
 ```
  eval $(minikube docker-env)
  docker build -f bank-ui/Dockerfile -t local/ui:latest .
  docker build -f account-service/Dockerfile -t local/account:latest .
  ......
  ```
- В корне проекта откройте(перейдите) в каталог ```bank-umbrella```
- Можно использовать разные пространства для логического разделения в кластере, мы используем ```test```.
- Запустите команду для сборки зависимостей, указанных в зонтичном(родительском) файле 'Chart.yaml'.
  ```
  helm dependency update
  ```
- Запустите команду для анализа и валидации Helm-chart 'my-bank'.

  ```
  helm lint .
  ```
  ![lint](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/lint.png)

- Запустите команду для ручной проверки(пасринга) всем описаний-манифестов
  ```
  helm upgrade --install my-bank . --namespace test --create-namespace --dry-run --debug \
  --set postgres-stateful.postgresPassword=******\
  --set keycloak.admin.password=******* \
  --set account-service.keycloak.clientSecret=******** \
  --set transfer-service.keycloak.clientSecret=******** \
  --set cash-service.keycloak.clientSecret=******* \
  --set bank-ui.keycloak.clientSecret=*********
  ```
  ![dry-run](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/dry-run1.png)

- Далее после предварительного тестирования(валидации) можем запустить команду для сборки но без ```-dry-run --debug```
  ```
  helm upgrade --install my-bank . --namespace test --create-namespace \ (аргументы см. выше)
  ```
- Запускаем ```ingress-nginx``` контроллер для внешней маршрутизации и доступа к ```bank-ui```
  ```
  kubectl apply -f ingress-nginx.yaml
  ```

- Наши сервисы начали запускаться, после запуска можем запустить написанные тесты в папке ```tests``` каждого сервиса.

```
helm test my-bank -n test
```
![tests](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/result_tests.png)

- Далее посмотрим, что все наши сервисы запущены и тесты выполнены
  ```
  kubectl get pods -n test
  ```
![tests](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/all_serv_and_tests.png)

- Также для прокерки доступности был использован механизм проб, к примеру после запуска посмотрим описание пода ```account-service```
  ```
  kubectl describe pod -n test my-bank-account-service-646d59649d-29j7c
  ```
kubectl describe pod -n test my-bank-account-service-646d59649d-29j7c
  ![tests](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/descdribe_probes.png)

- После того как все сервисы поднялись и ингрис контроллер запущен, наше приложение будет доступно по внешнему адресу
  ![tests](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/new_url.png)

### Тесты приложения
Очистим предыдущие сборки
```
./gradlew clean
```

Опубликуем стабы(для конрактных тестов)
```
./gradlew publishToMavenLocal
```

Сборка и выполнение тестов(в том числе и контрактных)
```
./gradlew build
```
Или одной командой

```
./gradlew clean publishToMavenLocal build
```



