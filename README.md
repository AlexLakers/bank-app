# bank-app

Это микросервисный проект банковского приложения с аутентификацией и авторизацией через Keycloak. Для взаимодействия микросервисов используется message
broker 'Kafka' и REST. Для депоймента используется Kubernetes, Helm и Docker. Для мониторинга используется Zipkin, Prometheus-stack и ELK-stack.

- Проект состоит из 5 микросервисов:
- bank-ui(frontend)
- cash-service(backend сервиса наличных,kafka-producer)
- account-service(backend сервиса аккаунтов пользователей,kafka-producer)
- transfer-service(backend сервиса переводов между счетами,kafka-producer)
- notification-service(backend сервиса уведомлений,kafka-consumer).

![Схема микросервисного проекта](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/schema_with_kafka.png)

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
- В проект встроена система уведомлений с использованием планировщика(Spring Scheduler, Outbox Transactions) и брокера
  сообщений 'Kafka'.
  Этот сервис выступает как 'producer' и отправляет в топик 'account-events' события.

### Безопасность

Система безопасности включает в себя:

- Для авторизации используется OAUTH2.0 и 'client credentails flow' без вмешивания пользвоателя в процесс(
  service-service).
- Access token - содержит информацию определенную в 'scope', в том числе и роли.
- Роли на текущий момент: 'SERVICE'-роль сервиса(не пользователя).
- Токен может передается как bearer-токен с запросом при взаимодействии с другими сервисами напрямую без 'api-gateway',
  в настоящее время этот сервис взаимодействует только с брокером сообщений.

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
- В проект встроена система уведомлений с использованием планировщика(Spring Scheduler, Outbox Transactions) и брокера
  сообщений 'Kafka'.
  Этот сервис выступает как 'producer' и отправляет в топик 'cash-events' события.

### Безопасность

Система безопасности включает в себя:

- Раздел безопасности тут аналогичен 'account-service', отличаются роли и привелегии.
- Роли: 'SERVICE'-роль сервиса(не пользователя) и разрешения 'ACCOUNT-WRITE'.
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
- В проект встроена система уведомлений с использованием планировщика(Spring Scheduler, Outbox Transactions) и брокера
  сообщений 'Kafka'.
  Этот сервис выступает как 'producer' и отправляет в топик 'transfer-events' события.

### Безопасность

Система безопасности включает в себя:

- Раздел безопасности тут аналогичен 'account-service','cash-service' отличаются роли и привелегии.
- Роли: 'SERVICE'-роль сервиса(не пользователя) и разрешения 'ACCOUNT-WRITE'.
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
- Данный сервис использует событийно-ориентированную систему взаимодействия, как 'consumer' он читает сообщения из разных топиков 'account-events','cash-events','transfer-events'.
- В сервисе используется проверка входных данных по ключу идемпотентности, обработка ошибок с ипользованием Spring Kafka.
- События обновления аккаунта, успешного снятия или списания или перевода.
- Сейчас сервис просто пишет в лог, но можно расширить функционал.
- Кроме лога успещные события(их идентификаторы) попадают в таблицу 'events_idempotence'(event_id,processed_at)

![сервис уведомлений](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/events_idempotence.png)

- Также присутствует таблица , для хранения сообщений с ошибками 'dead_letter_queue'

![сервис уведомлений](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/dlq_table.png)

### Пример события которые записались в лог

![сервис уведомлений](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/notifications_log.png)

Видим разные события от разных микросервисов.

## Мониторинг
Для монторинга микросервисного приложения в кластере используется комплекс решений:
-  **'Zipkin'**- для трассировки пользовательских запросов и межсервисного взаимодействия.
   Для примера, посомтрим детализацию запроса  на перевод денег, видим детализацию различных спанов в рамках одного трейса.

![Зипкин](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/transfer-zipkin.png)

