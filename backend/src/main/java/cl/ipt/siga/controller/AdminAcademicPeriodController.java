package cl.ipt.siga.controller;

import cl.ipt.siga.dto.AcademicPeriodRequest;
import cl.ipt.siga.dto.AcademicPeriodResponse;
import cl.ipt.siga.dto.AcademicPeriodStatusRequest;
import cl.ipt.siga.service.AcademicPeriodService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/periods")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración de períodos", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminAcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    public AdminAcademicPeriodController(AcademicPeriodService academicPeriodService) {
        this.academicPeriodService = academicPeriodService;
    }

    @GetMapping
    @Operation(summary = "Listar períodos", description = "Lista los períodos académicos del más reciente al más antiguo.")
    public List<AcademicPeriodResponse> findAll() {
        return academicPeriodService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar período", description = "Devuelve los datos del período académico indicado.")
    public AcademicPeriodResponse findById(@PathVariable Long id) {
        return academicPeriodService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Crear período académico",
            description = "Crea un período identificado como AÑO-SEMESTRE, inicialmente OPEN."
    )
    public AcademicPeriodResponse create(@Valid @RequestBody AcademicPeriodRequest request) {
        return academicPeriodService.create(request);
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Cambiar estado del período",
            description = """
                    Permite cerrar un período solo cuando todas sus inscripciones activas tienen nota final.
                    Al cerrarlo actualiza los estados académicos y lo deja permanentemente cerrado; no se puede reabrir.
                    """
    )
    public AcademicPeriodResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AcademicPeriodStatusRequest request
    ) {
        return academicPeriodService.updateStatus(id, request.status());
    }
}
