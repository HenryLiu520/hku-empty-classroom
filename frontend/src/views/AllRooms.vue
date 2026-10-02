<template>
  <div class="page">
    <!-- 左：全部房间（不按时间过滤） -->
    <el-card shadow="never" class="rooms-panel">
      <template #header>
        <div class="card-head">
          <span>All rooms <b>{{ rooms.length }}</b></span>
          <span class="hint">every room, not filtered by time</span>
        </div>
      </template>

      <el-input v-model="filter" placeholder="Filter room code or building" size="small" clearable class="mb-sm" />

      <div class="room-list" v-loading="loadingRooms">
        <div v-for="r in filteredRooms" :key="r.id" class="room-item"
             :class="{ active: selected && selected.id === r.id }" @click="select(r)">
          <div class="ri-top">
            <b>{{ r.code }}</b>
            <span class="ri-type">{{ r.roomType }}</span>
          </div>
          <div class="ri-meta">
            {{ r.building }} / {{ r.floor }} &middot; {{ r.capacity }} seats
            <span v-if="summary[r.code]" class="ri-stars">&middot; ★ {{ summary[r.code].average }} ({{ summary[r.code].count }})</span>
          </div>
        </div>
        <div v-if="!filteredRooms.length" class="muted small">No room matches that filter.</div>
      </div>
    </el-card>

    <!-- 右：房间详情 -->
    <div class="main">
      <el-card v-if="selected" shadow="never" class="mb">
        <template #header>
          <div class="card-head">
            <span>Room <b>{{ selected.code }}</b> &middot; {{ selected.building }} / {{ selected.floor }}</span>
            <span class="hint">attributes come from the room record; the timetable is computed from the timetable plus reported changes</span>
          </div>
        </template>

        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="Seats">{{ selected.capacity }}</el-descriptions-item>
          <el-descriptions-item label="Type">{{ selected.roomType }}</el-descriptions-item>
          <el-descriptions-item label="Building">{{ selected.building }}</el-descriptions-item>
          <el-descriptions-item label="Floor">{{ selected.floor }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="sec">
          Facilities
          <span v-if="selected.facilitiesVerifiedAt" class="hint">&middot; last verified {{ selected.facilitiesVerifiedAt }}</span>
          <span v-else class="hint">&middot; <b>sample data, not verified yet</b></span>
          <el-button v-if="isAdmin" link type="primary" size="small" style="margin-left: 8px" @click="toggleEdit">
            {{ editing ? 'Cancel' : 'Edit' }}
          </el-button>
        </h4>

        <el-descriptions v-if="!editing" :column="3" border size="small">
          <el-descriptions-item label="Seats total">{{ selected.capacity }}</el-descriptions-item>
          <el-descriptions-item label="Sockets">
            <span v-if="selected.sockets === null || selected.sockets === undefined" class="muted">—</span>
            <span v-else>{{ selected.sockets ? 'Yes' : 'No' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="Seat type">{{ selected.seatType || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-form v-else label-width="110px" class="fac-form">
          <el-form-item label="Seats total">
            <el-input-number v-model="form.capacity" :min="1" :max="500" size="small" />
          </el-form-item>
          <el-form-item label="Sockets">
            <el-switch v-model="form.sockets" active-text="Yes" inactive-text="No" />
          </el-form-item>
          <el-form-item label="Seat type">
            <el-input v-model="form.seatType" size="small" maxlength="60" style="width: 300px"
                      placeholder="Fixed rows / Long shared table / Individual desks" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" :loading="savingFacilities" @click="saveFacilities">Save</el-button>
            <el-button size="small" @click="editing = false">Cancel</el-button>
            <span class="hint" style="margin-left: 10px">Saving stamps today as the verified date and is recorded in the audit log.</span>
          </el-form-item>
        </el-form>

        <h4 class="sec">
          Timetable
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" size="small"
                          style="width: 150px; margin-left: 8px" @change="loadTimeline" />
        </h4>
        <el-alert v-if="timelineError" type="error" :closable="false" show-icon :title="timelineError" class="mb" />
        <div class="tl-grid" :style="gridStyle">
          <div v-for="(s, i) in (timeline ? timeline.segments : [])" :key="i"
               :class="['cell', s.type === 'BUSY' ? 'busy' : 'free']"
               :style="s.span > 1 ? { gridColumn: 'span ' + s.span } : null"
               :title="(s.type === 'BUSY' ? 'In use ' : 'Free ') + s.from + ' – ' + s.to">
            <span>{{ s.label }}</span>
          </div>
        </div>
        <div class="tl-axis" :style="gridStyle">
          <span v-for="(h, i) in axisHours" :key="i">{{ h }}</span>
        </div>
        <p class="tl-note">{{ tlNote }}</p>
        <div class="legend">
          <span><i class="sw busy"></i>In use (class or posted change, whole hours)</span>
          <span><i class="sw free"></i>Free</span>
        </div>

        <!-- ------------------------------------------------ 学生评价 -->
        <h4 class="sec">
          Student reviews
          <span v-if="reviews" class="hint">
            &middot; <b>★ {{ reviews.average }}</b> from {{ reviews.count }} review{{ reviews.count === 1 ? '' : 's' }}
          </span>
        </h4>

        <el-form label-width="90px" class="review-form">
          <el-form-item label="Your rating">
            <el-rate v-model="myRating" :max="5" show-score score-template="{value} / 5" />
          </el-form-item>
          <el-form-item label="Your review">
            <el-input v-model="myBody" type="textarea" :rows="3" maxlength="600" show-word-limit
                      placeholder="What is it like to study here? Noise, sockets, seating, light…" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingReview" @click="submitReview">
              {{ myExistingId ? 'Update your review' : 'Post your review' }}
            </el-button>
            <el-button v-if="myExistingId" @click="removeReview(myExistingId)">Delete yours</el-button>
            <span class="hint" style="margin-left: 10px">One review per person per room. Please review the room, not the people in it.</span>
          </el-form-item>
        </el-form>

        <div class="rev-head">
          <span class="hint">{{ reviews ? reviews.count : 0 }} review{{ reviews && reviews.count === 1 ? '' : 's' }}</span>
          <el-radio-group v-model="reviewSort" size="small" @change="loadReviews">
            <el-radio-button value="time">Newest first</el-radio-button>
            <el-radio-button value="rating">Highest rated</el-radio-button>
          </el-radio-group>
        </div>

        <el-alert v-if="reviewError" type="error" :closable="false" show-icon :title="reviewError" class="mb" />

        <div v-if="reviews && reviews.items.length">
          <div v-for="it in reviews.items" :key="it.id" class="rev-item">
            <div class="rev-top">
              <el-rate :model-value="it.rating" disabled size="small" />
              <span class="rev-meta">{{ it.author }} &middot; {{ it.updatedAt }}</span>
              <el-button v-if="it.mine" link type="danger" size="small" @click="removeReview(it.id)">Delete</el-button>
            </div>
            <div class="rev-body">{{ it.body }}</div>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="No reviews yet. Be the first to describe this room." />
      </el-card>

      <el-empty v-else description="Pick a room on the left to see its details." />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const isAdmin = computed(() => auth.role === 'admin')

const editing = ref(false)
const savingFacilities = ref(false)
const form = ref({ capacity: 0, sockets: true, seatType: '' })

const rooms = ref([])
const filter = ref('')
const selected = ref(null)
const loadingRooms = ref(false)
const summary = ref({})

const today = new Date().toISOString().slice(0, 10)
const date = ref(today)
const timeline = ref(null)
const gridStyle = ref('')
const tlNote = ref('')
const axisHours = ref([])
const timelineError = ref('')
const reviews = ref(null)
const reviewSort = ref('time')
const reviewError = ref('')
const myRating = ref(0)
const myBody = ref('')
const myExistingId = ref(null)
const savingReview = ref(false)

/** 按房号字母序；过滤只作用于列表，不改变选中项 */
const filteredRooms = computed(() => {
  const q = filter.value.trim().toLowerCase()
  const list = [...rooms.value].sort((a, b) => a.code.localeCompare(b.code))
  return q ? list.filter((r) => r.code.toLowerCase().includes(q) || r.building.toLowerCase().includes(q)) : list
})

onMounted(async () => {
  loadingRooms.value = true
  try {
    const [{ data: rs }, { data: sm }] = await Promise.all([api.get('/rooms'), api.get('/reviews/summary')])
    rooms.value = rs
    summary.value = sm
    if (rs.length) select(rs.find((r) => r.code === 'CPD-LG.01') || rs[0])
  } finally {
    loadingRooms.value = false
  }
})

async function select(r) {
  selected.value = r
  editing.value = false
  await Promise.all([loadTimeline(), loadReviews()])
}

function toggleEdit() {
  if (!editing.value) {
    form.value = {
      capacity: selected.value.capacity,
      sockets: selected.value.sockets !== false,
      seatType: selected.value.seatType || ''
    }
  }
  editing.value = !editing.value
}

async function saveFacilities() {
  savingFacilities.value = true
  try {
    const { data } = await api.patch(`/rooms/${selected.value.code}`, form.value)
    // 同步右侧详情与左侧列表里的这条记录
    selected.value = data
    const i = rooms.value.findIndex((r) => r.code === data.code)
    if (i >= 0) rooms.value[i] = data
    editing.value = false
    ElMessage.success('Facilities saved, verified date stamped')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Could not save the facilities')
  } finally {
    savingFacilities.value = false
  }
}

async function loadTimeline() {
  if (!selected.value) return
  timeline.value = null
  timelineError.value = ''
  try {
    const { data } = await api.get(`/rooms/${selected.value.code}/timeline`, { params: { date: date.value } })
    timeline.value = data
    // 占用以整块为单位：连续的整点块合成一格（span 格宽），标签写真实时间（13:00–14:50）
    const segs = data.segments || []
    const totalHours = segs.reduce((n, s) => n + (s.span || 1), 0)
    gridStyle.value = 'grid-template-columns: repeat(' + Math.max(1, totalHours) + ', 1fr)'
    const hours = []
    segs.forEach(s => {
      const base = parseInt(s.hour.slice(0, 2), 10) * 60 + parseInt(s.hour.slice(3), 10)
      for (let k = 0; k < (s.span || 1); k++) {
        const t = base + k * 60
        hours.push(String(Math.floor(t / 60)).padStart(2, '0') + ':' + String(t % 60).padStart(2, '0'))
      }
    })
    axisHours.value = hours
    const busy = segs.filter(s => s.type === 'BUSY' && s.label).map(s => s.label)
    tlNote.value = busy.length
      ? 'In use ' + busy.join(', ')
      : 'No classes and no posted changes on this day — free all day.'
  } catch (e) {
    timelineError.value = 'Could not load the timetable for this room: ' + (e.response?.status || e.message)
  }
}

async function loadReviews() {
  if (!selected.value) return
  reviewError.value = ''
  try {
    const { data } = await api.get(`/rooms/${selected.value.code}/reviews`, { params: { sort: reviewSort.value } })
    reviews.value = data
    const mine = data.items.find((i) => i.mine)
    myExistingId.value = mine ? mine.id : null
    myRating.value = mine ? mine.rating : 0
    myBody.value = mine ? mine.body : ''
  } catch (e) {
    reviewError.value = 'Could not load reviews: ' + (e.response?.status || e.message)
  }
}

async function submitReview() {
  if (!myRating.value) { ElMessage.warning('Please give a rating from 1 to 5 stars'); return }
  if (!myBody.value.trim()) { ElMessage.warning('Please write a few words as well'); return }
  savingReview.value = true
  try {
    await api.post(`/rooms/${selected.value.code}/reviews`, { rating: myRating.value, body: myBody.value })
    ElMessage.success('Review saved')
    await refreshAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Could not save the review')
  } finally {
    savingReview.value = false
  }
}

async function removeReview(id) {
  try {
    await api.delete(`/reviews/${id}`)
    ElMessage.success('Review deleted')
    await refreshAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Could not delete the review')
  }
}

async function refreshAll() {
  await loadReviews()
  summary.value = (await api.get('/reviews/summary')).data
}
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.mb-sm { margin-bottom: 10px; }
.page { display: flex; gap: 14px; align-items: flex-start; }
.rooms-panel { flex: 0 0 268px; width: 268px; }
.main { flex: 1; min-width: 0; }
.room-list { max-height: 640px; overflow: auto; margin: -4px; }
.room-item { padding: 7px 8px; border-radius: 6px; cursor: pointer; }
.room-item:hover { background: #F5F7FA; }
.room-item.active { background: #ECF5FF; }
.ri-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; }
.ri-meta { color: #8A94A6; font-size: 11.5px; }
.ri-type { color: #909399; font-size: 11px; }
.ri-stars { color: #E6A23C; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.hint { color: #8A94A6; font-size: 12px; }
.small { font-size: 12px; }
.muted { color: #909399; }
.sec { margin: 18px 0 8px; font-size: 13px; }
.fac-form { max-width: 640px; margin-top: 4px; }
.review-form { max-width: 720px; margin-top: 4px; }
.rev-head { display: flex; justify-content: space-between; align-items: center; margin: 6px 0 10px; }
.rev-item { border-top: 1px solid #F0F2F5; padding: 10px 0; }
.rev-top { display: flex; align-items: center; gap: 10px; }
.rev-meta { color: #8A94A6; font-size: 12px; }
.rev-body { margin-top: 4px; color: #303133; font-size: 13px; line-height: 1.6; }
.tl-grid { display: grid; gap: 2px; }
.cell { height: 30px; border-radius: 3px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 10px; overflow: hidden; white-space: nowrap; }
.busy { background: #5B8FF9; }
.free { background: #7BC96F; }
.buf { background: repeating-linear-gradient(45deg, #E4E7ED, #E4E7ED 4px, #F5F7FA 4px, #F5F7FA 8px); color: #8A94A6; }
.cell.busy, .cell.free { min-width: 0; }
.tl-axis { display: grid; gap: 2px; margin-top: 5px; color: #8A94A6; font-size: 9px; }
.tl-axis span { text-align: center; overflow: hidden; white-space: nowrap; }
.tl-note { margin: 8px 0 0; color: #606266; font-size: 12px; }
.legend { display: flex; gap: 18px; margin-top: 10px; color: #606266; font-size: 12px; }
.sw { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 6px; vertical-align: -2px; }
</style>
