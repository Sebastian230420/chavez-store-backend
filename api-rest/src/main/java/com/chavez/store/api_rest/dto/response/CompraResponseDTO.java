package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.EstadoCompra;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompraResponseDTO {

    private Long id;
    private String document;
    private Long proveedorId;
    private String proveedorNombre;
    private LocalDate issueDate;
    private EstadoCompra status;
    private BigDecimal total;
    private String notes;
    private List<DetalleCompraResponseDTO> detalles;
}