package cl.ipt.siga.controller;

import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.dto.EnrollmentSelectionRequest;
import cl.ipt.siga.dto.SectionResponse;
import cl.ipt.siga.service.StudentEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/periods/{periodId}")
@PreAuthorize("hasRole('STUDENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inscripción académica", description = "Oferta e inscripción del estudiante autenticado.")
public class StudentAcademicController {

    private final StudentEnrollmentService enrollmentService;

    public StudentAcademicController(StudentEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/offerings")
    @Operation(
            summary = "Consultar oferta disponible",
            description = """
                    Lista las secciones del plan del estudiante cuyos prerrequisitos ya aprobó y cuyas asignaturas
                    aún no ha aprobado. La inscripción vuelve a validar todas las reglas de negocio.
                    """
    )
    public List<SectionResponse> findOfferings(
            @Parameter(description = "ID del período académico.", example = "1")
            @PathVariable Long periodId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return enrollmentService.findOfferings(jwt.getSubject(), periodId);
    }

    @GetMapping("/enrollments")
    @Operation(
            summary = "Consultar inscripciones del período",
            description = "Lista las inscripciones y anulaciones del estudiante autenticado en el período."
    )
    public List<CourseEnrollmentResponse> findEnrollments(
            @PathVariable Long periodId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return enrollmentService.findEnrollments(jwt.getSubject(), periodId);
    }

    @PutMapping("/enrollments")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Guardar selección de asignaturas",
            description = """
                    Reemplaza la selección completa de secciones del estudiante autenticado. Las inscripciones que
                    ya no estén en la selección se anulan. Requiere estado REGULAR, matrícula del período y período
                    OPEN; valida plan, prerrequisitos, aprobaciones, carga, semestre, cupos y horarios.
                    """
    )
    public List<CourseEnrollmentResponse> replaceEnrollments(
            @PathVariable Long periodId,
            @Valid @RequestBody EnrollmentSelectionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return enrollmentService.replaceEnrollments(jwt.getSubject(), periodId, request);
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    @Operation(
            summary = "Anular una inscripción",
            description = "Anula una inscripción propia mientras el período esté OPEN y no tenga nota final registrada."
    )
    public CourseEnrollmentResponse cancelEnrollment(
            @PathVariable Long periodId,
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return enrollmentService.cancelEnrollment(jwt.getSubject(), periodId, enrollmentId);
    }
}
