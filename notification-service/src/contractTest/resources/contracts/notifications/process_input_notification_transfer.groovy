package contracts.notifications

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description '''Process TRANSFER_PERFORMED notification from transfer-service:
        eventId=3504103f-750d-4622-b6e9-dd9136a23b43,
        source=transfer-service,
        eventType=TRANSFER_PERFORMED,
        message=alexeev выполнил перевод petrov на сумму 200.00, новый баланс отправителя 800.00, новый баланс получателя 1200.00,
        payload={"transactionId":"3504103f-750d-4622-b6e9-dd9136a23b43","fromAccount":"alexeev","toAccount":"petrov","amount":500.00,"newBalanceSender":1500.00,"newBalanceReceiver":2500.00}
        and return notification response:
        notificationId=3504103f-750d-4622-b6e9-dd9136a23b43,
        status=PROCESSED,
        processedAt=2026-03-01T00:47:24.775009
    '''
    name 'process_input_notification_transfer'

    request {
        method POST()
        url '/api/v1/notifications'
        headers {
            header 'Authorization', 'Bearer test-token'
            contentType(applicationJson())
        }
        body(
                eventId: '3504103f-750d-4622-b6e9-dd9136a23b43',
                source: 'transfer-service',
                eventType: 'TRANSFER_PERFORMED',
                message: 'alexeev выполнил перевод petrov на сумму 200.00, новый баланс отправителя 800.00, новый баланс получателя 1200.00',
                payload: [
                        transactionId: '3504103f-750d-4622-b6e9-dd9136a23b43',
                        fromAccount: 'alexeev',
                        toAccount: 'sergeev',
                        amount: 200.00,
                        newBalanceSender: 800.00,
                        newBalanceReceiver: 1200.00
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

