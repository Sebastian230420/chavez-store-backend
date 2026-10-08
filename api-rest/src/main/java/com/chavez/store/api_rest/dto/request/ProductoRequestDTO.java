package com.chavez.store.api_rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.chavez.store.api_rest.entity.enums.UnidadBase;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequestDTO {

    /** NULL o vacio: el sistema genera el SKU (regla R-C-01). */
    @Size(max = 50, message = "El SKU no puede superar 50 caracteres")
    private String sku;

    @Size(max = 50, message = "El codigo de barras no puede superar 50 caracteres")
    private String barcode;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String name;

    private String description;

    @NotNull(message = "La categoria es obligatoria")
    private Long categoriaId;

    private Long marcaId;

    @NotNull(message = "La unidad base es obligatoria")
    private UnidadBase baseUnit;

    private Integer contentMl;

    @NotNull(message = "El stock minimo es obligatorio")
    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    private Integer minStock;

    private Integer maxStock;

    /** Regla R-C-04: al menos una presentacion de tipo VENTA. */
    @NotEmpty(message = "Debe registrar al menos una presentacion")
    @Valid
    private List<PresentacionRequestDTO> presentaciones;
}