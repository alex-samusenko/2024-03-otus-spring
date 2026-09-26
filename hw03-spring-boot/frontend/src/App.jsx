import { useEffect, useState } from "react";
import { api } from "./api";
import TestRun from "./TestRun";

const ROLE = {
  admin: "Администратор",
  manager: "Менеджер",
  mentor: "Преподаватель",
  user: "Слушатель",
};

export default function App() {
  const [user, setUser] = useState(undefined);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api("/api/session")
      .then((current) => {
        if (active) {
          setUser(current);
        }
      })
      .catch((cause) => {
        if (!active) {
          return;
        }
        if (cause.status === 401) {
          setUser(null);
          return;
        }
        setError(cause.message);
      });
    return () => {
      active = false;
    };
  }, []);

  async function logout() {
    await api("/api/session", { method: "DELETE" });
    setUser(null);
  }

  if (error && user === undefined) {
    return <main className="app"><p className="error">{error}</p></main>;
  }
  if (user === undefined) {
    return <main className="app"><p>Загрузка…</p></main>;
  }
  if (!user) {
    return <Login onSuccess={setUser} />;
  }
  return (
    <main className="app">
      <header className="topbar">
        <div>
          <p className="eyebrow">{ROLE[user.role] ?? user.role}</p>
          <strong>{user.lastName} {user.firstName}</strong>
        </div>
        <button className="secondary" type="button" onClick={logout}>Выйти</button>
      </header>
      {user.role === "admin" && <AdminCabinet />}
      {user.role === "manager" && <ManagerCabinet />}
      {user.role === "mentor" && <MentorCabinet />}
      {user.role === "user" && <ListenerCabinet />}
    </main>
  );
}

function Login({ onSuccess }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      onSuccess(await api("/api/session", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      }));
    } catch (cause) {
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="app">
      <form className="panel" onSubmit={submit}>
        <p className="eyebrow">Учебный центр</p>
        <h1>Вход в аттестацию</h1>
        <p className="lead">Логин и пароль берутся из имитации пользователей Moodle.</p>
        <p className="accounts">
          Демо-пароль у всех учётных записей: 1.
          Роли: admin, manager, mentor, petrov, user.
        </p>
        <div className="fields">
          <label>
            Логин
            <input
              type="text"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              required
            />
          </label>
          <label>
            Пароль
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
        </div>
        {error && <p className="error">{error}</p>}
        <p className="actions">
          <button className="primary" type="submit" disabled={busy}>Войти</button>
        </p>
      </form>
    </main>
  );
}

