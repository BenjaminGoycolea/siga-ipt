package cl.ipt.siga.service;

import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AcademicHistoryService {

    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public AcademicHistoryService(
            StudentRepository studentRepository,
            CourseEnrollmentRepository enrollmentRepository
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<CourseEnrollmentResponse> findOwnHistory(String keycloakSubject) {
        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new BusinessConflictException("The authenticated account has no Keycloak subject.");
        }
        Long studentId = studentRepository.findByKeycloakSubject(keycloakSubject)
                .orElseThrow(() -> new BusinessConflictException(
                        "The authenticated Keycloak account is not linked to a student."
                ))
                .getId();
        return findHistory(studentId);
    }

    public List<CourseEnrollmentResponse> findStudentHistory(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        return findHistory(studentId);
    }

    private List<CourseEnrollmentResponse> findHistory(Long studentId) {
        return enrollmentRepository.findAcademicHistory(
                        studentId,
                        CourseEnrollmentStatus.ENROLLED,
                        AcademicPeriodStatus.CLOSED
                ).stream()
                .map(CourseEnrollmentResponse::from)
                .toList();
    }
}
