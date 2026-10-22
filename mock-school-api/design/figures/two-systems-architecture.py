"""架构图 v2：加宽两系统之间的空隙，标签放进去；失败路径走下方 U 形"""
from pathlib import Path

W, H = 1320, 900
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
    fam = '"SF Mono", Menlo, monospace' if mono else '-apple-system, "PingFang SC", Arial, sans-serif'
    p.append(f'<text x="{x}" y="{y}" font-family={fam!r} font-size="{size}" font-weight="{weight}" '
             f'fill="{fill}" text-anchor="{anchor}">{s}</text>')


def arrow(x1, y1, x2, y2, stroke=API, sw=2.2, dash=None, marker="ah"):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    p.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}"{d} '
             f'marker-end="url(#{marker})"/>')


def badge(cx, cy, n, fill=ACCENT):
    p.append(f'<circle cx="{cx}" cy="{cy}" r="10.5" fill="{fill}"/>')
    text(cx, cy + 4, str(n), 11.5, 700, "#fff", "middle")


# ================= 边界
box(24, 84, 520, 520, LEFT_FILL, LEFT_STROKE, 14, 1.6)
text(42, 112, "主系统 · Empty Classroom", 15.5, 700, LEFT_STROKE)
text(42, 131, "生产用（被评分的这套）· Vue + Spring Boot + PostgreSQL", 11, 400, MUTED)

box(760, 84, 536, 520, RIGHT_FILL, RIGHT_STROKE, 14, 1.6)
text(778, 112, "模拟学校接口 · mock-school-api", 15.5, 700, RIGHT_STROKE)
text(778, 131, "测试替身 · 自己持有课表，模仿学校将来会提供的只读接口", 11, 400, MUTED)

# ================= 左：Vue
box(56, 200, 180, 62, "#fff", LEFT_STROKE)
text(68, 226, "Vue 3 单页应用", 12.5, 600)
text(68, 245, "localhost:5173", 10.5, 400, MUTED, mono=True)

# ================= 左：Spring
box(296, 168, 224, 250, "#fff", LEFT_STROKE, 10, 1.4)
text(308, 192, "Spring Boot API", 12.5, 600)
text(308, 209, "localhost:8080", 10.5, 400, MUTED, mono=True)
for i, (t1, t2) in enumerate([
    ("业务逻辑", "可用性计算 / 角色 / 审计"),
    ("TimetableProvider", "上游适配层（要加的那层）"),
    ("缓存 · 最近一次快照", "上游不可用时降级"),
]):
    y = 224 + i * 60
    hi = i == 1
    box(308, y, 200, 48, "#eaf3ef" if hi else "#f7faf9", LEFT_STROKE if hi else LINE, 7, 1.2)
    text(318, y + 20, t1, 12, 650 if hi else 500)
    text(318, y + 35, t2, 10, 400, MUTED)

# ================= 左：DB
box(96, 496, 210, 72, "#fff", LEFT_STROKE, 10, 1.2)
text(108, 521, "PostgreSQL", 12, 600)
text(108, 539, "localhost:5433", 10, 400, MUTED, mono=True)
text(108, 555, "库：emptyclassroom", 10, 400, MUTED, mono=True)

# ================= 右：Spring
box(800, 168, 456, 250, "#fff", RIGHT_STROKE, 10, 1.4)
text(812, 192, "Spring Boot", 12.5, 600)
text(812, 209, "localhost:9000", 10.5, 400, MUTED, mono=True)
for i, (t1, t2) in enumerate([
    ("GET /v1/meta", "provider · lastUpdated · revision"),
    ("GET /v1/rooms · /v1/timetable?date=", "只读契约（需要 X-Api-Key）"),
    ("/v1/admin/** · /v1/_control/**", "写接口 · 变旧 / 变慢 / 挂掉 开关"),
]):
    y = 224 + i * 60
    box(812, y, 432, 48, "#fdfaf4", RIGHT_STROKE if i < 2 else LINE, 7, 1.2)
    text(822, y + 20, t1, 11, 600, INK, mono=True)
    text(822, y + 35, t2, 10, 400, MUTED)

# ================= 右：DB + 控制台
box(812, 496, 196, 72, "#fff", RIGHT_STROKE, 10, 1.2)
text(824, 521, "PostgreSQL", 12, 600)
text(824, 539, "localhost:5433", 10, 400, MUTED, mono=True)
text(824, 555, "库：mockschool", 10, 400, MUTED, mono=True)

