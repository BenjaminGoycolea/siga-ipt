package cl.ipt.siga.service;

import cl.ipt.siga.dto.StudentRequest;
import cl.ipt.siga.dto.StudentResponse;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.entity.enums.CareerStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.StudentRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;

    public StudentService(
            StudentRepository studentRepository,
            CareerRepository careerRepository,
            StudyPlanRepository studyPlanRepository
    ) {
        this.studentRepository = studentRepository;
        this.careerRepository = careerRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(Long careerId) {
        List<Student> students = careerId == null
                ? studentRepository.findAll()
                : studentRepository.findByCareerIdOrderByFullNameAsc(careerId);
        return students.stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return StudentResponse.from(getStudent(id));
    }

    public StudentResponse create(StudentRequest request) {
        String nationalId = normalizeNationalId(request.nationalId());
        String email = normalizeEmail(request.email());
        ensureNationalIdAvailable(nationalId, null);
        ensureEmailAvailable(email, null);

        Career career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new ResourceNotFoundException("Career not found: " + request.careerId()));
        if (career.getStatus() != CareerStatus.ACTIVE) {
            throw new BusinessConflictException("Students cannot be registered in an inactive career.");
        }

        StudyPlan currentPlan = studyPlanRepository.findByCareerIdAndCurrentPlanTrue(career.getId())
                .orElseThrow(() -> new BusinessConflictException(
                        "The career has no current study plan and cannot receive new students."
                ));

        Student student = new Student(
                nationalId,
                request.fullName().trim(),
                email,
                career,
                currentPlan
        );
        return StudentResponse.from(studentRepository.save(student));
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student student = getStudent(id);
        String nationalId = normalizeNationalId(request.nationalId());
        String email = normalizeEmail(request.email());
        ensureNationalIdAvailable(nationalId, id);
        ensureEmailAvailable(email, id);

        Career career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new ResourceNotFoundException("Career not found: " + request.careerId()));

        if (!student.getCareer().getId().equals(career.getId())) {
            throw new BusinessConflictException(
                    "Changing a student's career is not supported."
            );
        }

        student.setNationalId(nationalId);
        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        return StudentResponse.from(studentRepository.save(student));
    }

    public StudentResponse updateStatus(Long id, StudentStatus status) {
        Student student = getStudent(id);
        if (status == StudentStatus.GRADUATED || status == StudentStatus.DISMISSED) {
            throw new BusinessConflictException(
                    "Graduated and dismissed statuses are assigned automatically during semester closure."
            );
        }
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.DISMISSED) {
            throw new BusinessConflictException("A graduated or dismissed student status cannot be changed.");
        }
        student.setStatus(status);
        return StudentResponse.from(studentRepository.save(student));
    }

    public StudentResponse updateKeycloakSubject(Long id, String keycloakSubject) {
        Student student = getStudent(id);
        String subject = keycloakSubject.trim();
        studentRepository.findByKeycloakSubject(subject)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("This Keycloak account is already linked to another student.");
                });
        student.setKeycloakSubject(subject);
        return StudentResponse.from(studentRepository.save(student));
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    private void ensureNationalIdAvailable(String nationalId, Long excludedStudentId) {
        studentRepository.findByNationalId(nationalId)
                .filter(existing -> !existing.getId().equals(excludedStudentId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A student with this national ID already exists.");
                });
    }

    private void ensureEmailAvailable(String email, Long excludedStudentId) {
        studentRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(excludedStudentId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A student with this email already exists.");
                });
    }

    private String normalizeNationalId(String nationalId) {
        return nationalId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
