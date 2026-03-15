package contracts.accounts

import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description "Should decrease balance for a given owner"
    name "decrease_balance_by_username"
    request {
        method 'PATCH'
        url '/api/v1/accounts/alexeev/balance/decrease'
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
        body(800.00)
        headers {
            contentType(applicationJson())
        }
    }
}

