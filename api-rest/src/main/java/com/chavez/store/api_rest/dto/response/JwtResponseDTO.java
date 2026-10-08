package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
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
public class JwtResponseDTO {

    private String token;

    /** Segundos hasta la expiracion, para que el frontend programe la renovacion. */
    private Long expiresIn;

    private String username;

    private String fullName;

    private List<String> roles;
}