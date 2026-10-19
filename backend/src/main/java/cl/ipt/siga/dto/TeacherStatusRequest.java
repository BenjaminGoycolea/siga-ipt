package cl.ipt.siga.dto;

import cl.ipt.siga.entity.enums.TeacherStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "TeacherStatusRequest", description = "Estado que se asignará al docente.")
public record TeacherStatusRequest(
        @Schema(description = "Estado ACTIVE o INACTIVE.", example = "ACTIVE")
        @NotNull TeacherStatus status
) {
}
