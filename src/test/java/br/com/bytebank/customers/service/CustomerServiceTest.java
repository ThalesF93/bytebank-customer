package br.com.bytebank.customers.service;

import br.com.bytebank.customers.api.dtos.requests.CustomerRequestDTO;
import br.com.bytebank.customers.api.dtos.requests.CustomerUpdateDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerShortResponseDTO;
import br.com.bytebank.customers.application.impl.CustomerServiceImpl;
import br.com.bytebank.customers.domain.entity.Customer;
import br.com.bytebank.customers.domain.exception.customized_exceptions.CustomerNotFoundException;
import br.com.bytebank.customers.domain.exception.customized_exceptions.DuplicateCustomerException;
import br.com.bytebank.customers.infrastructure.messaging.CustomerEventPublisher;
import br.com.bytebank.customers.infrastructure.repositories.CustomerRepository;
import br.com.bytebank.customers.tests_builders.CustomerTestsBuilders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	@InjectMocks
	CustomerServiceImpl customerService;

	@Mock
	CustomerRepository customerRepository;

	@Mock
	CustomerEventPublisher eventPublisher;

	@Mock
	RedisTemplate<String, Object> redisTemplate;

	@Mock
	ValueOperations<String, Object> valueOperations;

	@Mock
	ObjectMapper objectMapper;


	@Test
	@DisplayName("Should create a customer successfully")
	void mustCreateCustomer() {
		UUID idempotencyKey = UUID.randomUUID();
		CustomerRequestDTO dto = CustomerTestsBuilders.customerRequestDTOBuilder();
		var entity = CustomerTestsBuilders.createEntityWithDTO(dto);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(null);
		when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);

		var result = customerService.createCustomer(idempotencyKey, dto);

		verify(customerRepository).save(entity);
		verify(eventPublisher).publishCustomerCreated(idempotencyKey ,entity.getId());

		assertThat(dto.name()).isEqualTo(entity.getName());
	}

	@Test
	@DisplayName("Should return cached response when idempotency key already exists")
	void mustReturnCachedResponseWhenDuplicateRequest() throws JsonProcessingException {
		UUID idempotencyKey = UUID.randomUUID();
		CustomerRequestDTO dto = CustomerTestsBuilders.customerRequestDTOBuilder();
		CustomerResponseDTO cachedResponse = CustomerTestsBuilders.customerResponseDTOBuilder();
		String cachedJson = "AnyString";

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(cachedJson);
		when(objectMapper.readValue(anyString(), eq(CustomerResponseDTO.class))).thenReturn(cachedResponse);

		var result = customerService.createCustomer(idempotencyKey, dto);

		assertThat(result).isEqualTo(cachedResponse);
		verify(customerRepository, never()).save(any(Customer.class));
		verify(eventPublisher, never()).publishCustomerCreated(any() ,any());
	}

	@Test
	@DisplayName("Should throw Exception when passing a duplicated cpf")
	void mustThrowException() {
		UUID idempotencyKey = UUID.randomUUID();
		CustomerRequestDTO dto = CustomerTestsBuilders.customerRequestDTOBuilder();

		when(customerRepository.existsByCpf(dto.cpf())).thenReturn(true);

		assertThatExceptionOfType(DuplicateCustomerException.class)
				.isThrownBy(() -> customerService.createCustomer(idempotencyKey, dto))
				.withMessage("Customer with CPF number = " + dto.cpf() + " already exists");

		verify(customerRepository, never()).save(any(Customer.class));
	}
	@Test
	@DisplayName("Should return a page of CustomerShortResponseDTO")
	void mustReturnPageOfCustomers() {
		Customer customer = new Customer();
		customer.setId(UUID.randomUUID());
		customer.setName("João Silva");

		Pageable pageable = PageRequest.of(0, 10);
		Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);

		when(customerRepository.findAll(pageable)).thenReturn(page);

		Page<CustomerShortResponseDTO> result = customerService.getCustomers(pageable);

		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).name()).isEqualTo("João Silva");
	}

	@Test
	@DisplayName("Should return empty page when no customers exist")
	void mustReturnEmptyPage() {
		Pageable pageable = PageRequest.of(0, 10);

		when(customerRepository.findAll(pageable)).thenReturn(Page.empty());

		Page<CustomerShortResponseDTO> result = customerService.getCustomers(pageable);

		assertThat(result).isEmpty();
		verify(customerRepository).findAll(pageable);
	}

	@Test
	@DisplayName("Should uptade")
	void mustUpdateCustomer(){
		CustomerUpdateDTO dto = CustomerTestsBuilders.customerUpdateDTOBuilder();
		UUID id = UUID.randomUUID();
		Customer customer = CustomerTestsBuilders.createEntity();

		when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

		var result = customerService.updateCustomer(id, dto);

		verify(customerRepository, times(1)).save(customer);

		assertThat(result.name()).isEqualTo(customer.getName());
	}

	@Test
	@DisplayName("Should throw Customer Not found Exception")
	void mustThrowExceptionOnFindingCustomer(){
		CustomerUpdateDTO dto = CustomerTestsBuilders.customerUpdateDTOBuilder();
		UUID id = UUID.randomUUID();

		when(customerRepository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(CustomerNotFoundException.class)
				.isThrownBy(() -> customerService.updateCustomer(id, dto))
				.withMessage("Customer with id " +id + " not found");

		verify(customerRepository, never()).save(any());

	}

	@Test
	@DisplayName("Should return customer client response by id")
	void shouldReturnCustomerById() {
		UUID id = UUID.randomUUID();

		Customer customer = CustomerTestsBuilders.createEntity();
		customer.setId(id);

		when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

		CustomerShortResponseDTO result = customerService.findCustomerById(id);

		assertThat(result.id()).isEqualTo(id);
		assertThat(result.name()).isEqualTo(customer.getName());
		assertThat(result.email()).isEqualTo(customer.getEmail());

		verify(customerRepository).findById(id);
	}
	@Test
	@DisplayName("Should throw CustomerNotFoundException when customer does not exist")
	void shouldThrowWhenCustomerNotFound() {
		UUID id = UUID.randomUUID();

		when(customerRepository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(CustomerNotFoundException.class)
				.isThrownBy(() -> customerService.findCustomerById(id))
				.withMessage("Customer with id " + id + " not found");

		verify(customerRepository).findById(id);
	}

}
