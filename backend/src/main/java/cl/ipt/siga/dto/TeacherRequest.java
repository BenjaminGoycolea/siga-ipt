package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TeacherRequest", description = "Datos para registrar o actualizar un docente.")
public record TeacherRequest(
        @Schema(description = "RUN único del docente.", example = "12.345.678-9")
        @NotBlank @Size(max = 20) String nationalId,
        @Schema(description = "Nombre completo del docente.", example = "Camila González")
        @NotBlank @Size(max = 150) String fullName,
        @Schema(description = "Correo electrónico válido y único.", example = "camila.gonzalez@example.edu")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Título profesional.", example = "Ingeniera en Informática")
        @NotBlank @Size(max = 150) String professionalTitle,
        @Schema(description = "Grado académico.", example = "Magíster en Ciencias de la Computación")
        @NotBlank @Size(max = 150) String academicDegree
) {
}
