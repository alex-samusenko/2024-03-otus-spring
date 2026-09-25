package ru.otus.domain;

public enum Difficulty {

    EASY(10),
    MEDIUM(20),
    HARD(40);

    private final int weight;

    Difficulty(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
