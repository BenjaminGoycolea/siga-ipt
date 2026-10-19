package cl.ipt.siga.dto;

import cl.ipt.siga.entity.enums.CareerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CareerStatusRequest", description = "Estado que se asignará a la carrera.")
public record CareerStatusRequest(
        @Schema(
                description = "ACTIVE permite nuevos estudiantes; INACTIVE impide nuevas matrículas.",
                example = "ACTIVE"
        )
        @NotNull CareerStatus status
) {
}
