"""移动端设计日志的图：手机端 → 后端 → 数据库，5 个编号步骤（三栏 + 干净间隙）"""
from pathlib import Path

W, H = 1180, 620
INK, MUTED, LINE = "#111", "#5c6b67", "#c9d4d0"
GREEN, GREEN_FILL = "#024638", "#f4f8f6"
GOLD, GOLD_FILL = "#8a6d2f", "#fffaf0"
ACCENT = "#b49764"

P1, P2, P3 = (24, 392), (452, 844), (904, 1156)   # 三栏左右边界
TOP, BOT = 84, 556

p = []


def box(x, y, w, h, fill="#fff", stroke=LINE, r=8, sw=1.2):
    p.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" '
             f'stroke-width="{sw}"/>')


def text(x, y, s, size=13, weight=400, fill=INK, anchor="start", mono=False):
    fam = '"SF Mono", Menlo, monospace' if mono else '-apple-system, "Helvetica Neue", Arial, sans-serif'
    p.append(f'<text x="{x}" y="{y}" font-family={fam!r} font-size="{size}" font-weight="{weight}" '
             f'fill="{fill}" text-anchor="{anchor}">{s}</text>')


def arrow(x1, y1, x2, y2, stroke=GREEN, sw=2.2, dash=None, marker="ah"):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    p.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}"{d} '
             f'marker-end="url(#{marker})"/>')


def badge(cx, cy, n, fill=ACCENT):
    p.append(f'<circle cx="{cx}" cy="{cy}" r="11" fill="{fill}"/>')
    text(cx, cy + 4.2, str(n), 12, 700, "#fff", "middle")


text(28, 32, "How the phone client reaches the same backend", 17, 700, GREEN)
text(28, 52, "One API, two front ends. The backend does not know the phone app exists.", 11.5, 400, MUTED)

# ================= 栏 1：手机端 =================
box(P1[0], TOP, P1[1] - P1[0], BOT - TOP, GREEN_FILL, GREEN, 14, 1.6)
text(44, 110, "Phone client · mobile/", 14.5, 700, GREEN)
text(44, 127, "Vue 3 + Vite · localhost:5174", 10, 400, MUTED)

# 手机示意
box(44, 148, 150, 306, "#fff", GREEN, 20, 1.5)
box(96, 156, 46, 5, "#d8e4df", "#d8e4df", 3, 0)
p.append(f'<rect x="50" y="168" width="138" height="18" fill="{GREEN}" rx="0"/>')
text(56, 181, "Empty Classroom", 8.6, 700, "#fff")
box(50, 192, 138, 28, "#f7faf9", LINE, 6, 1)
text(56, 210, "Building · Date · From · To", 7.2, 400, MUTED)
box(50, 226, 138, 24, GREEN, None, 6, 0)
text(104, 242, "Search", 8.2, 700, "#fff")
for i, code in enumerate(["CPD-1.09", "KK-101", "MB-121"]):
    box(50, 260 + i * 38, 138, 32, "#fff", LINE, 6, 1)
    text(56, 273 + i * 38, code, 8.2, 650)
    text(56, 286 + i * 38, "free 15:00–16:50", 7.0, 400, MUTED)
box(50, 412, 138, 34, "#fbfdfc", LINE, 0, 1)
text(86, 432, "Find", 7.6, 650, GREEN)
text(150, 432, "Me", 7.6, 400, MUTED)

for i, (t1, t2) in enumerate([
    ("Views", "Find · Room · Me"),
    ("One API module", "axios, token in a header"),
    ("i18n", "English · 简体 · 繁體"),
]):
    y = 164 + i * 66
    box(214, y, 162, 54, "#fff", LINE, 7, 1.1)
    text(224, y + 21, t1, 11.2, 600)
    text(224, y + 37, t2, 9.0, 400, MUTED)

text(44, 470, "390 × 844, in a phone browser", 9.4, 400, MUTED)
text(44, 486, "Same origin, so no CORS", 9.4, 400, MUTED)
text(44, 502, "No install, no second backend", 9.4, 400, MUTED)
badge(60, 528, 1)
text(78, 532, "the phone opens the app on 5174", 9.2, 600, GREEN)

