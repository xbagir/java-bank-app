import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should withdraw money from account"
    request {
        method POST()
        url "/internal/accounts/petrov/withdraw"
        headers {
            contentType(applicationJson())
        }
        body(
                requestId: "33333333-3333-3333-3333-333333333333",
                amount: 100.00
        )
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                login: "petrov",
                name: "Петров Пётр",
                birthdate: "1995-05-15",
                balance: 400.00
        )
    }
}
