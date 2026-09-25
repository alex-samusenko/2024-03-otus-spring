import { useEffect, useRef, useState } from "react";

const DIFFICULTY = {
  EASY: "Лёгкий",
  MEDIUM: "Средний",
  HARD: "Сложный",
};

const CHOICE = {
  SINGLE: "один вариант",
  MULTIPLE: "несколько вариантов",
};

export default function App() {
  const [stage, setStage] = useState("form");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [session, setSession] = useState(null);
  const [index, setIndex] = useState(0);
  const [selections, setSelections] = useState([]);
  const [result, setResult] = useState(null);
  const [remaining, setRemaining] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const finished = useRef(false);
  const stateRef = useRef({});
  stateRef.current = { index, selections, session };

  useEffect(() => {
    if (stage !== "test" || !session) {
      return undefined;
    }
    const endsAt = Date.now() + session.timeLimitSeconds * 1000;
    const tick = () => {
      const left = Math.max(0, Math.round((endsAt - Date.now()) / 1000));
      setRemaining(left);
      if (left === 0) {
        finish(stateRef.current);
      }
    };
    tick();
    const timerId = setInterval(tick, 1000);
    return () => clearInterval(timerId);
  }, [stage, session]);

  async function start(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const response = await fetch("/api/attempts", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ firstName, lastName }),
      });
      if (!response.ok) {
        throw new Error("Не удалось начать аттестацию");
      }
      const body = await response.json();
      finished.current = false;
      setSession(body);
      setSelections(body.questions.map(() => []));
      setIndex(0);
      setResult(null);
      setStage("test");
    } catch (cause) {
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  function toggle(optionNumber) {
    const question = session.questions[index];
    setSelections((current) => current.map((selected, itemIndex) => {
      if (itemIndex !== index) {
        return selected;
      }
      if (question.choiceType === "SINGLE") {
        return [optionNumber];
      }
      return selected.includes(optionNumber)
        ? selected.filter((value) => value !== optionNumber)
        : [...selected, optionNumber];
    }));
  }

  async function persist(questionIndex, selected) {
    await persistAnswer(session.id, questionIndex, selected);
  }

  async function persistAnswer(attemptId, questionIndex, selected) {
    const response = await fetch(`/api/attempts/${attemptId}/answers`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ questionIndex, selected }),
    });
    if (!response.ok) {
      throw new Error("Ответ не сохранился");
    }
  }

  async function move(nextIndex) {
    setBusy(true);
    setError("");
    try {
      await persist(index, selections[index]);
      setIndex(nextIndex);
    } catch (cause) {
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  async function finish(snapshot) {
    if (finished.current) {
      return;
    }
    finished.current = true;
    const current = snapshot ?? { index, selections, session };
    setBusy(true);
    setError("");
    try {
      await persistAnswer(current.session.id, current.index, current.selections[current.index] ?? []);
      const response = await fetch(`/api/attempts/${current.session.id}/finish`, { method: "POST" });
      if (!response.ok) {
        throw new Error("Не удалось завершить аттестацию");
      }
      setResult(await response.json());
      setStage("result");
    } catch (cause) {
      finished.current = false;
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="app">
      {stage === "form" && (
        <Form
          firstName={firstName}
          lastName={lastName}
          error={error}
          busy={busy}
          onFirstName={setFirstName}
          onLastName={setLastName}
          onSubmit={start}
        />
      )}
      {stage === "test" && session && (
        <QuestionStep
          session={session}
          index={index}
          selected={selections[index] ?? []}
          remaining={remaining}
          error={error}
          busy={busy}
          onToggle={toggle}
          onBack={() => move(index - 1)}
          onNext={() => (index === session.questions.length - 1 ? finish() : move(index + 1))}
        />
      )}
      {stage === "result" && result && session && (
        <Result session={session} result={result} />
      )}
    </main>
  );
}

function Form({ firstName, lastName, error, busy, onFirstName, onLastName, onSubmit }) {
  return (
    <form className="panel" onSubmit={onSubmit}>
      <p className="eyebrow">Учебный центр</p>
      <h1>Аттестация слушателей</h1>
      <p className="lead">
        Ответьте на вопросы. Лёгкий вопрос весит 10 баллов, средний — 20, сложный — 40.
        Зачёт — от 80 из 100.
      </p>
      <div className="fields">
        <label>
          Имя
          <input value={firstName} onChange={(event) => onFirstName(event.target.value)} required />
        </label>
        <label>
          Фамилия
          <input value={lastName} onChange={(event) => onLastName(event.target.value)} required />
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      <p className="actions">
        <button className="primary" type="submit" disabled={busy}>Начать</button>
      </p>
    </form>
  );
}

function QuestionStep({ session, index, selected, remaining, error, busy, onToggle, onBack, onNext }) {
  const question = session.questions[index];
  const inputType = question.choiceType === "SINGLE" ? "radio" : "checkbox";
  return (
    <section className="panel">
      <div className="toolbar">
        <span>Вопрос {index + 1} из {session.questions.length}</span>
        <span className="timer">{formatTime(remaining)}</span>
      </div>
      <div className="meta">
        <span>{DIFFICULTY[question.difficulty]} · {question.weight}%</span>
        <span>{CHOICE[question.choiceType]}</span>
      </div>
      <h1>{question.text}</h1>
      <div className="options">
        {question.options.map((option, optionIndex) => {
          const number = optionIndex + 1;
          return (
            <label className="option" key={option}>
              <input
                type={inputType}
                name={`question-${index}`}
                checked={selected.includes(number)}
                onChange={() => onToggle(number)}
              />
              <span>{option}</span>
            </label>
          );
        })}
      </div>
      {error && <p className="error">{error}</p>}
      <div className="actions">
        <button className="secondary" type="button" onClick={onBack} disabled={busy || index === 0}>
          Назад
        </button>
        <button className="primary" type="button" onClick={onNext} disabled={busy}>
          {index === session.questions.length - 1 ? "Завершить" : "Далее"}
        </button>
      </div>
    </section>
  );
}

function Result({ session, result }) {
  return (
    <section className="panel">
      <p className="eyebrow">Результат</p>
      <h1 className={result.passed ? "passed" : "failed"}>
        {result.passed ? "Аттестация сдана" : "Аттестация не сдана"}
      </h1>
      <p className="result-score">{result.scorePercent} из 100</p>
      <p className="hint">
        Правильных ответов: {result.rightAnswersCount} из {result.questionsCount}.
        Проходной балл: {session.passingScore}.
      </p>
      {result.mistakes.length > 0 && (
        <ul className="mistakes">
          {result.mistakes.map((mistake) => {
            const question = session.questions[mistake.index];
            return (
              <li key={mistake.index}>
                <strong>{mistake.text}</strong>
                <p>Ваш выбор: {labels(question, mistake.selected)}</p>
                <p>Правильно: {labels(question, mistake.correct)}</p>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

function labels(question, numbers) {
  if (!numbers.length) {
    return "нет ответа";
  }
  return numbers.map((number) => question.options[number - 1]).join("; ");
}

function formatTime(totalSeconds) {
  if (totalSeconds == null) {
    return "";
  }
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}
