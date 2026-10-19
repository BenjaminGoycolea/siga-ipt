package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Identidad de Keycloak asociada al docente.")
public record TeacherIdentityRequest(
        @Schema(description = "Claim sub del token JWT del docente.", example = "b7f5a1c2-1234-4567-9876-123456789abc")
        @NotBlank @Size(max = 64) String keycloakSubject
) {
}
