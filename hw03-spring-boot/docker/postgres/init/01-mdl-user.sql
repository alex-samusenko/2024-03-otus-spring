-- Локальная имитация таблицы пользователей Moodle.
-- Это не полная схема Moodle: только поля, нужные для карточки слушателя аттестации.

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
    CONSTRAINT mdl_user_username_uk UNIQUE (mnethostid, username)
);

COMMENT ON TABLE mdl_user IS
    'Имитация mdl_user из LMS Moodle для локальной разработки аттестации';

INSERT INTO mdl_user (
    username, password, idnumber, firstname, lastname, email,
    institution, department, city, country, timecreated, timemodified
) VALUES
    (
        'employee', 'not-a-real-password', 'EMP-001',
        'Слушатель', 'Корпорация', 'employee@example.test',
        'Корпорация Галактика', 'Внедрение', 'Москва', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-01-15 10:00:00')::BIGINT
    ),
    (
        'customer', 'not-a-real-password', 'CUST-001',
        'Слушатель', 'Заказчик', 'customer@example.test',
        'Заказчик внедрения', 'Проектный офис', 'Санкт-Петербург', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-02-02 09:30:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-02-02 09:30:00')::BIGINT
    ),
    (
        'partner', 'not-a-real-password', 'PART-001',
        'Слушатель', 'Партнёр', 'partner@example.test',
        'Партнёр корпорации', 'Внедрение', 'Казань', 'RU',
        EXTRACT(EPOCH FROM TIMESTAMP '2026-03-10 14:15:00')::BIGINT,
        EXTRACT(EPOCH FROM TIMESTAMP '2026-03-10 14:15:00')::BIGINT
    );