- **'Filebeat'**- читает логи микросервисов прямо из /var/log/containers/*.log в кластере, обеспеспечивая буферизацию и надежность.
  А микросервисы сосредоточнены на бизнесс-логике, просто пишут логи в консоль(stdout) как обычно, но в формате JSON.Разворачивается в рамках 'ECK-operator'.

- **'Logstash'** -  принимает доги от 'Filebeat' в формате JSON с помощью плагина 'beats' на input. Далее отдает в 'output'  с помощью плагина 'elasticsearch'. Разворачивается в рамках 'ECK-operator'.

- **'Elasticsearch'** - позовляет вести удобный поиск данных(по словам к примеру) и создавать индексы для логов.Разворачивается в рамках 'ECK-operator'.

- **'Kibana'** - позовляет удобно просматривать, анализировать логи, фиьтровать и прочее. Тут главное создать 'index-pattern' , наш 'app-logs-*' описан ниже.

![КибанаИндекс](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/kibana-index.png)

А ниже можно посмотреть логи для 'cash-service' с помощью фильтра по полю 'log-file-path', а в строке поиска нипишем 'spanId' для поиска по тексту.

![Кибаналоги](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/kibana-cash-service.png)

- **'Prometheus + Alert-manager'** - для получения и хранения стандартных и бизнес-метрик из микросервисов в виде временных рядов и настройки алертов.
  К прмиеру так выглядит запрос на PQL для получения скорости изменения RPS метрики за 1м с лейблом 'account-service'.

![Пром](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/account-prom-all-req.png)

Так выглядит список некоторых алертов, которые были описаны файлами 'AlertRules' и настроены с ипользованием 'alert-manager' в 'prometheus-stack'.Сейчас они неактивны.

![ПромАлерт](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/prom-alerts-inactive.png)

А вот так , к примеру выглядит настроенный и сработанный Alert, когда бизнес-метрика ошибок операций с наличными срабатывает в рамках минуты 'Pending' т.е. еще не 'Firling'.

![ПромАлерт1](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/prom_cash_pending.png)

- **'Grafana'** - позволяет добвлять дашборды и виуализации для отображения различных метрик из разных истоников данных, мы используем 'Prometheus'.
  Можем использовать готовые дашборды(импортировать) или содавать свои. В этом проекте были созданы 2 дашборда('bank-http-metrics','bank-business-metrics').
  Также использовал дашборд 'Spring-Boot-3.x-Statisctics' для стандартных спринговых метрик.

![ГрафанаДашборды](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/dashboards.png)

Настройки дашбордов можно изменять и добавлять визуализации строки между ними для компановки.Мы добавили поля для группировки, к прмиеру поля 'username' и 'service'

А тут можно увидеть кастомный дашборд 'bank-http-metrics', видим что благодаря добавленному полю 'service' в дашборд мы можем фиьтровать.
![ГрафанаДашбордыХттп](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/bank-http-metrics-list-service.png)

Кастомный Дашборд для бизнесс-метрик можно увидеть ниже, но с фильтрацией по 'username' так как этот label передается со всеми бизнесс-метриками.
![ГрафанаДашбордыБизнес](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/bussiness-metrics.png)

А ниже  мы можем посомтреть JVM-метрики и стандартные Http-метирики с помощью дашборда 'Spring-Boot-3.x-Statisctics'.

![ГрафанаДашДжвм](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/account-jvm-stat.png)

![ГрафанаДашХттп](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/account-http-stat.png)

Ну и в заключении посомтрим на алерты в 'Grafana' которые подтянулись из 'Prometheus' компонента 'Alert manager'.

![ГрафанаАлерты](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/Grafana-alerts-inactive.png)

## api-gateway, consul, keycloak

### ⚠️ Важное примечание
Обратите внимание , 'Consul' используется только при локальном пуске, в кластере его нужно отключить , так как свойства будем брать из ConfigMap или Secrets, а взаимодействие на базе 'Service'.

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

Сборка осуществляется с помощью инфраструктуры Docker с использованием Docker compose.

- В корневой папке микросервисного проекта вы найдете файл для сборки:  ```compose.yaml```
  В нем описаны все службы(контейнеры), которые необходимы для работы приложения в целом.
- В папке каждого модуля располагаются файлы сборки образа:```bank-ui/Dockerfile```,```account-service/Dockerfile```....
- Для сборки и запуска приложения(всех сервисов) просто запустите из корня проекта следующую команду:
  ```
  docker compose up -d
  ```
- В результате поднимятся 4 базы данных Postgres, фронтенд , микросервисы аккаунтов, наличных,переводо и уведомлений.
  Также поднимется Consul(конфиг сервер и реестр сервисов)  и сервер авторизации Keycloak во внетренне сети докера.
- Далее отельно поднимем брокер сообщений Кафка:

```
  docker run -d --name kafka -p 9092:9092 apache/kafka:4.0.0
```

### ⚠️ Важное примечание

- Обратите внимание , вам надо создать файл ```.env``` с перемнными, которые описаны в ```.env.example```

### Kubernetes

Также вы можете развернуть микросервисное приложение используя ```Kubernetes``` и пакетный менеджер ```Helm``` для
упрощения описания сервисов.
В качестве кластера будет использовано стандарное решение от разработчиков Kubernetes - ```Minikube```.
Микросервисное приложение будет разворачиваться как зонтичный чарт с подчартами отельным релизом и брокер 'Кафка'
отельным релизом.

- Предварительно нужно установить ```Minikube```, ```kubectl```.
- Запускаем кластер
  ```
  minikube start
  ```
- В корне проекта откройте(перейдите) в каталог ```kafka```
- Используя пакетный менеджер 'Helm' , добавим репозиторий поднимем, создадим 'Helm-chart' для брокера сообщений 'Kafka'

 ```
helm repo add kafka-repo https://helm-charts.itboon.top/kafka
helm repo update
helm upgrade --install kafka kafka-repo/kafka -f values.yaml --namespace test --create-namespace
  ```
- В результате поднимется брокер 'Kafka' отельным релизом.

  ![lint](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/kafka_released.png)

- Для надежности конфиги для создания топиков в микросервисах продюсерах были отклбючены, создадим топики вручную.

 ```
kubectl exec -it kafka-broker-0 -n test -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic account-events --partitions 2 --replication-factor 1
kubectl exec -it kafka-broker-0 -n test -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic cash-events --partitions 2 --replication-factor 1
kubectl exec -it kafka-broker-0 -n test -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic transfer-events --partitions 2 --replication-factor 1
  ```

- Теперь приступим к микросервисам, собираем Docker-images для каждого сервиса внутри кластера Minikube. Из папки проекта.

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

![tests](https://raw.githubusercontent.com/AlexLakers/ParserJsonCsvToXml/master/WinFormsCsvJsonXml/App_Data/pictures/all_services_tests_kafka.png)

- Также для прокерки доступности был использован механизм проб, к примеру после запуска посмотрим описание
  пода ```account-service```
  ```
  kubectl describe pod -n test my-bank-account-service-646d59649d-....
  ```

kubectl describe pod -n test my-bank-account-service-646d59649d-....

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



