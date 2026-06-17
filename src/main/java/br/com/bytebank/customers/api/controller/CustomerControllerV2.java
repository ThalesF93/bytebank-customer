package br.com.bytebank.customers.api.controller;

import br.com.bytebank.customers.api.dtos.requests.CustomerRequestDTO;
import br.com.bytebank.customers.api.dtos.requests.CustomerUpdateDTO;
import br.com.bytebank.customers.api.dtos.responses.*;
import br.com.bytebank.customers.api.openapi.controller.CustomerControllerOpenApi;
import br.com.bytebank.customers.application.service.CustomerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerControllerV2 implements CustomerControllerOpenApi {

    private final CustomerService service;

    @Override
    @PostMapping
    public ResponseEntity<CustomerResponseDTO> save(
            @RequestHeader(value = "Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CustomerRequestDTO customerRequestDTO){
        log.atInfo()
                .setMessage("Request received. endpoint=POST")
                .addKeyValue("Customer" , customerRequestDTO.name())
                .log();

        var result = service.createCustomer(idempotencyKey, customerRequestDTO);
        HttpStatus status = result.isDuplicate() ? HttpStatus.OK : HttpStatus.CREATED;

        log.info("Request completed. clientId={} status={}", result.data().id(), result.data().customerStatus());
        return ResponseEntity.status(status).body(result.data());
    }

    @Override
    @GetMapping
    public PagedResponse<CustomerShortResponseDTO> getCostumers(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size){
        log.info("Obtaining costumers in database");
        var pageable = PageRequest.of(page, size);

        var costumerPage = service.getCustomers(pageable);

        return new PagedResponse<>(costumerPage);
    }

    @Override
    @PutMapping("/update/{id}")
    public ResponseEntity<CustomerUpdateDTO> updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerUpdateDTO dto){
        log.info("Updating customer from id={}", id);
        return ResponseEntity.ok(service.updateCustomer(id, dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CustomerClientResponseDTO> findCustomerById(@PathVariable UUID id){
        return ResponseEntity.ok(service.findCustomerByIdWithFeign(id));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<String> findByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(service.findCustomerByPhoneNumber(phone));
    }
}

