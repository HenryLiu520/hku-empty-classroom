<template>
  <div>
    <el-card shadow="never" class="mb">
      <div class="filters">
        <span class="lbl">Building</span>
        <el-select v-model="building" style="width: 200px" @change="search">
          <el-option label="All buildings" value="" />
          <el-option v-for="b in buildings" :key="b" :label="b + ', Centennial Campus'" :value="b" />
        </el-select>

        <span class="lbl">Date</span>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width: 170px" @change="search" />

        <span class="lbl">From</span>
        <el-time-select v-model="from" start="08:00" step="01:00" end="21:00" style="width: 120px" />

        <span class="lbl">To</span>
        <el-time-select v-model="to" :start="minTo" step="01:00" end="22:50" style="width: 120px" @change="search" />

        <el-button type="primary" :loading="loading" @click="search">Search</el-button>
        <el-button @click="reset">Reset</el-button>
      </div>

      <div class="filters mt-sm">
        <span class="lbl">Sort by</span>
        <el-select v-model="sortBy" style="width: 210px">
          <el-option v-for="o in sortOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>

        <el-switch v-model="showAll" active-text="Show all rooms" />
        <span class="hint" v-if="!showAll">Only rooms free for the whole interval are listed.</span>
      </div>
    </el-card>

    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-head">
          <span>
            <b>{{ result.matchedCount ?? 0 }}</b> of {{ (result.rooms || []).length }} rooms match
            <span class="hint" v-if="!showAll">&middot; listing matching rooms only</span>
            <span class="hint" v-else>&middot; listing every room</span>
          </span>
          <span class="hint">{{ sortLabel }} &middot; a free window stops 10 minutes before the next class, so there is time to pack up and leave. That is why rooms are offered until <b>:50</b>.</span>
        </div>
      </template>

      <el-table v-if="visibleRooms.length" :data="visibleRooms" v-loading="loading" @row-click="pick"
                highlight-current-row :row-class-name="rowClass">
        <el-table-column prop="room.code" label="Room" width="130">
          <template #default="{ row }"><b>{{ row.room.code }}</b></template>
        </el-table-column>
        <el-table-column label="Location" width="130">
          <template #default="{ row }">{{ row.room.building }} / {{ row.room.floor }}</template>
        </el-table-column>
        <el-table-column prop="room.capacity" label="Seats" width="80" />
        <el-table-column prop="room.roomType" label="Type" width="110" />
        <el-table-column label="Status" width="150">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Free window" min-width="190">
          <template #default="{ row }">
            <span v-if="row.matches" class="mono">{{ coveringWindow(row) }}</span>
            <span v-else-if="row.windows.length" class="mono muted">after {{ row.windows[0].start }}</span>
            <span v-else class="muted">no window long enough</span>
          </template>
        </el-table-column>
        <el-table-column label="Next class" width="120">
          <template #default="{ row }"><span class="mono">{{ row.nextBusyStart || '&mdash;' }}</span></template>
        </el-table-column>
      </el-table>

      <el-empty v-else :image-size="70"
                description="No room is free for that whole interval. Try a shorter interval, another building, or turn on 'Show all rooms'." />

      <el-alert type="warning" :closable="false" show-icon class="mt"
        title="Please note: a room must be given up if a class or a member of staff needs it, and a member of staff may ask you to leave. This page shows where rooms are free. It does not grant permission to use a room." />

      <el-alert type="info" :closable="false" show-icon class="mt"
        title="A room shown as free may already be used by other students, and rooms are shared. Free means the room is not timetabled or taken, not that it is empty." />
    </el-card>

    <el-card v-if="timeline" shadow="never">
      <template #header>
        <div class="card-head">
          <span>Room timeline: <b>{{ timeline.room }}</b>, {{ timeline.date }}</span>
          <span class="hint">Blue = class, green = free, hatched = changeover buffer</span>
        </div>
      </template>

      <div class="tl">
        <div v-for="(s, i) in timeline.segments" :key="i"
             :class="['seg', s.type === 'BUSY' ? 'busy' : s.type === 'FREE' ? 'free' : 'buf']"
             :style="{ width: s.widthPct + '%' }"
             :title="s.type + ' ' + s.start + ' - ' + s.end">
          <span v-if="s.type === 'FREE' && s.widthPct > 8">free</span>
        </div>
      </div>
      <div class="ticks">
        <span v-for="t in ticks" :key="t">{{ t }}</span>
      </div>
      <div class="legend">
        <span><i class="sw busy"></i>Class (from timetable)</span>
        <span><i class="sw free"></i>Free</span>
        <span><i class="sw buf"></i>Buffer, not offered</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import api from '../api'

const buildings = ref([])
const building = ref('')
const today = new Date().toISOString().slice(0, 10)
const date = ref(today)
const from = ref('14:00')
const to = ref('15:50')
const showAll = ref(false)
const sortBy = ref('window')

/** 排序方式：后续要加别的方式，在这里加一行即可 */
const sortOptions = [
  { value: 'window', label: 'Longest free window' },
  { value: 'code', label: 'Room code (A to Z)' },
  { value: 'building', label: 'Building, then room code' },
  { value: 'seats', label: 'Most seats first' }
]

const result = ref({})
const timeline = ref(null)
const loading = ref(false)

const ticks = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00']

/** To 的下拉只给"晚于 From"的时刻：最早是 From + 50 分钟 */
const minTo = computed(() => {
  const t = toMin(from.value) + 50
  return `${String(Math.floor(t / 60)).padStart(2, '0')}:50`
})

const sortLabel = computed(() => (sortOptions.find((o) => o.value === sortBy.value) || {}).label || '')

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
}

function coveringWindow(row) {
  const f = toMin(from.value)
  const span = toMin(to.value) - f
  const w = row.windows.find((x) => toMin(x.start) <= f && toMin(x.end) >= f + span)
  return w ? `${w.start} - ${w.end}` : (row.windows[0] ? `${row.windows[0].start} - ${row.windows[0].end}` : '')
}
function toMin(hhmm) { return parseInt(hhmm.slice(0, 2)) * 60 + parseInt(hhmm.slice(3)) }

function statusText(s) {
  return ({ AVAILABLE: 'Available', AVAILABLE_LATER: 'Free later', IN_USE: 'In use',
            NONE: 'No window' })[s] || s
}
function tagType(s) {
  return ({ AVAILABLE: 'success', AVAILABLE_LATER: 'primary', IN_USE: 'warning',
            NONE: 'info' })[s] || 'info'
}
function rowClass({ row }) { return row.matches ? 'matched-row' : '' }
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.lbl { color: #606266; font-size: 13px; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.hint { color: #8A94A6; font-size: 12px; }
.mono { font-variant-numeric: tabular-nums; }
.muted { color: #909399; }
.tl { display: flex; height: 28px; border: 1px solid #EBEEF5; border-radius: 4px; overflow: hidden; }
.seg { height: 100%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 11px; }
.busy { background: #5B8FF9; }
.free { background: #7BC96F; }
.buf { background: repeating-linear-gradient(45deg, #E4E7ED, #E4E7ED 4px, #F5F7FA 4px, #F5F7FA 8px); }
.ticks { display: flex; justify-content: space-between; color: #8A94A6; font-size: 11px; margin-top: 5px; }
.legend { display: flex; gap: 18px; margin-top: 10px; color: #606266; font-size: 12px; }
.sw { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 6px; vertical-align: -2px; }
.mt { margin-top: 12px; }
.mt-sm { margin-top: 10px; }
:deep(.matched-row) { background: #F6FFED; }
</style>
