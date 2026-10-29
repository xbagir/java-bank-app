import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should accept a notification"
    request {
        method POST()
        url "/notifications"
        headers {
            contentType(applicationJson())
        }
        body(
                eventId: "11111111-1111-1111-1111-111111111111",
                recipient: "ivanov",
                type: "CASH_DEPOSIT",
                message: "Счёт пополнен на 100 руб."
        )
    }
    response {
        status ACCEPTED()
        headers {
            contentType(applicationJson())
        }
        body(
                id: $(anyNumber()),
                eventId: "11111111-1111-1111-1111-111111111111",
                recipient: "ivanov",
                type: "CASH_DEPOSIT",
                message: "Счёт пополнен на 100 руб.",
                createdAt: $(anyNonEmptyString())
        )
    }
}
