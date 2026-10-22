# design/ — documents about the simulated feed

Everything about **why** and **how** the simulated feed was designed, in one place.

| File | What it is |
|---|---|
| `design-log-2026-10-22.md` | The design log (image embedded). Best for reading and for editing |
| `design-log-2026-10-22.docx` | Same content as Word (image embedded, editable) |
| `design-log-2026-10-22.pdf` | Same content as PDF, 2 pages |
| `figures/two-systems-architecture-en.png` | The architecture figure, English |
| `figures/two-systems-architecture.png` | The same figure, Chinese |
| `figures/*.py`, `figures/*.html` | Sources of the figures. Edit and re-render with headless Chrome: `--headless=new --screenshot=out.png --window-size=1320,950 file.html` |

The log covers, in order: why a stand-in was built at all (minimal impact on the University system, and our own system can actually run), how it was built, what the mock can do, the API documents, and the problems it is designed to survive (with polling of `/v1/meta` as the main answer).

The code itself lives one level up, in `mock-school-api/`. The design documents sit in the same folder as the code so the two cannot drift apart.
