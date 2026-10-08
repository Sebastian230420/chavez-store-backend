package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.chavez.store.api_rest.entity.enums.TipoPresentacion;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentacionUpdateRequestDTO {

    @NotBlank(message = "El nombre de la presentacion es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    private String name;

    @NotNull(message = "El factor de conversion es obligatorio")
    @Min(value = 1, message = "El factor de conversion debe ser mayor a 0")
    private Integer unitsBase;

    @NotNull(message = "El tipo de presentacion es obligatorio")
    private TipoPresentacion type;

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal price;

    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    private Integer stockMin;

    private Integer sortOrder;

    private Boolean active;
}