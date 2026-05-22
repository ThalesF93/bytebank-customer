package br.com.bytebank.customers.domain.exception.customized_exceptions;

import br.com.bytebank.customers.domain.exception.default_exception.DefaultException;
import org.springframework.http.HttpStatus;

public class DuplicateCustomerException  extends DefaultException {
    public DuplicateCustomerException(String cpf) {
        super("DUPLICATE_CUSTOMER","Customer with CPF number = " + cpf + " already exists" , HttpStatus.CONFLICT);
    }
}
