package br.com.bytebank.customers.api.dtos.responses;



import br.com.bytebank.customers.domain.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Response DTO with the most important values of Customers ")
public record CustomerShortResponseDTO(

        @Schema(description = "Customer's ID")
        UUID id,

        @Schema(description = "Customer's name")
        String name,

        @Schema(description = "Customer's email")
        String email,

        @Schema(description = "Show if the costumer is Active or Inactive")
        CustomerStatus CustomerStatus


) implements Serializable {
}
