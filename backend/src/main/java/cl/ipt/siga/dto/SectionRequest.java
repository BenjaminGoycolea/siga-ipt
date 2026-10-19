package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        name = "SectionRequest",
        description = "Asignatura, docente, capacidad y bloques horarios para una sección."
)
public record SectionRequest(
        @Schema(description = "ID de una asignatura existente.", example = "1")
        @NotNull @Positive Long courseId,
        @Schema(description = "ID de un docente ACTIVE.", example = "1")
        @NotNull @Positive Long teacherId,
        @Schema(description = "Capacidad máxima de la sección, entre 1 y 60.", example = "40", minimum = "1", maximum = "60")
        @Min(1) @Max(60) int capacity,
        @Schema(
                description = """
                        Uno o más bloques únicos. Día válido L, M, W, J, V y módulo de 1 a 6; por ejemplo ["L2","W3"].
                        El número de bloques debe ser igual a las horas TEL semanales de la asignatura dividido por 2.
                        """,
                example = "[\"L2\", \"W3\"]"
        )
        @NotNull @Size(min = 1, max = 3) List<@NotNull String> scheduleBlocks
) {
}
