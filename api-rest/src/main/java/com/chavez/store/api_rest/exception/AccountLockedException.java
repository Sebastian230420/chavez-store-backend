package com.chavez.store.api_rest.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Cuenta bloqueada por intentos fallidos. HTTP 423 · AUTH_002 */
public class AccountLockedException extends BusinessException {

    public AccountLockedException(LocalDateTime until) {
        super("AUTH_002", 423, "Cuenta bloqueada temporalmente hasta "
                + until.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }
}