box(1044, 496, 212, 72, "#fff", RIGHT_STROKE, 10, 1.2)
text(1056, 521, "控制台（浏览器）", 12, 600)
text(1056, 539, "127.0.0.1:9000/", 10, 400, MUTED, mono=True)
text(1056, 555, "看课表网格 / 改教室 / 触发开关", 10, 400, MUTED)

# ================= 左内部连线
arrow(236, 231, 296, 231)
badge(266, 212, "1")
text(60, 292, "① 用户：某天 14:00–15:50", 10.5, 500, MUTED)
text(60, 307, "哪间教室空？", 10.5, 500, MUTED)

arrow(380, 418, 260, 496, LEFT_STROKE, 1.8)
text(268, 466, "读课表 / 写占用 / 评价", 10, 400, MUTED)

# ================= 右内部连线
arrow(910, 418, 910, 496, RIGHT_STROKE, 1.8)
text(920, 464, "SQL", 10, 400, MUTED)

p.append(f'<line x1="1130" y1="496" x2="1080" y2="418" stroke="{RIGHT_STROKE}" stroke-width="1.6"/>')
text(1140, 464, "同源静态页", 10, 400, MUTED)

# ================= 主连接（HTTP）
AY = 292
arrow(520, AY, 760, AY, API, 3.2)
badge(540, AY - 58, "2")
badge(748, AY - 58, "3")

text(556, 258, "GET /v1/meta", 10.5, 650, API, mono=True)
text(556, 274, "GET /v1/timetable?date=", 10.5, 650, API, mono=True)
text(556, AY + 18, "Header: X-Api-Key", 10, 500, MUTED, mono=True)
text(556, AY + 34, "← JSON: slots, lastUpdated, revision", 10, 500, MUTED, mono=True)
text(556, AY + 54, "唯一通道：HTTP + API key", 10.5, 700, API)

# ================= 失败路径（U 形，走下方）
p.append(f'<path d="M 762 404 L 762 446 L 472 446 L 472 348" '
         f'fill="none" stroke="{FAIL}" stroke-width="1.8" stroke-dasharray="6 5" marker-end="url(#ahr)"/>')
text(486, 464, "503 / 超时 → 回落最近一次快照", 10.5, 650, FAIL)
text(486, 480, "并在回答里标注「数据截至 HH:MM」", 10, 400, FAIL)

# ================= 契约
box(430, 636, 470, 58, "#fff", ACCENT, 10, 1.6, "7 5")
text(452, 660, "唯一共享物：OpenAPI 3.1 契约", 12, 700, "#8a6d2f")
text(452, 678, "mock-school-api/docs/openapi.yaml · 不共享库 / 代码 / 文件", 10, 400, MUTED)
p.append(f'<line x1="540" y1="636" x2="470" y2="608" stroke="{ACCENT}" stroke-width="1.2" stroke-dasharray="5 4"/>')
p.append(f'<line x1="790" y1="636" x2="860" y2="608" stroke="{ACCENT}" stroke-width="1.2" stroke-dasharray="5 4"/>')

# ================= 底部四步
text(24, 726, "一次调用是怎么走的", 13, 700, INK)
steps = [
    ("1", "Vue → Spring：用户要「某天 14:00–15:50 哪间房空」"),
    ("2", "适配层 → 模拟系统：先 GET /v1/meta 看 lastUpdated 与 revision"),
    ("3", "再 GET /v1/timetable?date=… 拉课表；Spring 用「课表 − 已登记占用」算空闲块"),
    ("4", "上游 503 或超时：不报错、不崩，回落快照并标注数据时间，前端照常显示"),
]
for i, (n, s) in enumerate(steps):
    y = 758 + i * 26
    badge(34, y - 4, n)
    text(56, y, s, 11.5, 500, INK)
text(24, 758 + 4 * 26 + 6, "隔离边界：两个进程 · 两个数据库 · 零代码依赖 —— 唯一的通道就是中间那条粗箭头。",
     11.5, 650, MUTED)

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <marker id="ah" markerWidth="11" markerHeight="11" refX="9" refY="5.5" orient="auto">
    <path d="M0,1 L10,5.5 L0,10 z" fill="{API}"/></marker>
  <marker id="ahr" markerWidth="11" markerHeight="11" refX="9" refY="5.5" orient="auto">
    <path d="M0,1 L10,5.5 L0,10 z" fill="{FAIL}"/></marker>
</defs>
<rect width="{W}" height="{H}" fill="#fff"/>
{chr(10).join(p)}
</svg>'''

Path("/tmp/arch-two-systems.html").write_text(
    f'<!doctype html><html><head><meta charset="utf-8"><style>html,body{{margin:0;background:#fff}}</style></head>'
    f'<body>{svg}</body></html>')
print("ok")