# ================= 栏 2：后端 =================
box(P2[0], TOP, P2[1] - P2[0], BOT - TOP, "#fff", GREEN, 14, 1.6)
text(472, 110, "Spring Boot API", 14.5, 700, GREEN)
text(472, 127, "localhost:8080 · unchanged by this work", 10, 400, MUTED)

for i, (t1, t2) in enumerate([
    ("Auth", "same token, same roles"),
    ("Availability", "timetable minus occupancy"),
    ("Reviews", "text, ratings, photos"),
]):
    y = 152 + i * 74
    box(472, y, 352, 60, "#f7faf9", LINE, 7, 1.1)
    text(484, y + 23, t1, 11.8, 600)
    text(484, y + 40, t2, 9.5, 400, MUTED)

text(472, 404, "The phone app calls the same endpoints", 9.6, 400, MUTED)
text(472, 421, "with the same token. Nothing was forked.", 9.6, 400, MUTED)
badge(488, 470, 3)
text(506, 474, "reads the request, computes free blocks", 9.2, 600, GREEN)
badge(488, 500, 5)
text(506, 504, "answers with rooms and their free windows", 9.2, 600, GREEN)

# ================= 栏 3：数据库 =================
box(P3[0], 150, P3[1] - P3[0], 150, GOLD_FILL, GOLD, 12, 1.4)
text(920, 176, "PostgreSQL", 13.5, 700, GOLD)
text(920, 195, "localhost:5433", 10, 400, MUTED, mono=True)
text(920, 213, "emptyclassroom", 10, 400, MUTED, mono=True)
for i, t in enumerate(["rooms · class_slots", "room_reviews · review_photos", "app_user · audit_log"]):
    text(920, 240 + i * 17, t, 9.3, 400, MUTED)
badge(920, 322, 4)
text(920, 348, "SQL: the timetable and", 9.2, 600, GOLD)
text(920, 364, "the published changes", 9.2, 600, GOLD)

# ================= 连线 =================
# ② 请求出去（手机 → 后端），间隙里写清代理
arrow(P1[1] + 4, 300, P2[0] - 4, 300)
badge(422, 282, 2)
text(422, 330, "proxy", 9.2, 600, GOLD, "middle")
text(422, 344, "/api", 9.0, 400, MUTED, "middle", mono=True)

# ④ 后端 ↔ 数据库（间隙里写 SQL）
arrow(P2[1] + 4, 210, P3[0] - 4, 210, GOLD, 2.0, marker="ahg")
arrow(P3[0] - 4, 240, P2[1] + 4, 240, GOLD, 1.6, marker="ahg")
text(874, 196, "SQL", 9.2, 400, MUTED, "middle")

# ⑤ 返回并渲染（沿底部走过去）
p.append(f'<path d="M648 556 V586 H196 V560" fill="none" stroke="{GREEN}" stroke-width="2" '
         f'stroke-dasharray="6 5" marker-end="url(#ah)"/>')
text(660, 580, "JSON back: rooms, free windows, reviews, photos", 9.4, 600, GREEN)

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
    <path d="M0 0 L10 5 L0 10 z" fill="{GREEN}"/>
  </marker>
  <marker id="ahg" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
    <path d="M0 0 L10 5 L0 10 z" fill="{GOLD}"/>
  </marker>
</defs>
<rect width="{W}" height="{H}" fill="#fff"/>
{chr(10).join(p)}
</svg>'''

out = Path("/Users/ran/Documents/comp1110/项目/empty-classroom/mobile/design/figures")
out.mkdir(parents=True, exist_ok=True)
(out / "phone-to-backend-en.svg").write_text(svg, encoding="utf-8")

import pymupdf
doc = pymupdf.open(str(out / "phone-to-backend-en.svg"))
doc[0].get_pixmap(matrix=pymupdf.Matrix(2, 2)).save(str(out / "phone-to-backend-en.png"))
print("ok", (out / "phone-to-backend-en.png").stat().st_size // 1024, "KB")
