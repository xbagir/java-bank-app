import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should transfer money between accounts"
    request {
        method POST()
        url "/internal/accounts/transfer"
        headers {
            contentType(applicationJson())
        }
        body(
                requestId: "44444444-4444-4444-4444-444444444444",
                fromLogin: "petrov",
                toLogin: "ivanov",
                amount: 100.00
        )
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                fromLogin: "petrov",
                toLogin: "ivanov",
                amount: 100.00,
                balance: 400.00
        )
    }
}
