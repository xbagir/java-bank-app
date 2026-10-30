import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should deposit money to account"
    request {
        method POST()
        url "/internal/accounts/petrov/deposit"
        headers {
            contentType(applicationJson())
        }
        body(
                requestId: "22222222-2222-2222-2222-222222222222",
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
                balance: 600.00
        )
    }
}
