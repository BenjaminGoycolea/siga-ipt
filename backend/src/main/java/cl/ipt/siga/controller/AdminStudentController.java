package cl.ipt.siga.controller;

import cl.ipt.siga.dto.StudentRequest;
import cl.ipt.siga.dto.StudentResponse;
import cl.ipt.siga.dto.StudentStatusRequest;
import cl.ipt.siga.dto.MatriculationRequest;
import cl.ipt.siga.dto.MatriculationResponse;
import cl.ipt.siga.dto.StudentIdentityRequest;
import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.service.StudentMatriculationService;
import cl.ipt.siga.service.AcademicHistoryService;
import cl.ipt.siga.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración de estudiantes", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminStudentController {

    private final StudentService studentService;
    private final StudentMatriculationService matriculationService;
    private final AcademicHistoryService academicHistoryService;

    public AdminStudentController(
            StudentService studentService,
            StudentMatriculationService matriculationService,
            AcademicHistoryService academicHistoryService
    ) {
        this.studentService = studentService;
        this.matriculationService = matriculationService;
        this.academicHistoryService = academicHistoryService;
    }

    @GetMapping
    @Operation(
            summary = "Listar estudiantes",
            description = "Devuelve todos los estudiantes. Se puede filtrar opcionalmente por el ID de una carrera."
    )
    public List<StudentResponse> findAll(
            @Parameter(description = "Filtra los estudiantes por carrera.", example = "1")
            @RequestParam(required = false) Long careerId
    ) {
        return studentService.findAll(careerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar estudiante", description = "Devuelve los datos del estudiante indicado.")
    public StudentResponse findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar estudiante",
            description = """
                    Registra un estudiante en una carrera activa y lo asocia automáticamente a su plan vigente.
                    El estado inicial es REGULAR. RUN y correo deben ser únicos.
                    """
    )
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar datos del estudiante",
            description = """
                    Actualiza RUN, nombre, correo y carrera del estudiante. La carrera debe ser la misma que ya tiene
                    asociada; el cambio de carrera no está disponible mientras no se implemente el historial académico.
                    """
    )
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Actualizar estado del estudiante",
            description = """
                    Cambia el estado administrativo del estudiante. GRADUATED y DISMISSED se asignan automáticamente
                    durante el cierre académico y no se aceptan en esta operación.
                    """
    )
    public StudentResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StudentStatusRequest request
    ) {
        return studentService.updateStatus(id, request.status());
    }

    @PutMapping("/{id}/keycloak-subject")
    @Operation(
            summary = "Vincular cuenta Keycloak",
            description = """
                    Asocia el estudiante con el claim sub de su cuenta Keycloak. El valor debe obtenerse del token
                    JWT del propio estudiante y no puede estar asociado a otra persona.
                    """
    )
    public StudentResponse updateKeycloakSubject(
            @PathVariable Long id,
            @Valid @RequestBody StudentIdentityRequest request
    ) {
        return studentService.updateKeycloakSubject(id, request.keycloakSubject());
    }

    @PutMapping("/{studentId}/periods/{periodId}/matriculation")
    @Operation(
            summary = "Registrar matrícula administrativa del período",
            description = """
                    Registra si el estudiante completó la matrícula institucional externa a SIGA IPT.
                    Solo un estudiante REGULAR puede quedar marcado como matriculado.
                    """
    )
    public MatriculationResponse updateMatriculation(
            @PathVariable Long studentId,
            @PathVariable Long periodId,
            @Valid @RequestBody MatriculationRequest request
    ) {
        return matriculationService.update(studentId, periodId, request.matriculated());
    }

    @GetMapping("/{id}/academic-history")
    @Operation(
            summary = "Consultar historial académico",
            description = "Lista las asignaturas, períodos, calificaciones y resultados de períodos cerrados del estudiante."
    )
    public List<CourseEnrollmentResponse> findAcademicHistory(@PathVariable Long id) {
        return academicHistoryService.findStudentHistory(id);
    }
}
