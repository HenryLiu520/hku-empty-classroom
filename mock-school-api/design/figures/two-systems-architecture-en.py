"""架构图（英文版）：主系统 + 模拟学校接口。浅色，供报告使用。"""
from pathlib import Path

W, H = 1320, 950
INK, MUTED, LINE = "#111", "#5c6b67", "#c9d4d0"
LEFT_FILL, LEFT_STROKE = "#f4f8f6", "#024638"
RIGHT_FILL, RIGHT_STROKE = "#fbf7ef", "#8a6d2f"
ACCENT, API, FAIL = "#b49764", "#024638", "#a3312c"

p = []


def box(x, y, w, h, fill="#fff", stroke=LINE, r=8, sw=1.2, dash=None):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    p.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" '
             f'stroke-width="{sw}"{d}/>')


def text(x, y, s, size=13, weight=400, fill=INK, anchor="start", mono=False):
    fam = '"SF Mono", Menlo, monospace' if mono else '-apple-system, "Helvetica Neue", Arial, sans-serif'
    p.append(f'<text x="{x}" y="{y}" font-family={fam!r} font-size="{size}" font-weight="{weight}" '
             f'fill="{fill}" text-anchor="{anchor}">{s}</text>')


def arrow(x1, y1, x2, y2, stroke=API, sw=2.2, dash=None, marker="ah"):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    p.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}"{d} '
             f'marker-end="url(#{marker})"/>')


def badge(cx, cy, n, fill=ACCENT):
    p.append(f'<circle cx="{cx}" cy="{cy}" r="10.5" fill="{fill}"/>')
    text(cx, cy + 4, str(n), 11.5, 700, "#fff", "middle")


# ================= boundaries
box(24, 84, 520, 520, LEFT_FILL, LEFT_STROKE, 14, 1.6)
text(42, 112, "Main system · Empty Classroom", 15.5, 700, LEFT_STROKE)
text(42, 131, "In production (the graded system) · Vue + Spring Boot + PostgreSQL", 11, 400, MUTED)

box(760, 84, 536, 520, RIGHT_FILL, RIGHT_STROKE, 14, 1.6)
text(778, 112, "Simulated school feed · mock-school-api", 15.5, 700, RIGHT_STROKE)
text(778, 131, "Test double · holds its own timetable; imitates the read-only feed the University will provide",
     11, 400, MUTED)

# ================= left: Vue
box(56, 200, 180, 62, "#fff", LEFT_STROKE)
text(68, 226, "Vue 3 single-page app", 12.5, 600)
text(68, 245, "localhost:5173", 10.5, 400, MUTED, mono=True)

# ================= left: Spring
box(296, 168, 224, 250, "#fff", LEFT_STROKE, 10, 1.4)
text(308, 192, "Spring Boot API", 12.5, 600)
text(308, 209, "localhost:8080", 10.5, 400, MUTED, mono=True)
for i, (t1, t2) in enumerate([
    ("Business logic", "availability · roles · audit"),
    ("TimetableProvider", "upstream adapter (to be added)"),
    ("Cache · last good snapshot", "degraded mode when the feed fails"),
]):
    y = 224 + i * 60
    hi = i == 1
    box(308, y, 200, 48, "#eaf3ef" if hi else "#f7faf9", LEFT_STROKE if hi else LINE, 7, 1.2)
    text(318, y + 20, t1, 12, 650 if hi else 500)
    text(318, y + 35, t2, 10, 400, MUTED)

# ================= left: DB
box(96, 496, 210, 72, "#fff", LEFT_STROKE, 10, 1.2)
text(108, 521, "PostgreSQL", 12, 600)
text(108, 539, "localhost:5433", 10, 400, MUTED, mono=True)
text(108, 555, "database: emptyclassroom", 10, 400, MUTED, mono=True)

# ================= right: Spring
box(800, 168, 456, 250, "#fff", RIGHT_STROKE, 10, 1.4)
text(812, 192, "Spring Boot", 12.5, 600)
text(812, 209, "localhost:9000", 10.5, 400, MUTED, mono=True)
for i, (t1, t2) in enumerate([
    ("GET /v1/meta", "provider · lastUpdated · revision"),
    ("GET /v1/rooms · /v1/timetable?date=", "read-only contract (needs X-Api-Key)"),
    ("/v1/admin/** · /v1/_control/**", "write side · stale / slow / down switches"),
]):
    y = 224 + i * 60
    box(812, y, 432, 48, "#fdfaf4", RIGHT_STROKE if i < 2 else LINE, 7, 1.2)
    text(822, y + 20, t1, 11, 600, INK, mono=True)
    text(822, y + 35, t2, 10, 400, MUTED)

# ================= right: DB + console
box(812, 496, 196, 72, "#fff", RIGHT_STROKE, 10, 1.2)
text(824, 521, "PostgreSQL", 12, 600)
text(824, 539, "localhost:5433", 10, 400, MUTED, mono=True)
text(824, 555, "database: mockschool", 10, 400, MUTED, mono=True)

