package br.com.bytebank.customers.application.service;

import br.com.bytebank.customers.api.dtos.requests.CustomerRequestDTO;
import br.com.bytebank.customers.api.dtos.requests.CustomerUpdateDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerClientResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerShortResponseDTO;
import br.com.bytebank.customers.application.impl.CustomerServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerServiceImpl.ServiceResult<CustomerResponseDTO> createCustomer(UUID idempotencyKey, CustomerRequestDTO customerRequestDTO);

    Page<CustomerShortResponseDTO> getCustomers(Pageable pageable);

    String findCustomerByPhoneNumber(String phone);

    CustomerUpdateDTO updateCustomer(UUID id, CustomerUpdateDTO customerRequestDTO);

    CustomerShortResponseDTO findCustomerById(UUID id);

    CustomerClientResponseDTO findCustomerByIdWithFeign(UUID id);

}
