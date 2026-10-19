package cl.ipt.siga.config;

import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile({"dev", "prod"})
public class DevelopmentDataInitializer implements CommandLineRunner {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final CourseRepository courseRepository;

    public DevelopmentDataInitializer(
            CareerRepository careerRepository,
            StudyPlanRepository studyPlanRepository,
            CourseRepository courseRepository
    ) {
        this.careerRepository = careerRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Career programmingCareer = findOrCreateCareer(
                "10450",
                "Técnico Analista Programador",
                "Two-year technical program in software analysis and programming.",
                2000
        );
        findOrCreateCareer(
                "10451",
                "Técnico en Análisis de Datos",
                "Two-year technical program in data analysis.",
                2018
        );
        Career cybersecurityCareer = findOrCreateCareer(
                "10452",
                "Técnico en Ciberseguridad",
                "Two-year technical program in cybersecurity.",
                2012
        );

        Career dataCareer = careerRepository.findByCode("10451").orElseThrow();

        StudyPlan programmingPlan = findOrCreatePlan(programmingCareer, "2021.3");
        setCurrentPlan(programmingPlan);
        seedCoursesIfMissing(programmingPlan, programmingCourses());

        StudyPlan dataPlan = findOrCreatePlan(dataCareer, "2022.3");
        setCurrentPlan(dataPlan);
        seedCoursesIfMissing(dataPlan, dataAnalysisCourses());

        StudyPlan cybersecurityPlan = findOrCreatePlan(cybersecurityCareer, "2021.3");
        setCurrentPlan(cybersecurityPlan);
        seedCoursesIfMissing(cybersecurityPlan, cybersecurityCourses());
    }

    private Career findOrCreateCareer(String code, String name, String description, int startYear) {
        return careerRepository.findByCode(code)
                .orElseGet(() -> careerRepository.save(new Career(code, name, description, startYear, 50)));
    }

    private StudyPlan findOrCreatePlan(Career career, String version) {
        return studyPlanRepository.findByCareerIdAndVersion(career.getId(), version)
                .orElseGet(() -> studyPlanRepository.save(new StudyPlan(version, false, career)));
    }

    private void setCurrentPlan(StudyPlan plan) {
        studyPlanRepository.findAllByCareerIdAndCurrentPlanTrue(plan.getCareer().getId()).stream()
                .filter(current -> !current.getId().equals(plan.getId()))
                .forEach(current -> current.setCurrentPlan(false));
        plan.setCurrentPlan(true);
        studyPlanRepository.save(plan);
    }

    private void seedCoursesIfMissing(StudyPlan plan, List<CourseDefinition> definitions) {
        if (courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(plan.getId()).isEmpty()) {
            seedCourses(plan, definitions);
        }
    }

