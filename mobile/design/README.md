# design/ — documents about the phone client

| File | What it is |
|---|---|
| `design-log-2026-10-22.md` | The design log (figure embedded). Best for reading and for editing |
| `design-log-2026-10-22.docx` | Same content as Word, for review or for pasting into a report |
| `design-log-2026-10-22.pdf` | Same content as PDF, 3 pages |
| `figures/phone-to-backend-en.svg` | The figure, vector |
| `figures/phone-to-backend-en.png` | The figure, 2x raster |
| `figures/gen-figure.py` | The script that draws the figure |

The figure is generated, not drawn by hand: edit `figures/gen-figure.py` and rerun it, and the SVG
and PNG next to it are replaced.

The log follows the same shape as `mock-school-api/design/design-log-2026-10-22.md`: why the thing
exists, what was built, how the pieces call each other, what it deliberately does not do, what went
wrong and what was done about it, and what is still open.
