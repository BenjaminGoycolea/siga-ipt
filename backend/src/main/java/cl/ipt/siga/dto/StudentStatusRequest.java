package cl.ipt.siga.dto;

import cl.ipt.siga.entity.enums.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StudentStatusRequest", description = "Estado administrativo que se asignará al estudiante.")
public record StudentStatusRequest(
        @Schema(
                description = "No se aceptan GRADUATED ni DISMISSED; esos estados los define el cierre académico.",
                example = "POSTPONED"
        )
        @NotNull StudentStatus status
) {
}
