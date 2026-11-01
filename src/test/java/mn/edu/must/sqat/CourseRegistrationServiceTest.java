
package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock
    StudentRepository repository;

    @Mock
    EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }
    // A1: Idevhitei oyutan amjilttai burtgeh
    @Test
    void register_activeStudent_returnsOK() {

        Student student = new Student(
                "S001",
                "student@gmail.com",
                true,
                List.of("MATH101")
        );

        when(repository.findStudent("S001")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of("MATH101"));

        String result = service.register("S001", "CS101");

        assertEquals("OK", result);

        verify(repository).saveRegistration("S001", "CS101");

        verify(emailSender).send(
                eq("student@gmail.com"),
                eq("Бүртгэл баталгаажлаа"),
                contains("CS101")
        );
    }

    // A2: Oyutan oldohgui uyd burtgel hiihgui
    @Test
    void register_studentNotFound_returnsError() {

        when(repository.findStudent("S002")).thenReturn(null);

        String result = service.register("S002", "CS101");

        assertEquals("ERROR_NO_STUDENT", result);

        verify(repository, never()).saveRegistration(anyString(), anyString());

        verifyNoInteractions(emailSender);
    }

    // A3: Idevhigui oyutan burtgeh bolomjgui
    @Test
    void register_inactiveStudent_returnsError() {

        Student student = new Student(
                "S003",
                "inactive@gmail.com",
                false,
                List.of()
        );

        when(repository.findStudent("S003")).thenReturn(student);

        String result = service.register("S003", "CS101");

        assertEquals("ERROR_INACTIVE_STUDENT", result);

        verify(repository, never()).saveRegistration(anyString(), anyString());

        verifyNoInteractions(emailSender);
    }

    // A4: Uridchilan nuhtsul hangaagui uyd burtgehgui
    @Test
    void register_missingPrerequisites_returnsError() {

        Student student = new Student(
                "S004",
                "student@gmail.com",
                true,
                List.of()
        );

        when(repository.findStudent("S004")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of("MATH101"));

        String result = service.register("S004", "CS101");

        assertEquals("ERROR_PREREQUISITES", result);

        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5: Email ilgeehed aldaa garsan ch burtgel amjilttai baina
    @Test
    void register_emailFails_registrationStillSucceeds() {

        Student student = new Student(
                "S005",
                "student@gmail.com",
                true,
                List.of()
        );

        when(repository.findStudent("S005")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        doThrow(new RuntimeException("Email sending failed"))
                .when(emailSender)
                .send(anyString(), anyString(), anyString());

        String result = service.register("S005", "CS101");

        assertEquals("OK", result);

        verify(repository).saveRegistration("S005", "CS101");

        verify(emailSender).send(
                eq("student@gmail.com"),
                eq("Бүртгэл баталгаажлаа"),
                contains("CS101")
        );
    }

    // A6: Null utga damjuulahad IllegalArgumentException uuseh
    @Test
    void register_nullInput_throwsException() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(null, "CS101")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register("S001", null)
        );

        verifyNoInteractions(repository, emailSender);
    }

    // A7: Email iin body dotor hicheeliin code baigaa eseh
    @Test
    void register_emailBodyContainsCourseId() {

        Student student = new Student(
                "S007",
                "student@gmail.com",
                true,
                List.of()
        );

        when(repository.findStudent("S007")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        String result = service.register("S007", "CS101");

        ArgumentCaptor<String> bodyCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(emailSender).send(
                eq("student@gmail.com"),
                eq("Бүртгэл баталгаажлаа"),
                bodyCaptor.capture()
        );

        assertEquals("OK", result);
        assertEquals(
                true,
                bodyCaptor.getValue().contains("CS101")
        );
    }

    // A8: Parameterized Test - gurval turliin aldaag shalgah
    @ParameterizedTest
    @CsvSource({
            "S008, ERROR_NO_STUDENT",
            "S009, ERROR_INACTIVE_STUDENT",
            "S010, ERROR_NO_COURSE"
    })
    void register_errorCases_returnsExpectedError(
            String studentId,
            String expectedResult
    ) {

        if (expectedResult.equals("ERROR_NO_STUDENT")) {

            when(repository.findStudent(studentId)).thenReturn(null);

        } else if (expectedResult.equals("ERROR_INACTIVE_STUDENT")) {

            Student student = new Student(
                    studentId,
                    "inactive@gmail.com",
                    false,
                    List.of()
            );

            when(repository.findStudent(studentId)).thenReturn(student);

        } else if (expectedResult.equals("ERROR_NO_COURSE")) {

            Student student = new Student(
                    studentId,
                    "student@gmail.com",
                    true,
                    List.of()
            );

            when(repository.findStudent(studentId)).thenReturn(student);
            when(repository.courseExists("CS101")).thenReturn(false);
        }

        String result = service.register(studentId, "CS101");

        assertEquals(expectedResult, result);
    }
}
