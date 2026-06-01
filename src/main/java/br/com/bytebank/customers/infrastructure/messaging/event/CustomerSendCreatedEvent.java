package br.com.bytebank.customers.infrastructure.messaging.event;

import java.util.UUID;

public record CustomerSendCreatedEvent(
        UUID customerId,

        UUID idempotencyKey
) {
}
