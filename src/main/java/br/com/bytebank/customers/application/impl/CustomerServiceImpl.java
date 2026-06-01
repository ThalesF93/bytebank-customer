package br.com.bytebank.customers.application.impl;

import br.com.bytebank.customers.api.dtos.requests.CustomerRequestDTO;
import br.com.bytebank.customers.api.dtos.requests.CustomerUpdateDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerShortResponseDTO;
import br.com.bytebank.customers.application.service.CustomerService;
import br.com.bytebank.customers.domain.entity.Customer;
import br.com.bytebank.customers.domain.enums.AccountStatus;
import br.com.bytebank.customers.domain.enums.CustomerStatus;
import br.com.bytebank.customers.domain.exception.customized_exceptions.CustomerNotFoundException;
import br.com.bytebank.customers.domain.exception.customized_exceptions.DuplicateCustomerException;
import br.com.bytebank.customers.domain.exception.customized_exceptions.IdempotencyCacheException;
import br.com.bytebank.customers.infrastructure.messaging.CustomerEventPublisher;
import br.com.bytebank.customers.infrastructure.messaging.event.CustomerSendCreatedEvent;
import br.com.bytebank.customers.infrastructure.repositories.CustomerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import static br.com.bytebank.customers.domain.exception.customized_exceptions.IdempotencyCacheException.Operation.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    @CacheEvict(value = {"account-status", "customers-by-id"}, allEntries = true)
    public ServiceResult<CustomerResponseDTO> createCustomer(UUID idempotencyKey, CustomerRequestDTO customerRequestDTO) {
        checkDuplicateCPF(customerRequestDTO);

        String cacheKey = "idempotency:customer:" + idempotencyKey;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Duplicate customer detected. idempotencyKey={}", idempotencyKey);
            return new ServiceResult<>(fromIdempotencyCache(cached, CustomerResponseDTO.class), true);
        }

        var customerEntity = toEntity(customerRequestDTO);
        customerEntity.setAccountStatus(AccountStatus.PENDING);
        repository.save(customerEntity);

        var event = new CustomerSendCreatedEvent(customerEntity.getId(), idempotencyKey);
        eventPublisher.publishCustomerCreated(event);

        log.info("Registered customer. customerId={}", customerEntity.getId());

        var response = CustomerResponseDTO.accountPending(customerEntity);

        toIdempotencyCache(cacheKey, response);

        return new ServiceResult<>(response, false);

    }

    @Override
    public Page<CustomerShortResponseDTO> getCustomers(Pageable pageable){
        return repository.findAll(pageable)
                .map(convertToCustomerResumeResponseDTO());
    }

    @Override
    @CacheEvict(value = "customers-by-id", allEntries = true)
    public CustomerUpdateDTO updateCustomer(UUID uuid, CustomerUpdateDTO customerUpdateDTO) {
        var customer = repository.findById(uuid).orElseThrow(
                ()-> new CustomerNotFoundException(uuid)
        );

        customer.setName(customerUpdateDTO.name());
        customer.setAddress(customerUpdateDTO.address());
        customer.setEmail(customerUpdateDTO.email());
        repository.save(customer);

        return new CustomerUpdateDTO(customerUpdateDTO.name(), customerUpdateDTO.email(), customerUpdateDTO.address());

    }

    @Override
    @Cacheable(value = "customers-by-id", key = "#id")
    public CustomerShortResponseDTO findCustomerById(UUID id) {
        var customer = repository.findById(id).orElseThrow(
                ()-> new CustomerNotFoundException(id)
        );
        return new CustomerShortResponseDTO(customer.getId(), customer.getName(), customer.getEmail(), customer.getCustomerStatus());
    }

    private static Function<Customer, CustomerShortResponseDTO> convertToCustomerResumeResponseDTO() {
        return customer -> new CustomerShortResponseDTO(
                customer.getId(), customer.getName(), customer.getEmail(), customer.getCustomerStatus()
        );
    }

    private void checkDuplicateCPF(CustomerRequestDTO dto){
        final var cpf = dto.cpf();

        if (repository.existsByCpf(cpf)){
            throw new DuplicateCustomerException(cpf);
        }
    }

    private static Customer toEntity(CustomerRequestDTO customerRequestDTO) {
        var customerEntity = new Customer();
        customerEntity.setName(customerRequestDTO.name());
        customerEntity.setCpf(customerRequestDTO.cpf());
        customerEntity.setAge(customerRequestDTO.age());
        customerEntity.setEmail(customerRequestDTO.email());
        customerEntity.setAddress(customerRequestDTO.address());



        customerEntity.setCustomerStatus(CustomerStatus.ACTIVE);
        return customerEntity;
    }

    private void toIdempotencyCache(String cacheKey, Object value) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response. type={}", value.getClass().getSimpleName(), e);
            throw new IdempotencyCacheException(SERIALIZE);
        }
    }

    private <T> T fromIdempotencyCache(Object value, Class<T> clazz) {
        try {
            return objectMapper.readValue(value.toString(), clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize idempotency response. type={}", clazz.getSimpleName(), e);
            throw new IdempotencyCacheException(DESERIALIZE);
        }
    }

    public record ServiceResult<T>(T data, boolean isDuplicate){}
}
