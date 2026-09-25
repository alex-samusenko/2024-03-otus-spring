-- Локальная имитация пользователей Moodle.
-- Пароль всех учётных записей — MD5 от строки «1»: c4ca4238a0b923820dcc509a6f75849b.
-- Роль хранится в этой имитации прямо в mdl_user. В промышленном Moodle роль лежит в mdl_role_assignments.

CREATE TABLE mdl_user (
    id BIGSERIAL PRIMARY KEY,
    auth VARCHAR(20) NOT NULL DEFAULT 'manual',
    confirmed SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    suspended SMALLINT NOT NULL DEFAULT 0,
    mnethostid BIGINT NOT NULL DEFAULT 1,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    idnumber VARCHAR(255),
    firstname VARCHAR(100) NOT NULL,
    lastname VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    institution VARCHAR(255),
    department VARCHAR(255),
    city VARCHAR(120),
    country CHAR(2),
    lang VARCHAR(30) NOT NULL DEFAULT 'ru',
    timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Moscow',
    timecreated BIGINT NOT NULL,
    timemodified BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT mdl_user_username_uk UNIQUE (mnethostid, username)
);

COMMENT ON TABLE mdl_user IS
    'Имитация mdl_user из LMS Moodle для локальной разработки аттестации';

INSERT INTO mdl_user (
    username, password, idnumber, firstname, lastname, email,
    institution, department, city, country, timecreated, timemodified, role
) VALUES
    (
        'admin', 'c4ca4238a0b923820dcc509a6f75849b', 'ADM-001',
        'Администратор', 'Центра', 'admin@example.test',
        'Учебный центр', 'Администрация', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        'admin'
    ),
    (
        'manager', 'c4ca4238a0b923820dcc509a6f75849b', 'MGR-001',
        'Менеджер', 'Центра', 'manager@example.test',
        'Учебный центр', 'Аттестация', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        'manager'
    ),
    (
        'mentor', 'c4ca4238a0b923820dcc509a6f75849b', 'MNT-001',
        'Преподаватель', 'Направления', 'mentor@example.test',
        'Корпорация Галактика', 'Учебный центр', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        'mentor'
    ),
    (
        'petrov', 'c4ca4238a0b923820dcc509a6f75849b', 'MNT-002',
        'Преподаватель', 'Петров', 'petrov@example.test',
        'Корпорация Галактика', 'Учебный центр', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        'mentor'
    ),
    (
        'user', 'c4ca4238a0b923820dcc509a6f75849b', 'USR-001',
        'Слушатель', 'Демо', 'user@example.test',
        'Корпорация Галактика', 'Внедрение', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        'user'
    ),
    (
        'employee', 'c4ca4238a0b923820dcc509a6f75849b', 'EMP-001',
        'Слушатель', 'Корпорация', 'employee@example.test',
        'Корпорация Галактика', 'Внедрение', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        'user'
    ),
    (
        'customer', 'c4ca4238a0b923820dcc509a6f75849b', 'CUST-001',
        'Слушатель', 'Заказчик', 'customer@example.test',
        'Заказчик внедрения', 'Проектный офис', 'Санкт-Петербург', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-02-02 09:30:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-02-02 09:30:00')::BIGINT,
        'user'
    ),
    (
        'partner', 'c4ca4238a0b923820dcc509a6f75849b', 'PART-001',
        'Слушатель', 'Партнёр', 'partner@example.test',
        'Партнёр корпорации', 'Внедрение', 'Казань', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-03-10 14:15:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-03-10 14:15:00')::BIGINT,
        'user'
    );
