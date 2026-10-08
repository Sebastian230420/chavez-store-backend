package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoResponseDTO {

    private Long id;
    private String sku;
    private String barcode;
    private String name;
    private String description;
    private Long categoriaId;
    private String categoriaNombre;
    private Long marcaId;
    private String marcaNombre;
    private String baseUnit;
    private Integer contentMl;
    private Integer stock;
    private Integer minStock;
    private Integer maxStock;
    private BigDecimal costAvg;
    private Boolean active;
    private List<PresentacionResponseDTO> presentaciones;

    /** (precio - costo) / precio, en porcentaje. Margen teorico. */
    private BigDecimal margenTeoricoPct;

    /** Dia en que el producto mas proximo vence, si tiene lotes. */
    private LocalDate primerVencimiento;
    private Integer diasParaVencer;
}