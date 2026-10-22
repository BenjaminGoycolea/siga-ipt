package cl.ipt.siga.service;

import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.dto.SectionResponse;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.CourseEnrollment;
import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TeacherAcademicService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public TeacherAcademicService(
            TeacherRepository teacherRepository,
            SectionRepository sectionRepository,
            AcademicPeriodRepository academicPeriodRepository,
            CourseEnrollmentRepository enrollmentRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findAssignedSections(String keycloakSubject, Long periodId) {
        Teacher teacher = getTeacherBySubject(keycloakSubject);
        ensureActive(teacher);
        return sectionRepository.findByTeacherIdAndAcademicPeriodId(teacher.getId(), periodId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollmentResponse> findSectionEnrollments(String keycloakSubject, Long sectionId) {
        Teacher teacher = getTeacherBySubject(keycloakSubject);
        ensureActive(teacher);
        ensureSectionAssignedToTeacher(teacher, sectionId);
        return enrollmentRepository.findBySectionIdAndStatusOrderByStudentName(
                        sectionId,
                        CourseEnrollmentStatus.ENROLLED
                ).stream()
                .map(CourseEnrollmentResponse::from)
                .toList();
    }

    public CourseEnrollmentResponse updateFinalGrade(
            String keycloakSubject,
            Long sectionId,
            Long enrollmentId,
            BigDecimal finalGrade
    ) {
        Teacher teacher = getTeacherBySubject(keycloakSubject);
        ensureActive(teacher);
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
        if (!enrollment.getSection().getId().equals(sectionId)) {
            throw new ResourceNotFoundException("Enrollment not found: " + enrollmentId);
        }

        Long periodId = enrollment.getAcademicPeriod().getId();
        AcademicPeriod period = academicPeriodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic period not found: " + periodId
                ));
        if (period.getStatus() != AcademicPeriodStatus.OPEN) {
            throw new BusinessConflictException("Final grades can only be changed in an open academic period.");
        }

        enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
        if (!enrollment.getSection().getId().equals(sectionId)) {
            throw new ResourceNotFoundException("Enrollment not found: " + enrollmentId);
        }
        ensureSectionAssignedToTeacher(teacher, sectionId);
        if (enrollment.getStatus() != CourseEnrollmentStatus.ENROLLED) {
            throw new BusinessConflictException("A cancelled enrollment cannot receive a final grade.");
        }

        enrollment.setFinalGrade(finalGrade);
        return CourseEnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    private Teacher getTeacherBySubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BusinessConflictException("The authenticated account has no Keycloak subject.");
        }
        return teacherRepository.findByKeycloakSubject(subject)
                .orElseThrow(() -> new BusinessConflictException(
                        "The authenticated Keycloak account is not linked to a teacher."
                ));
    }

    private void ensureActive(Teacher teacher) {
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessConflictException("Only active teachers can manage final grades.");
        }
    }

    private void ensureSectionAssignedToTeacher(Teacher teacher, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        if (!section.getTeacher().getId().equals(teacher.getId())) {
            throw new ResourceNotFoundException("Section not found: " + sectionId);
        }
    }
}
