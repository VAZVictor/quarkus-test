package info.unterrainer.htl.service;

import info.unterrainer.htl.Student;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@Accessors(fluent = true)
@ApplicationScoped
public class DatabaseService {
    @Setter
    private Long nextId = 1L;
    private final Map<Long, Student> students = new HashMap<>();

    public void reset() {
        students.clear();
        nextId = 1L;

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Student s = Student.builder().id(inc()).fullName("Gerald Unterrainer").email("gerald" + "@unterrainer.info")
                .birthDate(LocalDateTime.parse("1975-05-02 00:00", f)).build();
        addStudent(s);
        s = Student.builder().id(inc()).fullName("Kasperl Unterrainer").email("kasperl@unterrainer.info")
                .birthDate(LocalDateTime.parse("1995-10-23 00:00", f)).build();
        addStudent(s);
    }


    public Long inc() {
        return nextId++;
    }

    public Student getStudent(long id) {
        return students.get(id);
    }

    public Collection<Student> getStudents() {
        return students.values();
    }

    public void addStudent(Student s) {
        students.put(s.getId(), s);
    }

    public void delStudent(long id) {
        students.remove(id);
    }

    public boolean containsStudent(long id) {
        return students.containsKey(id);
    }
}