    private List<CourseDefinition> programmingCourses() {
        return List.of(
                new CourseDefinition("TAP101", "Fundamentos de Programación", 1, 2, 2, 2, 6),
                new CourseDefinition("TAP102", "Matemática Aplicada", 1, 2, 2, 0, 5),
                new CourseDefinition("TAP103", "Fundamentos de Computación", 1, 2, 0, 2, 4),
                new CourseDefinition("TAP104", "Bases de Datos I", 1, 2, 0, 2, 5),
                new CourseDefinition("TAP105", "Comunicación Efectiva", 1, 2, 2, 0, 4),
                new CourseDefinition("TAP106", "Inglés Técnico I", 1, 2, 2, 0, 4),
                new CourseDefinition("TAP201", "Programación Orientada a Objetos", 2, 2, 2, 2, 6, "TAP101"),
                new CourseDefinition("TAP202", "Estructuras de Datos", 2, 2, 2, 2, 6, "TAP101"),
                new CourseDefinition("TAP203", "Bases de Datos II", 2, 2, 0, 2, 5, "TAP104"),
                new CourseDefinition("TAP204", "Desarrollo Web I", 2, 2, 0, 4, 6, "TAP101"),
                new CourseDefinition("TAP205", "Sistemas Operativos", 2, 2, 0, 2, 4, "TAP103"),
                new CourseDefinition("TAP206", "Inglés Técnico II", 2, 2, 2, 0, 3, "TAP106"),
                new CourseDefinition("TAP301", "Desarrollo Web II", 3, 2, 0, 4, 6, "TAP201", "TAP204"),
                new CourseDefinition("TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, "TAP201"),
                new CourseDefinition("TAP303", "Desarrollo de Aplicaciones Móviles", 3, 2, 0, 4, 6, "TAP201"),
                new CourseDefinition("TAP304", "Administración de Bases de Datos", 3, 2, 0, 2, 5, "TAP203"),
                new CourseDefinition("TAP305", "Redes y Comunicaciones", 3, 2, 0, 2, 4, "TAP205"),
                new CourseDefinition("TAP306", "Taller de Programación", 3, 0, 0, 4, 4, "TAP201", "TAP202"),
                new CourseDefinition("TAP401", "Arquitectura y Desarrollo de Software", 4, 2, 0, 4, 6, "TAP301", "TAP302"),
                new CourseDefinition("TAP402", "Desarrollo de Aplicaciones Cloud", 4, 2, 0, 4, 6, "TAP301"),
                new CourseDefinition("TAP403", "Seguridad de Aplicaciones", 4, 2, 0, 2, 4, "TAP301", "TAP305"),
                new CourseDefinition("TAP404", "Calidad y Pruebas de Software", 4, 2, 0, 2, 5, "TAP302"),
                new CourseDefinition("TAP405", "Proyecto de Desarrollo de Software", 4, 0, 0, 6, 6, "TAP301", "TAP302", "TAP306"),
                new CourseDefinition("TAP406", "Emprendimiento y Gestión de Proyectos", 4, 2, 2, 0, 3, "TAP302")
        );
    }

    private List<CourseDefinition> dataAnalysisCourses() {
        return List.of(
                new CourseDefinition("TAD101", "Fundamentos de Programación", 1, 2, 2, 2, 6),
                new CourseDefinition("TAD102", "Matemática Aplicada", 1, 2, 2, 0, 5),
                new CourseDefinition("TAD103", "Introducción al Análisis de Datos", 1, 2, 0, 2, 5),
                new CourseDefinition("TAD104", "Bases de Datos I", 1, 2, 0, 2, 5),
                new CourseDefinition("TAD105", "Herramientas Computacionales", 1, 0, 0, 4, 4),
                new CourseDefinition("TAD106", "Comunicación Efectiva", 1, 2, 2, 0, 4),
                new CourseDefinition("TAD201", "Programación para Análisis de Datos", 2, 2, 0, 4, 6, "TAD101"),
                new CourseDefinition("TAD202", "Probabilidad y Estadística", 2, 2, 2, 2, 6, "TAD102"),
                new CourseDefinition("TAD203", "Bases de Datos II", 2, 2, 0, 2, 5, "TAD104"),
                new CourseDefinition("TAD204", "Preparación y Calidad de Datos", 2, 2, 0, 4, 5, "TAD103", "TAD101"),
                new CourseDefinition("TAD205", "Visualización de Datos", 2, 2, 0, 2, 5, "TAD103"),
                new CourseDefinition("TAD206", "Inglés Técnico", 2, 2, 2, 0, 3),
                new CourseDefinition("TAD301", "Estadística Aplicada", 3, 2, 2, 2, 6, "TAD202"),
                new CourseDefinition("TAD302", "Minería de Datos", 3, 2, 0, 4, 6, "TAD201", "TAD202"),
                new CourseDefinition("TAD303", "Inteligencia de Negocios", 3, 2, 0, 4, 5, "TAD203", "TAD205"),
                new CourseDefinition("TAD304", "Integración y Procesamiento de Datos", 3, 2, 0, 4, 5, "TAD201", "TAD203"),
                new CourseDefinition("TAD305", "Visualización Avanzada de Datos", 3, 2, 0, 2, 4, "TAD205"),
                new CourseDefinition("TAD306", "Ética y Protección de Datos", 3, 2, 2, 0, 4, "TAD103"),
                new CourseDefinition("TAD401", "Fundamentos de Machine Learning", 4, 2, 0, 4, 6, "TAD301", "TAD302"),
                new CourseDefinition("TAD402", "Big Data y Procesamiento de Datos", 4, 2, 0, 4, 5, "TAD304"),
                new CourseDefinition("TAD403", "Analítica de Negocios", 4, 2, 0, 2, 5, "TAD301", "TAD303"),
                new CourseDefinition("TAD404", "Desarrollo de Soluciones de Datos", 4, 2, 0, 4, 5, "TAD302", "TAD304"),
                new CourseDefinition("TAD405", "Proyecto de Análisis de Datos", 4, 0, 0, 6, 6, "TAD302", "TAD303", "TAD305"),
                new CourseDefinition("TAD406", "Emprendimiento y Gestión de Proyectos", 4, 2, 2, 0, 3)
        );
    }

    private List<CourseDefinition> cybersecurityCourses() {
        return List.of(
                new CourseDefinition("TCS101", "Fundamentos de Programación", 1, 2, 2, 2, 6),
                new CourseDefinition("TCS102", "Fundamentos de Sistemas Computacionales", 1, 2, 0, 2, 5),
                new CourseDefinition("TCS103", "Redes de Computadores I", 1, 2, 0, 2, 5),
                new CourseDefinition("TCS104", "Sistemas Operativos", 1, 2, 0, 2, 5),
                new CourseDefinition("TCS105", "Matemática Aplicada", 1, 2, 2, 0, 5),
                new CourseDefinition("TCS106", "Comunicación Efectiva", 1, 2, 2, 0, 4),
                new CourseDefinition("TCS201", "Programación para Ciberseguridad", 2, 2, 0, 4, 5, "TCS101"),
                new CourseDefinition("TCS202", "Redes de Computadores II", 2, 2, 0, 4, 6, "TCS103"),
                new CourseDefinition("TCS203", "Administración de Sistemas Linux", 2, 2, 0, 4, 5, "TCS104"),
                new CourseDefinition("TCS204", "Fundamentos de Ciberseguridad", 2, 2, 2, 2, 6, "TCS102", "TCS103"),
                new CourseDefinition("TCS205", "Bases de Datos", 2, 2, 0, 2, 5, "TCS101"),
                new CourseDefinition("TCS206", "Inglés Técnico", 2, 2, 2, 0, 3),
                new CourseDefinition("TCS301", "Seguridad de Redes", 3, 2, 0, 4, 6, "TCS202", "TCS204"),
                new CourseDefinition("TCS302", "Seguridad de Sistemas Operativos", 3, 2, 0, 4, 5, "TCS203", "TCS204"),
                new CourseDefinition("TCS303", "Criptografía Aplicada", 3, 2, 2, 2, 5, "TCS204"),
                new CourseDefinition("TCS304", "Seguridad de Aplicaciones Web", 3, 2, 0, 4, 5, "TCS201", "TCS204"),
                new CourseDefinition("TCS305", "Monitoreo y Detección de Amenazas", 3, 2, 0, 4, 5, "TCS202", "TCS204"),
                new CourseDefinition("TCS306", "Legislación y Ética en Ciberseguridad", 3, 2, 2, 0, 4, "TCS204"),
                new CourseDefinition("TCS401", "Ethical Hacking y Pruebas de Penetración", 4, 2, 0, 4, 6, "TCS301", "TCS302"),
                new CourseDefinition("TCS402", "Gestión de Incidentes de Ciberseguridad", 4, 2, 0, 4, 5, "TCS305"),
                new CourseDefinition("TCS403", "Seguridad en Cloud", 4, 2, 0, 4, 5, "TCS301", "TCS302"),
                new CourseDefinition("TCS404", "Análisis Forense Digital", 4, 2, 0, 4, 5, "TCS302"),
                new CourseDefinition("TCS405", "Proyecto de Ciberseguridad", 4, 0, 0, 6, 6, "TCS301", "TCS302", "TCS305"),
                new CourseDefinition("TCS406", "Gestión de Riesgos de Ciberseguridad", 4, 2, 2, 0, 3, "TCS204")
        );
    }

    private void seedCourses(StudyPlan plan, List<CourseDefinition> definitions) {
        Map<String, Course> coursesByCode = new LinkedHashMap<>();
        for (CourseDefinition definition : definitions) {
            Course course = new Course(
                    definition.code(),
                    definition.name(),
                    definition.semester(),
                    definition.theoryHours(),
                    definition.exerciseHours(),
                    definition.laboratoryHours(),
                    definition.credits(),
                    plan
            );
            coursesByCode.put(course.getCode(), course);
        }

        courseRepository.saveAll(coursesByCode.values());

        for (CourseDefinition definition : definitions) {
            Course course = coursesByCode.get(definition.code());
            definition.prerequisiteCodes().stream()
                    .map(coursesByCode::get)
                    .forEach(course.getPrerequisites()::add);
        }
        courseRepository.saveAll(coursesByCode.values());
    }

    private record CourseDefinition(
            String code,
            String name,
            int semester,
            int theoryHours,
            int exerciseHours,
            int laboratoryHours,
            int credits,
            List<String> prerequisiteCodes
    ) {
        private CourseDefinition(
                String code,
                String name,
                int semester,
                int theoryHours,
                int exerciseHours,
                int laboratoryHours,
                int credits,
                String... prerequisiteCodes
        ) {
            this(
                    code,
                    name,
                    semester,
                    theoryHours,
                    exerciseHours,
                    laboratoryHours,
                    credits,
                    List.of(prerequisiteCodes)
            );
        }
    }
}
