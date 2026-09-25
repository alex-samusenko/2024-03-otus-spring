package ru.otus.web;

import ru.otus.domain.Question;
import ru.otus.domain.Student;
import ru.otus.web.dto.AttemptResultView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Attempt {

    private final String id;

    private final Student student;

    private final List<Question> questions;

    private final AttemptMeta meta;

    private final List<Set<Integer>> selected;

    private AttemptResultView result;

    public Attempt(String id, Student student, List<Question> questions, AttemptMeta meta) {
        this.id = id;
        this.student = student;
        this.questions = List.copyOf(questions);
        this.meta = meta;
        this.selected = new ArrayList<>();
        for (var i = 0; i < this.questions.size(); i++) {
            selected.add(Set.of());
        }
    }

    public String getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public AttemptMeta getMeta() {
        return meta;
    }

    public List<Set<Integer>> getSelected() {
        return selected;
    }

    public AttemptResultView getResult() {
        return result;
    }

    public synchronized void select(int index, Set<Integer> values) {
        selected.set(index, Set.copyOf(values));
    }

    public synchronized void complete(AttemptResultView graded) {
        this.result = graded;
    }
}
