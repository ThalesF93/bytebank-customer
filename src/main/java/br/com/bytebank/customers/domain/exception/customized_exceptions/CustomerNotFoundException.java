package br.com.bytebank.customers.domain.exception.customized_exceptions;

import br.com.bytebank.customers.domain.exception.default_exception.DefaultException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CustomerNotFoundException extends DefaultException {
    public CustomerNotFoundException(UUID uuid) {
        super("CUSTOMER_NOT_FOUND", "Customer with id " + uuid + " not found", HttpStatus.NOT_FOUND);
    }

    public CustomerNotFoundException(String phone){
        super("CUSTOMER_NOT_FOUND", "Customer with phone " + phone + " not found", HttpStatus.NOT_FOUND);
    }
}
