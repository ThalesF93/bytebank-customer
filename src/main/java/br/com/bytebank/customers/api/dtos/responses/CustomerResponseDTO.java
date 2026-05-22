package br.com.bytebank.customers.api.dtos.responses;



import br.com.bytebank.customers.domain.entity.Customer;
import br.com.bytebank.customers.domain.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Response DTO with creation information")
public record  CustomerResponseDTO(

        @Schema(description = "Returns ID create")
        UUID id,

        @Schema(description = "Returns the status 'active' or 'inactive'")
        CustomerStatus customerStatus,

        @Schema(description = "Returns the user who created the customer when possible")
        String createByUser,

        @Schema(description = "Return the Date and Time of creation")
        LocalDateTime createdDate,

        @Schema(description = "Returns the user who edited the customer when possible")
        String editedByUser,

        @Schema(description = "Return the Date and Time of modification")
        String editDate,

        @Schema(description = "Returns a message")
        String message
) implements Serializable {
    public static CustomerResponseDTO accountCreated(Customer entity) {
        return new CustomerResponseDTO(
                entity.getId(),
                entity.getCustomerStatus(),
                entity.getCreateByUser(),
                entity.getCreatedDate(),
                null,
                null,
                "Registered customer and account created successfully!"
        );
    }
    public static CustomerResponseDTO accountPending(Customer entity) {
        return new CustomerResponseDTO(
                entity.getId(),
                entity.getCustomerStatus(),
                entity.getCreateByUser(),
                entity.getCreatedDate(),
                null,
                null,
                "Customer registered! Your account is being created and will be available soon."
        );}

}
