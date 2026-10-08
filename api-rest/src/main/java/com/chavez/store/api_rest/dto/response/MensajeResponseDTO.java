package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MensajeResponseDTO {

    private String mensaje;
    private Boolean exito;

    public static MensajeResponseDTO ok(String mensaje) {
        return MensajeResponseDTO.builder().mensaje(mensaje).exito(true).build();
    }
}