package cl.ipt.siga.service;

import cl.ipt.siga.dto.MatriculationResponse;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.StudentMatriculation;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.StudentMatriculationRepository;
import cl.ipt.siga.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentMatriculationService {

    private final StudentMatriculationRepository matriculationRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public StudentMatriculationService(
            StudentMatriculationRepository matriculationRepository,
            StudentRepository studentRepository,
            AcademicPeriodRepository academicPeriodRepository,
            CourseEnrollmentRepository enrollmentRepository
    ) {
        this.matriculationRepository = matriculationRepository;
        this.studentRepository = studentRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public MatriculationResponse update(Long studentId, Long periodId, boolean matriculated) {
        Student student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        AcademicPeriod period = academicPeriodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + periodId));

        if (matriculated && student.getStatus() != StudentStatus.REGULAR) {
            throw new BusinessConflictException("Only regular students can be marked as matriculated.");
        }
        if (!matriculated && enrollmentRepository.existsByStudentIdAndAcademicPeriodIdAndStatus(
                studentId,
                periodId,
                CourseEnrollmentStatus.ENROLLED
        )) {
            throw new BusinessConflictException("Matriculation cannot be revoked while the student has enrollments.");
        }

        StudentMatriculation record = matriculationRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId)
                .orElseGet(() -> new StudentMatriculation(student, period, matriculated));
        record.setMatriculated(matriculated);
        return MatriculationResponse.from(matriculationRepository.save(record));
    }
}
