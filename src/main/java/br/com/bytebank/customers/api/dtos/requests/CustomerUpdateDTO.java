package br.com.bytebank.customers.api.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO from Updated customer")
public record CustomerUpdateDTO(

        @Schema(description = "Name updated")
        String name,

        @Schema(description = "Email updated")
        String email,

        @Schema(description = "Phone updated")
        String phoneNumber,

        @Schema(description = "Address updated")
        String address
) {
}
