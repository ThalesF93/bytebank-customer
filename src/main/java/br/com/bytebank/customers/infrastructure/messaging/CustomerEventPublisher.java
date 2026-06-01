
package br.com.bytebank.customers.infrastructure.messaging;

import br.com.bytebank.customers.infrastructure.config.RabbitMQConfig;
import br.com.bytebank.customers.infrastructure.messaging.event.CustomerCreatedEvent;
import br.com.bytebank.customers.infrastructure.messaging.event.CustomerSendCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishCustomerCreated(CustomerSendCreatedEvent customerEvent) {
        var event = new CustomerCreatedEvent(customerEvent.customerId(), customerEvent.idempotencyKey());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_CUSTOMER,
                RabbitMQConfig.ROUTING_KEY_CUSTOMER_CREATED,
                event
        );
        log.info("Event published: CustomerCreatedEvent customerId={}", customerEvent.customerId());
    }
}