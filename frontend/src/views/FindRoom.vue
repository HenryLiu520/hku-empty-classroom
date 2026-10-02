<template>
  <div>
    <el-card shadow="never" class="mb">
      <div class="filters">
        <span class="lbl">{{ t('find.building') }}</span>
        <el-select v-model="building" style="width: 200px" @change="search">
          <el-option :label="t('find.allBuildings')" value="" />
          <el-option v-for="b in buildings" :key="b" :label="b" :value="b" />
        </el-select>

        <span class="lbl">{{ t('common.date') }}</span>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width: 170px" @change="search" />

        <span class="lbl">{{ t('find.from') }}</span>
        <el-time-select v-model="from" start="08:00" step="01:00" end="21:00" style="width: 120px" />

        <span class="lbl">{{ t('find.to') }}</span>
        <el-time-select v-model="to" :start="minTo" step="01:00" end="22:50" style="width: 120px" @change="search" />

        <el-button type="primary" :loading="loading" @click="search">{{ t('find.search') }}</el-button>
        <el-button @click="reset">{{ t('find.reset') }}</el-button>
      </div>

      <div class="filters mt-sm">
        <span class="lbl">{{ t('find.sortBy') }}</span>
        <el-select v-model="sortBy" style="width: 230px">
          <el-option v-for="o in sortOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>

        <el-switch v-model="showAll" :active-text="t('find.showAll')" />
        <span class="hint" v-if="!showAll">{{ t('find.onlyFree') }}</span>
      </div>
    </el-card>

    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-head">
          <span>
            {{ t('find.matched', { matched: result.matchedCount ?? 0, total: (result.rooms || []).length }) }}
            <span class="hint" v-if="!showAll">&middot; {{ t('find.listingMatching') }}</span>
            <span class="hint" v-else>&middot; {{ t('find.listingEvery') }}</span>
          </span>
          <span class="hint">{{ sortLabel }} &middot; {{ t('find.freeStops') }}</span>
        </div>
      </template>

      <el-table v-if="visibleRooms.length" :data="visibleRooms" v-loading="loading" @row-click="pick"
                highlight-current-row :row-class-name="rowClass">
        <el-table-column prop="room.code" :label="t('find.colRoom')" width="130">
          <template #default="{ row }"><b>{{ row.room.code }}</b></template>
        </el-table-column>
        <el-table-column :label="t('find.colLocation')" width="130">
          <template #default="{ row }">{{ row.room.building }} / {{ row.room.floor }}</template>
        </el-table-column>
        <el-table-column prop="room.capacity" :label="t('find.colSeats')" width="80" />
        <el-table-column prop="room.roomType" :label="t('find.colType')" width="110" />
        <el-table-column :label="t('find.colStatus')" width="150">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('find.colWindow')" min-width="190">
          <template #default="{ row }">
            <span v-if="row.matches" class="mono">{{ coveringWindow(row) }}</span>
            <span v-else-if="row.windows.length" class="mono muted">
              {{ t('find.after', { time: row.windows[0].start }) }}
            </span>
            <span v-else class="muted">{{ t('find.noWindowLong') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('find.colNext')" width="120">
          <template #default="{ row }"><span class="mono">{{ row.nextBusyStart || '—' }}</span></template>
        </el-table-column>
      </el-table>

      <el-empty v-else :image-size="70" :description="t('find.empty')" />

      <el-alert type="warning" :closable="false" show-icon class="mt" :title="t('find.noticePermission')" />

      <el-alert type="info" :closable="false" show-icon class="mt" :title="t('find.noticeShared')" />
    </el-card>

    <el-card v-if="timeline" shadow="never">
      <template #header>
        <div class="card-head">
          <span>{{ t('find.timelineTitle', { room: timeline.room, date: timeline.date }) }}</span>
          <span class="hint">{{ t('find.noTimelineHint') }}</span>
        </div>
      </template>

      <div class="tl-grid" :style="gridStyle">
        <div v-for="(s, i) in timeline.segments" :key="i"
             :class="['cell', s.type === 'BUSY' ? 'busy' : 'free']"
             :style="s.span > 1 ? { gridColumn: 'span ' + s.span } : null"
             :title="(s.type === 'BUSY' ? t('rooms.inUseTip', { from: s.from, to: s.to })
                                        : t('rooms.freeTip', { from: s.from, to: s.to }))">
          <span>{{ s.label }}</span>
        </div>
      </div>
      <div class="tl-axis" :style="gridStyle">
        <span v-for="(h, i) in axisHours" :key="i">{{ h }}</span>
      </div>
      <div class="legend">
        <span><i class="sw busy"></i>{{ t('find.legendInUse') }}</span>
        <span><i class="sw free"></i>{{ t('find.legendFree') }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '../api'

const { t } = useI18n()

const buildings = ref([])
const building = ref('')
const today = new Date().toISOString().slice(0, 10)
const date = ref(today)
const from = ref('14:00')
const to = ref('15:50')
const showAll = ref(false)
const sortBy = ref('window')

/** 排序方式：后续要加别的方式，在这里加一行即可 */
const sortOptions = computed(() => [
  { value: 'window', label: t('find.sortWindow') },
  { value: 'code', label: t('find.sortCode') },
  { value: 'building', label: t('find.sortBuilding') },
  { value: 'seats', label: t('find.sortSeats') }
])

const result = ref({})
const timeline = ref(null)
const loading = ref(false)
const gridStyle = ref('')
const axisHours = ref([])

/** To 的下拉只给"晚于 From"的时刻：最早是 From + 50 分钟 */
const minTo = computed(() => {
  const m = toMin(from.value) + 50
  return `${String(Math.floor(m / 60)).padStart(2, '0')}:50`
})

const sortLabel = computed(() => (sortOptions.value.find((o) => o.value === sortBy.value) || {}).label || '')

/** 列表内容：默认只列符合时间段要求的房间，排序方式可选 */
const visibleRooms = computed(() => {
  const rows = (result.value.rooms || []).filter((r) => showAll.value || r.matches)
  const by = {
    code: (a, b) => a.room.code.localeCompare(b.room.code),
    building: (a, b) => (a.room.building + ' ' + a.room.code).localeCompare(b.room.building + ' ' + b.room.code),
    seats: (a, b) => b.room.capacity - a.room.capacity,
    window: (a, b) => longestWindow(b) - longestWindow(a)
  }
  return [...rows].sort(by[sortBy.value] || by.window)
})

function longestWindow(row) {
  return (row.windows || []).reduce((m, w) => Math.max(m, w.minutes), 0)
}

/** From 一变就保证 To 仍在其之后，然后重新查询 */
watch(from, () => {
  if (toMin(to.value) <= toMin(from.value)) to.value = minTo.value
  search()
})

onMounted(async () => {
  const { data } = await api.get('/buildings')
  buildings.value = data
  await search()
})

async function search() {
  loading.value = true
  try {
    const { data } = await api.get('/availability', {
      params: { date: date.value, building: building.value, from: from.value, to: to.value }
    })
    result.value = data
    timeline.value = null
  } finally {
    loading.value = false
  }
}

function reset() {
  building.value = ''
  date.value = today
  from.value = '14:00'
  to.value = '15:50'
  showAll.value = false
  sortBy.value = 'window'
  search()
}

async function pick(row) {
  const { data } = await api.get(`/rooms/${row.room.code}/timeline`, { params: { date: date.value } })
  timeline.value = data
  // 与「所有教室」页同一套方格：连续占用合并成一整块，轴上每个整点标在格线上
  const segs = data.segments || []
  const totalHours = segs.reduce((n, s) => n + (s.span || 1), 0)
  gridStyle.value = 'grid-template-columns: repeat(' + Math.max(1, totalHours) + ', 1fr)'
  const hours = []
  segs.forEach(s => {
    const base = parseInt(s.hour.slice(0, 2), 10) * 60 + parseInt(s.hour.slice(3), 10)
    for (let k = 0; k < (s.span || 1); k++) {
      const m = base + k * 60
      hours.push(String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0'))
    }
  })
  axisHours.value = hours
}

function coveringWindow(row) {
  const f = toMin(from.value)
  const span = toMin(to.value) - f
  const w = row.windows.find((x) => toMin(x.start) <= f && toMin(x.end) >= f + span)
  return w ? `${w.start} - ${w.end}` : (row.windows[0] ? `${row.windows[0].start} - ${row.windows[0].end}` : '')
}
function toMin(hhmm) { return parseInt(hhmm.slice(0, 2)) * 60 + parseInt(hhmm.slice(3)) }

function statusText(s) {
  return ({
    AVAILABLE: t('find.statusAvailable'), AVAILABLE_LATER: t('find.statusLater'),
    IN_USE: t('find.statusInUse'), NONE: t('find.statusNone')
  })[s] || s
}
function tagType(s) {
  return ({ AVAILABLE: 'success', AVAILABLE_LATER: 'primary', IN_USE: 'warning', NONE: 'info' })[s] || 'info'
}
function rowClass({ row }) { return row.matches ? 'matched-row' : '' }
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.lbl { color: #5c6b67; font-size: 13px; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.hint { color: #7d8c88; font-size: 12px; }
.mono { font-variant-numeric: tabular-nums; }
.muted { color: #7d8c88; }
.tl-grid { display: grid; gap: 2px; }
.cell { height: 30px; border-radius: 3px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 10px; overflow: hidden; white-space: nowrap; min-width: 0; }
.busy { background: #9fb0aa; }
.free { background: #2f8b6d; }
.tl-axis { display: grid; gap: 2px; margin-top: 7px; color: #7d8c88; font-size: 9px; }
.tl-axis span { position: relative; text-align: left; white-space: nowrap; }
.tl-axis span::before { content: ''; position: absolute; left: 0; top: -6px; width: 1px; height: 4px; background: #dfe6e3; }
.legend { display: flex; gap: 18px; margin-top: 10px; color: #5c6b67; font-size: 12px; }
.sw { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 6px; vertical-align: -2px; }
.mt { margin-top: 12px; }
.mt-sm { margin-top: 10px; }
:deep(.matched-row) { background: #f2f8f5; }
</style>
