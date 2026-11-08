---
name: record-decision
description: Append an architecture decision record to docs/decisions.md when a Vagor decision is made, changed, or superseded (most are the user's own product or release decisions), and update the docs that state it.
argument-hint: "<short decision title>"
---

# Record a decision

Title: `$ARGUMENTS`

Most of Vagor's decisions are the user's: what the app does, how reminders
behave, what the listing and the legal text say, how the app is sold. An
ADR records what the user decided and when, in the words of the sources. It
never adds a reason the user didn't give.

1. Read `docs/decisions.md`. The next number is the highest `ADR-NNNN` plus
   one, zero-padded to four digits.
2. Draft the ADR using the template at the top of that file:
   - **Status:** `Accepted` only if the user explicitly made or approved
     the decision in this conversation. Otherwise `Proposed`.
   - **Date:** today's date. In the text, keep the date the user decided
     ("decided with the user on 2026-11-01").
   - **Context, Decision, Consequences, Alternatives considered:** factual
     and specific, from what the user said and what the code and docs show.
     State numbers where they exist (times, limits, API levels, prices).
     Name the files, classes and settings it affects.
   - **Anything you don't know** (a reason, an alternative, a trigger the
     user didn't give): write "Not recorded." Never invent one.
   - **Revisit when:** a concrete, observable trigger the user or the
     sources give, not "if needed"; otherwise "Not recorded."
3. **If this supersedes an earlier ADR:**
   - Change only that ADR's `Status` line to `Superseded by ADR-NNNN`.
   - Do not rewrite its text. History stays intact.
4. **If the user accepts or rejects a `Proposed` ADR** without changing it,
   change only its `Status` line (`Accepted`, or `Rejected` with the date).
5. Append the new ADR at the end of `docs/decisions.md`.
6. Update every doc that states the old decision, and reference the new ADR
   number in each place you change:
   - `AGENTS.md` (what we are building, the rules)
   - `docs/00-overview.md` (the product requirements table)
   - the numbered docs and the subsystem specs in `docs/subsystems/`
   - `docs/02-dependency-policy.md` for a dependency decision
   - `.claude/rules/` files that state the old behaviour
   - `docs/roadmap.md`: tick or move the M7 item the decision settles
7. **Release decisions** the checklist asks to be written down (the refund
   policy, Age Signals) are also recorded, with their date, where
   `docs/05-release.md` asks for them.
8. **If the decision changes what the app stores, shares, promises or
   charges,** follow `.claude/rules/manifest-privacy-and-legal.md`: both
   copies of the privacy policy, `LICENSE` sections 5 and 6, and the store
   listing must stay true. Tell the user which of those need changing; don't
   change `LICENSE`'s terms without their say-so.
9. Report the ADR number and title, its status, and the list of docs
   updated. If it's `Proposed`, say what the user needs to decide.
