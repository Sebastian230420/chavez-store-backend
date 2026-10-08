package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.MetodoPago;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbonoDelDiaDTO {

    private Long abonoId;
    private String cliente;
    private BigDecimal amount;
    private MetodoPago method;
}