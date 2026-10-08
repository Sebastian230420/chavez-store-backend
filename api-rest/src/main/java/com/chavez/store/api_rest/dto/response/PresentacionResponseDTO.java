package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.TipoPresentacion;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresentacionResponseDTO {

    private Long id;
    private Long productoId;
    private String name;
    private Integer unitsBase;
    private TipoPresentacion type;
    private BigDecimal price;
    private Integer stockMin;
    private Boolean active;
    private Integer sortOrder;
}