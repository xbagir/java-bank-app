import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return account by login"
    request {
        method GET()
        url "/internal/accounts/petrov"
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
                balance: 500.00
        )
    }
}
