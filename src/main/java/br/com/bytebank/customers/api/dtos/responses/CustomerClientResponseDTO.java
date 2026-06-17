package br.com.bytebank.customers.api.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Customer data response")
public record CustomerClientResponseDTO(

        @Schema(description = "Customer ID")
        UUID id,

        @Schema(description = "Customer's name")
        String name,

        @Schema(description = "Customer's email")
        String email
) implements Serializable {
}
