package br.com.bytebank.customers.api.openapi.controller;

import br.com.bytebank.customers.api.dtos.requests.CustomerRequestDTO;
import br.com.bytebank.customers.api.dtos.requests.CustomerUpdateDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.CustomerShortResponseDTO;
import br.com.bytebank.customers.api.dtos.responses.PagedResponse;
import br.com.bytebank.customers.domain.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "MS - Customers")
public interface CustomerControllerOpenApi {

    @Operation(summary = "Create Customer", description = "Creates a new customer into database")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CustomerResponseDTO> save(
            @Parameter(
                    description = "Unique key to ensure idempotency of the request",
                    required = true
            )  UUID idempotencyKey,
            @RequestBody(description = "Attributes required to Create a customer", required = true) CustomerRequestDTO customerRequestDTO);

    @Operation(summary = "Returns a list from all customers")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns a list or an empty list",
                    content = @Content(schema = @Schema(implementation = CustomerShortResponseDTO.class))
            )
    })
    PagedResponse<CustomerShortResponseDTO> getCostumers(@Parameter(description = "Parameter corresponding with the number of the page with defaultValue = 0") int page,
                                                         @Parameter(description = "Inform how many elements the page may have" ) int size);

    @Operation(description = "Update a customer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found and successfully updated",
                    content = @Content(schema = @Schema(implementation = CustomerUpdateDTO.class))
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CustomerUpdateDTO> updateCustomer(@Parameter (description = "Path variable that inform customer id", required = true) UUID id,
                                                     @RequestBody (description = "DTO to inform to update customer", required = true) CustomerUpdateDTO dto);

    @Operation(summary = "Find customer by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found successfully",
                    content = @Content(schema = @Schema(implementation = CustomerShortResponseDTO.class))
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CustomerShortResponseDTO> findCustomerById(@Parameter(description = "PathVariable to inform and find customer", required = true) UUID id);
}
