package cl.ipt.siga.dto;

import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AcademicPeriodStatusRequest", description = "Estado que se asignará al período.")
public record AcademicPeriodStatusRequest(
        @Schema(description = "Estado OPEN o CLOSED.", example = "OPEN")
        @NotNull AcademicPeriodStatus status
) {
}