box(1044, 496, 212, 72, "#fff", RIGHT_STROKE, 10, 1.2)
text(1056, 521, "Control panel (browser)", 12, 600)
text(1056, 539, "127.0.0.1:9000/", 10, 400, MUTED, mono=True)
text(1056, 555, "timetable grid · edit rooms · flip switches", 9.5, 400, MUTED)

# ================= left internals
arrow(236, 231, 296, 231)
badge(266, 206, "1")
text(238, 224, "request", 9.5, 500, MUTED)

# return path: computed answer goes back to the page
arrow(296, 252, 236, 252, LEFT_STROKE, 1.8)
badge(266, 272, "5")
text(240, 246, "answer", 9.5, 500, MUTED)
text(60, 292, "① the user asks: which rooms", 10.5, 500, MUTED)
text(60, 307, "are free 14:00–15:50 that day?", 10.5, 500, MUTED)

arrow(380, 418, 260, 496, LEFT_STROKE, 1.8)
text(268, 466, "reads timetable · writes changes · reviews", 10, 400, MUTED)

# ================= right internals
arrow(910, 418, 910, 496, RIGHT_STROKE, 1.8)
text(920, 464, "SQL", 10, 400, MUTED)
p.append(f'<line x1="1130" y1="496" x2="1080" y2="418" stroke="{RIGHT_STROKE}" stroke-width="1.6"/>')
text(1140, 464, "same-origin static page", 10, 400, MUTED)

# step 4: our own computation (timetable minus occupancy)
badge(486, 240, "4")

# ================= main link (HTTP)
AY = 292
arrow(520, AY, 760, AY, API, 3.2)
badge(540, AY - 58, "2")
badge(748, AY - 58, "3")

text(556, 258, "GET /v1/meta", 10.5, 650, API, mono=True)
text(556, 274, "GET /v1/timetable?date=", 10.5, 650, API, mono=True)
text(556, AY + 18, "Header: X-Api-Key", 10, 500, MUTED, mono=True)
text(556, AY + 34, "← JSON: slots, lastUpdated, revision", 10, 500, MUTED, mono=True)
text(556, AY + 54, "the only channel: HTTP + API key", 10.5, 700, API)

# ================= failure path (U-shape, below)
p.append(f'<path d="M 762 404 L 762 446 L 528 446 L 528 300 L 514 300" '
         f'fill="none" stroke="{FAIL}" stroke-width="1.8" stroke-dasharray="6 5" marker-end="url(#ahr)"/>')
text(486, 464, "503 / timeout → fall back to the last good snapshot", 10.5, 650, FAIL)
text(486, 480, "and state the data time in the answer", 10, 400, FAIL)

# ================= contract
box(430, 636, 470, 58, "#fff", ACCENT, 10, 1.6, "7 5")
text(452, 660, "The only shared artefact: the OpenAPI 3.1 contract", 12, 700, "#8a6d2f")
text(452, 678, "mock-school-api/docs/openapi.yaml · no shared database, code or files", 10, 400, MUTED)
p.append(f'<line x1="540" y1="636" x2="470" y2="608" stroke="{ACCENT}" stroke-width="1.2" stroke-dasharray="5 4"/>')
p.append(f'<line x1="790" y1="636" x2="860" y2="608" stroke="{ACCENT}" stroke-width="1.2" stroke-dasharray="5 4"/>')

# ================= steps
text(24, 726, "How one call flows", 13, 700, INK)
steps = [
    ("1", "Vue → Spring: the user asks which room is free 14:00–15:50 on a given day"),
    ("2", "Spring's adapter → simulated feed: GET /v1/meta, read lastUpdated and revision"),
    ("3", "if revision changed: GET /v1/timetable?date=…  (Header: X-Api-Key)"),
    ("4", "Spring computes free blocks as timetable minus registered occupancy"),
    ("5", "Spring → Vue: the free rooms, plus the data time (and a stale warning if needed)"),
]
for i, (n, s) in enumerate(steps):
    y = 758 + i * 26
    badge(34, y - 4, n)
    text(56, y, s, 11.5, 500, INK)
text(24, 758 + 5 * 26 + 6,
     "Isolation: two processes · two databases · zero code coupling — the thick arrow is the only channel.",
     11.5, 650, MUTED)

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <marker id="ah" markerWidth="11" markerHeight="11" refX="9" refY="5.5" orient="auto">
    <path d="M0,1 L10,5.5 L0,10 z" fill="{API}"/></marker>
  <marker id="ahr" markerWidth="11" markerHeight="11" refX="9" refY="5.5" orient="auto">
    <path d="M0,1 L10,5.5 L0,10 z" fill="{FAIL}"/></marker>
  <marker id="ahg" markerWidth="11" markerHeight="11" refX="9" refY="5.5" orient="auto">
    <path d="M0,1 L10,5.5 L0,10 z" fill="{ACCENT}"/></marker>
</defs>
<rect width="{W}" height="{H}" fill="#fff"/>
{chr(10).join(p)}
</svg>'''

Path("/tmp/arch-two-systems-en.html").write_text(
    f'<!doctype html><html><head><meta charset="utf-8"><style>html,body{{margin:0;background:#fff}}</style></head>'
    f'<body>{svg}</body></html>')
print("ok")
