package cl.ipt.siga.controller;

import cl.ipt.siga.dto.TeacherRequest;
import cl.ipt.siga.dto.TeacherResponse;
import cl.ipt.siga.dto.TeacherStatusRequest;
import cl.ipt.siga.dto.TeacherIdentityRequest;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.service.TeacherService;
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
@RequestMapping("/api/admin/teachers")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración de docentes", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminTeacherController {

    private final TeacherService teacherService;

    public AdminTeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    @Operation(summary = "Listar docentes", description = "Lista docentes ordenados por nombre, con filtro de estado opcional.")
    public List<TeacherResponse> findAll(
            @Parameter(description = "Filtra por estado ACTIVE o INACTIVE.", example = "ACTIVE")
            @RequestParam(required = false) TeacherStatus status
    ) {
        return teacherService.findAll(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar docente", description = "Devuelve los datos del docente indicado.")
    public TeacherResponse findById(@PathVariable Long id) {
        return teacherService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar docente",
            description = "Registra un docente con estado inicial ACTIVE. El RUN y el correo deben ser únicos."
    )
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return teacherService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar datos del docente",
            description = "Actualiza RUN, nombre, correo, título profesional y grado académico; el estado no cambia."
    )
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return teacherService.update(id, request);
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Cambiar estado del docente",
            description = """
                    Cambia el estado entre ACTIVE e INACTIVE. No se puede desactivar a un docente con secciones
                    asignadas en períodos abiertos.
                    """
    )
    public TeacherResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TeacherStatusRequest request
    ) {
        return teacherService.updateStatus(id, request.status());
    }

    @PutMapping("/{id}/keycloak-subject")
    @Operation(
            summary = "Vincular cuenta Keycloak",
            description = "Asocia el docente con el claim sub de su cuenta Keycloak, que debe ser único."
    )
    public TeacherResponse updateKeycloakSubject(
            @PathVariable Long id,
            @Valid @RequestBody TeacherIdentityRequest request
    ) {
        return teacherService.updateKeycloakSubject(id, request.keycloakSubject());
    }
}
