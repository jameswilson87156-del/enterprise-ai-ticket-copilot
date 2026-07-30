#!/usr/bin/env python3
"""Controlled policy fixture self-check for structured-output decision demo cases.

This script does not call a Provider, does not read project runtime data, and does not
modify the existing RAG baseline files. It evaluates deterministic fixture expectations
encoded in data/eval/structured_decision_eval_cases.jsonl; it does not execute the Java
implementation and must not be described as a production benchmark or model accuracy.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = ROOT / "data" / "eval" / "structured_decision_eval_cases.jsonl"


def load_cases() -> list[dict]:
    text = CASES.read_text(encoding="utf-8")
    if "??" in text:
        raise ValueError("JSONL contains consecutive question-mark placeholder text.")
    return [json.loads(line) for line in text.splitlines() if line.strip()]


def decide(case: dict) -> dict:
    retrieval_hits = set(case.get("retrieval_hits", []))
    citations = case.get("model_citations", [])
    structured_error = bool(case.get("structured_error", False))
    invalid = structured_error or not retrieval_hits or any(citation not in retrieval_hits for citation in citations)
    missing_citation = bool(retrieval_hits) and not citations and not case.get("abstained", False)
    valid = not invalid and not missing_citation
    final_abstain = bool(case.get("abstained", False)) or invalid or missing_citation
    final_review = (
        bool(case.get("model_review", False))
        or case.get("risk_level") == "HIGH"
        or final_abstain
        or invalid
        or missing_citation
        or bool(case.get("missing_information"))
    )
    unsupported_accepted = bool(case.get("unsupported_answer", False)) and not final_abstain
    return {
        "valid": valid,
        "abstain": final_abstain,
        "review": final_review,
        "unsupported_accepted": unsupported_accepted,
    }


def pct(num: int, den: int) -> float:
    return 100.0 if den == 0 else round(num * 100.0 / den, 2)


def main() -> None:
    cases = load_cases()
    decisions = [(case, decide(case)) for case in cases]
    mismatches = []
    for case, decision in decisions:
        expected = {
            "valid": bool(case.get("expected_valid")),
            "abstain": bool(case.get("expected_abstain")),
            "review": bool(case.get("expected_review")),
        }
        for field, expected_value in expected.items():
            if decision[field] != expected_value:
                mismatches.append(f"{case.get('id')}:{field}")
    valid_expected = [item for item in decisions if item[0].get("expected_valid")]
    invalid_expected = [item for item in decisions if not item[0].get("expected_valid")]
    no_evidence = [item for item in decisions if not item[0].get("retrieval_hits")]
    unsupported = [item for item in decisions if item[0].get("unsupported_answer")]
    review_expected = [item for item in decisions if item[0].get("expected_review")]

    print("POLICY_FIXTURE_SELF_CHECK=true")
    print("JAVA_IMPLEMENTATION_EXECUTED=false")
    print("PRODUCTION_BENCHMARK=false")
    print(f"cases={len(cases)}")
    print(f"Expected Field Mismatches={','.join(mismatches) if mismatches else 'NONE'}")
    print(f"Valid Citation Rate={pct(sum(1 for _, decision in valid_expected if decision['valid']), len(valid_expected)):.2f}%")
    print(f"Invalid Citation Rejection Rate={pct(sum(1 for _, decision in invalid_expected if not decision['valid']), len(invalid_expected)):.2f}%")
    print(f"No-Evidence Abstention Rate={pct(sum(1 for _, decision in no_evidence if decision['abstain']), len(no_evidence)):.2f}%")
    print(f"Unsupported Answer Acceptance Rate={pct(sum(1 for _, decision in unsupported if decision['unsupported_accepted']), len(unsupported)):.2f}%")
    print(f"Human Review Gate Recall={pct(sum(1 for _, decision in review_expected if decision['review']), len(review_expected)):.2f}%")
    if mismatches:
        sys.exit(1)


if __name__ == "__main__":
    main()