function AdminCabinet() {
  const [listeners, setListeners] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api("/api/listeners").then(setListeners).catch((cause) => setError(cause.message));
  }, []);

  return (
    <section className="panel">
      <h1>Слушатели</h1>
      {error && <p className="error">{error}</p>}
      {!listeners && !error && <p>Загрузка…</p>}
      {listeners && listeners.length === 0 && <p className="hint">Слушателей пока нет.</p>}
      {listeners && listeners.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Логин</th>
                <th>Слушатель</th>
                <th>Почта</th>
                <th>Организация</th>
                <th>Подразделение</th>
              </tr>
            </thead>
            <tbody>
              {listeners.map((listener) => (
                <tr key={listener.username}>
                  <td>{listener.username}</td>
                  <td>{listener.lastName} {listener.firstName}</td>
                  <td>{listener.email}</td>
                  <td>{listener.institution}</td>
                  <td>{listener.department}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function ManagerCabinet() {
  const [listeners, setListeners] = useState([]);
  const [tests, setTests] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [username, setUsername] = useState("");
  const [testCode, setTestCode] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function load() {
    const [listenerRows, testRows, assignmentRows, attemptRows] = await Promise.all([
      api("/api/listeners"),
      api("/api/tests"),
      api("/api/assignments"),
      api("/api/reports/attempts"),
    ]);
    setListeners(listenerRows);
    setTests(testRows);
    setAssignments(assignmentRows);
    setAttempts(attemptRows);
    setUsername((current) => current || listenerRows[0]?.username || "");
    setTestCode((current) => current || testRows[0]?.code || "");
  }

  useEffect(() => {
    load().catch((cause) => setError(cause.message));
  }, []);

  async function assign(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await api("/api/assignments", {
        method: "POST",
        body: JSON.stringify({ username, testCode }),
      });
      setMessage("Тест назначен");
      await load();
    } catch (cause) {
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <section className="panel">
        <h1>Назначить тест</h1>
        <form className="fields" onSubmit={assign}>
          <label>
            Слушатель
            <select value={username} onChange={(event) => setUsername(event.target.value)} required>
              {listeners.map((listener) => (
                <option key={listener.username} value={listener.username}>
                  {listener.lastName} {listener.firstName} ({listener.username})
                </option>
              ))}
            </select>
          </label>
          <label>
            Тест
            <select value={testCode} onChange={(event) => setTestCode(event.target.value)} required>
              {tests.map((test) => (
                <option key={test.code} value={test.code}>
                  {test.title} · {test.mentor}
                </option>
              ))}
            </select>
          </label>
          {message && <p className="ok">{message}</p>}
          {error && <p className="error">{error}</p>}
          <p className="actions">
            <button className="primary" type="submit" disabled={busy}>Назначить</button>
          </p>
        </form>
      </section>
      <section className="panel stack">
        <h2>Назначения</h2>
        <AssignmentTable rows={assignments} />
        <h2>Попытки</h2>
        <AttemptTable rows={attempts} showListener />
      </section>
    </>
  );
}

function MentorCabinet() {
  const [tests, setTests] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api("/api/mentor/tests").then(setTests).catch((cause) => setError(cause.message));
  }, []);

  return (
    <section className="panel">
      <h1>Мои тесты</h1>
      {error && <p className="error">{error}</p>}
      {tests && tests.length === 0 && <p className="hint">За вами не закреплено ни одного теста.</p>}
      {tests && tests.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Тест</th>
                <th>Удачные</th>
                <th>Неудачные</th>
              </tr>
            </thead>
            <tbody>
              {tests.map((test) => (
                <tr key={test.code}>
                  <td>{test.title}</td>
                  <td>{test.passedCount}</td>
                  <td>{test.failedCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function ListenerCabinet() {
  const [assignments, setAssignments] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [session, setSession] = useState(null);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState("");

  async function load() {
    const [assignmentRows, attemptRows] = await Promise.all([
      api("/api/my/assignments"),
      api("/api/my/attempts"),
    ]);
    setAssignments(assignmentRows);
    setAttempts(attemptRows);
  }

  useEffect(() => {
    load().catch((cause) => setError(cause.message));
  }, []);

  async function openAssignment(assignment) {
    setBusyId(assignment.id);
    setError("");
    try {
      const next = assignment.activeAttemptId
        ? await api(`/api/attempts/${assignment.activeAttemptId}`)
        : await api("/api/attempts", {
          method: "POST",
          body: JSON.stringify({ assignmentId: assignment.id }),
        });
      setSession(next);
    } catch (cause) {
      setError(cause.message);
    } finally {
      setBusyId("");
    }
  }

  if (session) {
    return (
      <TestRun
        session={session}
        onExit={() => {
          setSession(null);
          load().catch((cause) => setError(cause.message));
        }}
      />
    );
  }

  return (
    <section className="panel">
      <h1>Моя аттестация</h1>
      {error && <p className="error">{error}</p>}
      <h2>Назначенные тесты</h2>
      {assignments.length === 0 && <p className="hint">Менеджер пока не назначил тест.</p>}
      {assignments.length > 0 && (
        <ul className="assignment-list">
          {assignments.map((assignment) => (
            <li key={assignment.id}>
              <span>{assignment.testTitle}</span>
              <button
                className="primary"
                type="button"
                disabled={busyId === assignment.id}
                onClick={() => openAssignment(assignment)}
              >
                {assignment.activeAttemptId ? "Продолжить" : "Пройти"}
              </button>
            </li>
          ))}
        </ul>
      )}
      <h2>Мои попытки</h2>
      <AttemptTable rows={attempts} />
    </section>
  );
}

function AssignmentTable({ rows }) {
  if (rows.length === 0) {
    return <p className="hint">Назначений пока нет.</p>;
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Слушатель</th>
            <th>Тест</th>
            <th>Дата</th>
            <th>Статус</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.listenerName}</td>
              <td>{row.testTitle}</td>
              <td>{formatDate(row.assignedAt)}</td>
              <td>{row.finished ? "завершено" : "ожидает"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AttemptTable({ rows, showListener = false }) {
  if (rows.length === 0) {
    return <p className="hint">Попыток пока нет.</p>;
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {showListener && <th>Слушатель</th>}
            <th>Тест</th>
            <th>Дата</th>
            <th>Результат</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={`${row.listenerName}-${row.finishedAt}-${row.testTitle}`}>
              {showListener && <td>{row.listenerName}</td>}
              <td>{row.testTitle}</td>
              <td>{formatDate(row.finishedAt)}</td>
              <td className={row.passed ? "passed" : "failed"}>{resultLabel(row)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function resultLabel(row) {
  return `${row.scorePercent} из 100 · ${row.passed ? "сдана" : "не сдана"}`;
}

function formatDate(value) {
  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
