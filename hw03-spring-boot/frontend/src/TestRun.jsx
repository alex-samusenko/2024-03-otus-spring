import { useEffect, useRef, useState } from "react";
import { api } from "./api";

const DIFFICULTY = {
  EASY: "Лёгкий",
  MEDIUM: "Средний",
  HARD: "Сложный",
};

const CHOICE = {
  SINGLE: "один вариант",
  MULTIPLE: "несколько вариантов",
};

export default function TestRun({ session, onExit }) {
  const [index, setIndex] = useState(0);
  const [selections, setSelections] = useState(() => session.questions.map(() => []));
  const [result, setResult] = useState(null);
  const [remaining, setRemaining] = useState(session.remainingSeconds);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const finished = useRef(false);
  const stateRef = useRef({});
  stateRef.current = { index, selections, session };

  useEffect(() => {
    if (result) {
      return undefined;
    }
    const endsAt = Date.now() + session.remainingSeconds * 1000;
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
  }, [session, result]);

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

  async function persistAnswer(attemptId, questionIndex, selected) {
    await api(`/api/attempts/${attemptId}/answers`, {
      method: "PUT",
      body: JSON.stringify({ questionIndex, selected }),
    });
  }

  async function move(nextIndex) {
    setBusy(true);
    setError("");
    try {
      await persistAnswer(session.id, index, selections[index]);
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
    const current = snapshot ?? stateRef.current;
    setBusy(true);
    setError("");
    try {
      await persistAnswer(current.session.id, current.index, current.selections[current.index] ?? []);
      setResult(await api(`/api/attempts/${current.session.id}/finish`, { method: "POST" }));
    } catch (cause) {
      finished.current = false;
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  if (result) {
    return <Result session={session} result={result} onExit={onExit} />;
  }

  return (
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
  );
}

function QuestionStep({ session, index, selected, remaining, error, busy, onToggle, onBack, onNext }) {
  const question = session.questions[index];
  const inputType = question.choiceType === "SINGLE" ? "radio" : "checkbox";
  return (
    <section className="panel">
      <p className="eyebrow">{session.testTitle}</p>
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
            <label className="option" key={`${index}-${number}`}>
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

function Result({ session, result, onExit }) {
  return (
    <section className="panel">
      <p className="eyebrow">{session.testTitle}</p>
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
      <p className="actions">
        <button className="primary" type="button" onClick={onExit}>В кабинет</button>
      </p>
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
