package br.com.bytebank.customers.api.openapi.controller;

import br.com.bytebank.customers.api.dtos.requests.CustomerRequestDTO;
import br.com.bytebank.customers.api.dtos.requests.CustomerUpdateDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerShortResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

public interface CustomerControllerOpenApi {

    ResponseEntity<CustomerResponseDTO> save(@Valid @RequestBody CustomerRequestDTO customerRequestDTO);

    PagedResponse<CustomerShortResponseDTO> getCostumers(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size);

    ResponseEntity<CustomerUpdateDTO> updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerUpdateDTO dto);

    ResponseEntity<CustomerShortResponseDTO> findCustomerById(@PathVariable UUID id);
}
