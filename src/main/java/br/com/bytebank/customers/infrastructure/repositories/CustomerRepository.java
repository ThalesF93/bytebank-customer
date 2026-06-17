package br.com.bytebank.customers.infrastructure.repositories;

import br.com.bytebank.customers.api.dtos.responses.CustomerShortResponseDTO;
import br.com.bytebank.customers.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {


    boolean existsByCpf(String cpf);

    UUID id(UUID id);

    Optional<CustomerShortResponseDTO> findCustomerByPhoneNumber(String phone);
}
