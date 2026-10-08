package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProveedorResponseDTO {

    private Long id;
    private String document;
    private String name;
    private String phone;
    private String email;
    private String address;
    private Boolean active;
    private Long totalCompras;
    private BigDecimal totalComprado;
    private LocalDate ultimaCompra;
}