package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.TipoCliente;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClienteResponseDTO {

    private Long id;
    private String document;
    private TipoCliente type;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private BigDecimal creditLimit;
    private BigDecimal saldoActual;
    private BigDecimal disponible;
    private Boolean active;
}