package contracts.notifications


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description '''Process input notification request:
        eventId=3504103f-750d-4622-b6e9-dd9136a23b43,
        source=account-service,
        eventType=ACCOUNT_UPDATED,
        message=Данные пользователя alexeev были обновлены,
        payload={"username":"alexeev","name":"Alexeev Alexey","balance":10000.00,"birthdate":"1993-01-21"}
        and return notification response:
        notificationId=3504103f-750d-4622-b6e9-dd9136a23b43,
        status=PROCESSED,
        processedAt=2026-03-01T00:47:24.775009
    '''
    name 'process_input_notification_account'

    request {
        method POST()
        url '/api/v1/notifications'
        headers {
            header 'Authorization', value(
                    consumer(regex('Bearer\\s+.+')),
                    producer('Bearer test-token')
            )
            contentType(applicationJson())
        }
        body(
                eventId: '3504103f-750d-4622-b6e9-dd9136a23b43',
                source: 'account-service',
                eventType: 'ACCOUNT_UPDATED',
                message: 'Данные пользователя alexeev были обновлены',
                payload: [
                        username: 'alexeev',
                        name: 'Alexeev Alexey',
                        balance: 10000.00,
                        birthdate: '1993-01-21'
                ]
        )
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                notificationId: '3504103f-750d-4622-b6e9-dd9136a23b43',
                status: 'PROCESSED',
                processedAt: '2026-03-01T00:47:24.775009'
        )
    }
}

