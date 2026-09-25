package ru.otus.db;

record SeedUser(String username, String role, String idNumber, PersonName name, String email, OrgUnit org) {

    record PersonName(String firstName, String lastName) {
    }

    record OrgUnit(String institution, String department, String city) {
    }
}
