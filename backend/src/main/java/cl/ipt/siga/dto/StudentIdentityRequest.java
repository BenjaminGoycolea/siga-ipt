package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Identificador `sub` del usuario de Keycloak asociado al estudiante.")
public record StudentIdentityRequest(
        @Schema(description = "Claim sub del token JWT del estudiante.", example = "7f8e9d10-1234-5678-abcd-0123456789ab")
        @NotBlank @Size(max = 64) String keycloakSubject
) {
}
