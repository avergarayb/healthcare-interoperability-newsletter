# Healthcare Interoperability Newsletter

An independent technical and editorial project for researching, experimenting with, documenting, and publishing work on healthcare interoperability.

This repository is a public workspace. It is intended to hold source code, notes, architecture artifacts, and written editions related to standards-based health data exchange. It is not a product, a certified implementation, or a claim of professional expertise.

## Purpose

The purpose of this repository is to create a single, independent place to:

- study healthcare interoperability concepts and official standards
- run small, reproducible technical experiments
- keep source code and supporting notes together
- document technical decisions as they are made
- prepare written material that can later be published

The project exists to learn in public, keep the work inspectable, and separate facts from opinions.

## Scope

This repository covers research, experiments, documentation, and publication drafts related to healthcare interoperability.

It does **not** currently include:

- a production system
- a complete FHIR server deployment
- a finished backend application
- Docker services or databases
- AI agents or RAG pipelines

Those items may appear later as explicit experiments. They are out of scope until they are introduced and documented in this repository.

## Main topics

The work in this repository is expected to focus on:

- Healthcare interoperability
- HL7 FHIR
- integration of health information systems
- APIs and backend architecture
- artificial intelligence applied to healthcare
- later exploration of RAG and AI agents, if and when those experiments are defined

Topics will be added only when there is concrete research, code, or documentation to support them.

## Repository structure

```text
.
├── README.md
├── newsletter/          # Drafts and published editions
├── experiments/         # Reproducible technical experiments
├── docs/
│   ├── research/        # Notes, sources, and investigation material
│   ├── architecture/    # Public diagrams and architecture notes
│   └── decisions/       # Relevant technical decisions
└── infra/
    └── docker/          # Docker assets specific to this repository
```

Empty directories are kept in Git with placeholder files so the structure is visible from the first commit.

## Approach

Work in this repository should stay small, explicit, and verifiable.

- Start with a question or a constrained experiment.
- Prefer official standards and primary technical sources over secondary summaries.
- Keep each experiment reproducible from the files in this repository.
- Write down decisions that affect the design or the public documentation.
- Separate facts, observations, experiments, and conclusions.
- Do not present unfinished work as a completed system.

## Project principles

- Learn by experimentation.
- Prefer official standards and primary technical sources.
- Keep experiments reproducible.
- Document relevant technical decisions.
- Distinguish facts, observations, experiments, and conclusions.
- Avoid unsupported claims.
- Keep public documentation clean and useful.
- Never commit secrets or sensitive information.
- Keep this repository independent from other personal projects.

## Current status

The project is in its initial phase.

The repository structure and public description are in place. No experiments, services, or application code have been implemented yet.

The first planned experiment is:

**FHIR Lab #001 — Patient**

That experiment has not been started. It will be added later under `experiments/` with its own notes and source files.

## Future direction

Later work may include:

- the first FHIR experiment around the Patient resource
- additional interoperability experiments
- public architecture notes based on those experiments
- recorded technical decisions
- newsletter editions derived from the research
- eventual exploration of AI, RAG, and agents in healthcare contexts

Nothing in that list is implemented yet. Each item should be introduced with a clear scope, sources, and documentation inside this repository.
