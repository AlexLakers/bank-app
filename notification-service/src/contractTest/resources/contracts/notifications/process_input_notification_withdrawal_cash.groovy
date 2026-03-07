package contracts.notifications


import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description '''Process cash withdrawal notification:
        eventId=3504103f-750d-4622-b6e9-dd9136a23b43,
        source=cash-service,
        eventType=CASH_WITHDRAWAL,
        message=Пользователь: alexeev выполнил: CASH_WITHDRAWAL на сумму: 200.00, новый баланс: 800.00,
        payload={"transactionId":"3504103f-750d-4622-b6e9-dd9136a23b43","accountHolder":"alexeev","amount":200.00,"newBalance":4000.00}
        and return notification response:
        notificationId=3504103f-750d-4622-b6e9-dd9136a23b43,
        status=PROCESSED,
        processedAt=2026-03-01T00:47:24.775009
    '''
    name 'process_input_notification_withdrawal_cash'

    request {
        method POST()
        url '/api/v1/notifications'
        headers {
            header 'Authorization', 'Bearer test-token'
            contentType(applicationJson())
        }
        body(
                eventId: '3504103f-750d-4622-b6e9-dd9136a23b43',
                source: 'cash-service',
                eventType: 'CASH_WITHDRAWAL',
                message: 'Пользователь: alexeev выполнил: CASH_WITHDRAWAL на сумму: 200.00, новый баланс: 800.00',
                payload: [
                        transactionId: '3504103f-750d-4622-b6e9-dd9136a23b43',
                        accountHolder: 'alexeev',
                        amount: 200.00,
                        newBalance: 800.00
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

