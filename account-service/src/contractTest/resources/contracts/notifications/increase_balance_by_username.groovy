package contracts.notifications


import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description "Should increase balance for a given owner"
    name "increase_balance_by_username"
    request {
        method 'PATCH'
        url '/api/v1/accounts/alexeev/balance/increase'
        headers {
            header('Authorization': value(
                    consumer(regex('Bearer .*')),
                    producer('Bearer test-token')
            ))
            contentType(applicationJson())
        }
        body(
                amount: 200.00
        )
    }
    response {
        status OK()
        body("1200.00")
        headers {
            contentType(applicationJson())
        }
    }
}

