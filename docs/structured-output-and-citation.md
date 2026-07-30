# Structured Output, Citation Validation and Abstention

Phase 4 upgrades Copilot runtime output from free text into a bounded structured result contract. This is still a demo-friendly internal ticket assistant flow, not an automatic ticket resolution system and not production reliability evidence.

## Structured result contract

Each new `copilot_run` stores one immutable `copilot_result` row with:

- `answer`: final accepted operator-facing suggestion, bounded to 1200 characters.
- `riskLevel`: `LOW`, `MEDIUM` or `HIGH`.
- `modelHumanReviewRequired`: the model/local-rule review recommendation.
- `finalHumanReviewRequired`: the system-enforced review gate result.
- `missingInformation`: bounded JSON array of short missing-information labels.
- `abstained`: whether the final result is a safe refusal.
- `abstentionReasonCode`: finite machine code, separate from the user-visible answer.
- `outputValidationStatus`: structured JSON validation status.
- `citationValidationStatus`: Citation ID validation status.

The system does not persist the raw Provider response, the full prompt, API keys, Authorization headers, Base URLs or upstream exception bodies.

## Prompt evidence boundary

Provider prompts are built only from the current run's retrieval snapshots. Each allowed evidence item contains:

- `knowledgeArticleId` copied from the immutable retrieval snapshot's article number.
- title snapshot.
- category snapshot.
- controlled excerpt snapshot.
- relevance score.

The prompt instructs the Provider to return exactly one JSON object, not Markdown, and to cite only the supplied `knowledgeArticleId` values. Full knowledge articles, `errorLog`, secrets, URLs and external sources are not included in the prompt.

## Citation validation boundary

Citation validation checks that model citations reference only the current run's immutable `retrieval_hit` rows. It rejects:

- Citation IDs from another run.
- Knowledge articles that exist but were not retrieved in this run.
- invented IDs.
- non-abstention answers without Citation.
- abstention answers with Citation.

Validated citations are persisted in `copilot_result_citation` and link back to `retrieval_hit`. This proves the Citation ID is in the allowed evidence set; it is not sentence-level entailment or full factual verification.

## Abstention and review gate

The system creates safe abstentions when there is no retrieval evidence, the structured output is invalid, the Provider omits citations, or citation validation fails. With no retrieval hit, the remote Provider is not called.

Final human review is system-gated by:

- model/local-rule review recommendation.
- `riskLevel=HIGH`.
- fallback usage.
- abstention.
- structured output validation failure.
- citation validation failure.
- existing ticket business rules.

The model cannot disable mandatory review by returning `humanReviewRequired=false`. No flow automatically approves or closes a ticket.

## Trace and legacy compatibility

`GET /api/tickets/{id}/trace-evidence` continues to return `evidenceSource=LEGACY_DERIVED` for old data and `IMMUTABLE_RUN` for new runs. New traces replay persisted structured results and validated citations from `copilot_result` / `copilot_result_citation`; they do not call a Provider, rerun retrieval, or revalidate citations at read time.

Frontend showcase pages remain disconnected from the real backend in this phase. No Responses API adapter, vector database, distributed trace, or production benchmark is introduced.
