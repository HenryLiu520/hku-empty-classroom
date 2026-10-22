# mock-school-api — 模拟的学校课表接口（test double）

> ⚠️ 这是**我们自己写的模拟上游**，不是香港大学的真实接口。报告里必须写成
> *a simulated feed / stub service*，不得写成 "the University's API"。

## 它为什么存在

我们的系统要读学校的课表（Plan §3.1：课表是**输入**，只读），但读取权限还没拿到
（Plan §5.1 的 Plan B）。而最难测的恰恰是**这条边界**：

- 上游的数据是 2 小时前的，我们的回答还准吗？界面有没有说实话？
- 上游要 3 秒才回，会不会把我们的请求拖死？
- 上游直接挂了，我们是报错还是降级？

没有上游就没法回答这三问。所以我们做一个**独立进程**，实现预期的只读契约，
再加三个只有测试替身才有的开关。

## 接口（只读，需要 `X-Api-Key`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/v1/meta` | provider / term / version / **lastUpdated** / rooms / classSlots |
| GET | `/v1/rooms[?building=]` | 教室清单（code / building / floor / capacity / sockets / seatType） |
| GET | `/v1/timetable?date=YYYY-MM-DD[&building=]` | 该日全部课时（按星期几映射周课表，周一 = 1） |
| GET | `/v1/timetable/{roomCode}?date=YYYY-MM-DD` | 单间房的当日课时 |

`lastUpdated` 是**消费端判断数据新鲜度的唯一依据** —— 这正是 Plan §3.1 要求的
"the answer has to state how fresh the data behind it is"。

## 控制开关（真接口不会有；不要求 API key，只绑 127.0.0.1）

| 方法 | 路径 | 效果 |
|---|---|---|
| POST | `/v1/_control/stale?minutes=90` | 把 `lastUpdated` 往前提 90 分钟 |
| POST | `/v1/_control/slow?ms=800` | 每个只读响应先睡 800 ms |
| POST | `/v1/_control/down[?value=true]` | 只读端点直接 503 |
| POST | `/v1/_control/reset` | 全部复位 |
| GET | `/v1/_control/state` | 当前开关状态 |

## 跑起来

```bash
scripts/mock-school.sh start      # 首次会先 mvn package（约 30 s）
scripts/mock-school.sh status
scripts/verify-mock.sh            # 8 步证据：契约 + 401 + 旧数据 + 慢响应 + 503 + 复位
scripts/mock-school.sh stop
```

默认端口 **9000**，默认 API key **`hku-demo-key`**（可用 `MOCK_API_KEY` 覆盖）。

## 数据来源

`src/main/resources/seed.json` 是从项目数据库**导出**的（10 间房 / 79 条课时），不是手写的。
导出脚本见提交说明；改数据就重新导出一次，不要手工编辑这个文件。

## 它没有做什么（报告里可以写成 limitations）

- 不做写入：真实接口我们也只会读。
- 不做分页/增量同步：原型数据量小，全量返回就够。
- 不做认证令牌的签发/续期：只校验一个固定的 key，够用来测"有没有权限"这一层。
- 不模拟"部分数据缺失"：下一步可以加一个 `/_control/partial` 开关。
