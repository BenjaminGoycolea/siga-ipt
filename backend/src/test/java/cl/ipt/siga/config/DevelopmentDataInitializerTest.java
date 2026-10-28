package cl.ipt.siga.config;

import cl.ipt.siga.entity.Course;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DevelopmentDataInitializerTest {

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void developmentProfileLoadsCareersAndAllCurricula() {
        var programmingCareer = careerRepository.findByCode("10450").orElseThrow();
        var dataCareer = careerRepository.findByCode("10451").orElseThrow();
        var cybersecurityCareer = careerRepository.findByCode("10452").orElseThrow();

        assertThat(careerRepository.findByCode("10450")).isPresent();
        assertThat(careerRepository.findByCode("10451")).isPresent();
        assertThat(careerRepository.findByCode("10452")).isPresent();
        assertThat(dataCareer.getAnnualVacancies()).isEqualTo(50);
        assertThat(cybersecurityCareer.getDurationSemesters()).isEqualTo(4);

        var plan = studyPlanRepository
                .findByCareerIdAndVersion(programmingCareer.getId(), "2021.3")
                .orElseThrow();
        var courses = courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(plan.getId());

        assertThat(courses).hasSize(24);
        assertThat(plan.isCurrentPlan()).isTrue();
        assertThat(courses).extracting(Course::getCode).contains("TAP101", "TAP406");

        var webDevelopmentTwo = courseRepository.findByStudyPlanIdAndCode(plan.getId(), "TAP301").orElseThrow();
        assertThat(webDevelopmentTwo.getPrerequisites())
                .extracting(Course::getCode)
                .containsExactlyInAnyOrder("TAP201", "TAP204");

        var dataPlan = studyPlanRepository.findByCareerIdAndVersion(dataCareer.getId(), "2022.3").orElseThrow();
        var dataCourses = courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(dataPlan.getId());
        assertThat(dataCourses).hasSize(24);
        assertThat(dataCourses).extracting(Course::getSemester).contains(1, 2, 3, 4);

        var machineLearning = courseRepository.findByStudyPlanIdAndCode(dataPlan.getId(), "TAD401").orElseThrow();
        assertThat(machineLearning.getPrerequisites())
                .extracting(Course::getCode)
                .containsExactlyInAnyOrder("TAD301", "TAD302");

        assertThat(studyPlanRepository.findByCareerIdAndCurrentPlanTrue(dataCareer.getId())).contains(dataPlan);
        var cybersecurityPlan = studyPlanRepository
                .findByCareerIdAndVersion(cybersecurityCareer.getId(), "2021.3")
                .orElseThrow();
        var cybersecurityCourses = courseRepository
                .findByStudyPlanIdOrderBySemesterAscCodeAsc(cybersecurityPlan.getId());
        assertThat(cybersecurityCourses).hasSize(24);
        assertThat(cybersecurityCourses).extracting(Course::getSemester).contains(1, 2, 3, 4);
        assertThat(cybersecurityPlan.isCurrentPlan()).isTrue();

        var networkSecurity = courseRepository
                .findByStudyPlanIdAndCode(cybersecurityPlan.getId(), "TCS301")
                .orElseThrow();
        assertThat(networkSecurity.getPrerequisites())
                .extracting(Course::getCode)
                .containsExactlyInAnyOrder("TCS202", "TCS204");
        assertThat(studyPlanRepository.findByCareerIdAndCurrentPlanTrue(cybersecurityCareer.getId()))
                .contains(cybersecurityPlan);
    }
}
