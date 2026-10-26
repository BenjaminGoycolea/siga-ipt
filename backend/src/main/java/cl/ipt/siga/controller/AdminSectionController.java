package cl.ipt.siga.controller;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.dto.SectionResponse;
import cl.ipt.siga.service.SectionService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración de secciones", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminSectionController {

    private final SectionService sectionService;

    public AdminSectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping("/periods/{periodId}/sections")
    @Operation(
            summary = "Listar secciones de un período",
            description = "Lista las secciones de la oferta académica del período indicado."
    )
    public List<SectionResponse> findByPeriod(
            @Parameter(description = "ID del período académico.", example = "1")
            @PathVariable Long periodId
    ) {
        return sectionService.findByPeriod(periodId);
    }

    @PostMapping("/periods/{periodId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Crear sección",
            description = """
                    Agrega una asignatura a la oferta del período. El período debe estar OPEN, el docente debe estar
                    ACTIVE y no debe tener otra sección en los mismos bloques horarios. Solo puede existir una
                    sección por asignatura y período. Los bloques deben corresponder a las horas TEL.
                    """
    )
    public SectionResponse create(
            @Parameter(description = "ID del período OPEN al que se agregará la sección.", example = "1")
            @PathVariable Long periodId,
            @Valid @RequestBody SectionRequest request
    ) {
        return sectionService.create(periodId, request);
    }

    @GetMapping("/sections/{id}")
    @Operation(summary = "Consultar sección", description = "Devuelve la configuración de una sección.")
    public SectionResponse findById(@PathVariable Long id) {
        return sectionService.findById(id);
    }

    @PutMapping("/sections/{id}")
    @Operation(
            summary = "Actualizar sección",
            description = "Reemplaza asignatura, docente, capacidad y bloques horarios; el período debe seguir OPEN."
    )
    public SectionResponse update(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return sectionService.update(id, request);
    }
}
