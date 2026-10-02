<template>
  <div class="page">
    <!-- 左：全部房间（不按时间过滤） -->
    <el-card shadow="never" class="rooms-panel">
      <template #header>
        <div class="card-head">
          <span>{{ t('rooms.title') }} <b>{{ rooms.length }}</b></span>
          <span class="hint">{{ t('rooms.subtitle') }}</span>
        </div>
      </template>

      <el-input v-model="filter" :placeholder="t('rooms.filterPlaceholder')" size="small" clearable class="mb-sm" />

      <div class="room-list" v-loading="loadingRooms">
        <div v-for="r in filteredRooms" :key="r.id" class="room-item"
             :class="{ active: selected && selected.id === r.id }" @click="select(r)">
          <div class="ri-top">
            <b>{{ r.code }}</b>
            <span class="ri-type">{{ r.roomType }}</span>
          </div>
          <div class="ri-meta">
            {{ r.building }} / {{ r.floor }} &middot; {{ r.capacity }} {{ t('find.colSeats') }}
            <span v-if="summary[r.code]" class="ri-stars">&middot; ★ {{ summary[r.code].average }} ({{ summary[r.code].count }})</span>
          </div>
        </div>
        <div v-if="!filteredRooms.length" class="muted small">{{ t('rooms.noMatch') }}</div>
      </div>
    </el-card>

    <!-- 右：房间详情 -->
    <div class="main">
      <el-card v-if="selected" shadow="never" class="mb">
        <template #header>
          <div class="card-head">
            <span>{{ t('rooms.roomLabel') }} <b>{{ selected.code }}</b> &middot; {{ selected.building }} / {{ selected.floor }}</span>
            <span class="hint">{{ t('rooms.attributesHint') }}</span>
          </div>
        </template>

        <el-descriptions :column="4" border size="small">
          <el-descriptions-item :label="t('find.colSeats')">{{ selected.capacity }}</el-descriptions-item>
          <el-descriptions-item :label="t('find.colType')">{{ selected.roomType }}</el-descriptions-item>
          <el-descriptions-item :label="t('find.building')">{{ selected.building }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.floor')">{{ selected.floor }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="sec">
          {{ t('rooms.facilities') }}
          <span v-if="selected.facilitiesVerifiedAt" class="hint">
            &middot; {{ t('rooms.lastVerified', { when: selected.facilitiesVerifiedAt }) }}
          </span>
          <span v-else class="hint">&middot; <b>{{ t('rooms.sampleData') }}</b></span>
          <el-button v-if="isAdmin" link type="primary" size="small" style="margin-left: 8px" @click="toggleEdit">
            {{ editing ? t('common.cancel') : t('common.edit') }}
          </el-button>
        </h4>

        <el-descriptions v-if="!editing" :column="3" border size="small">
          <el-descriptions-item :label="t('rooms.seatsTotal')">{{ selected.capacity }}</el-descriptions-item>
          <el-descriptions-item :label="t('rooms.sockets')">
            <span v-if="selected.sockets === null || selected.sockets === undefined" class="muted">—</span>
            <span v-else>{{ selected.sockets ? t('common.yes') : t('common.no') }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('rooms.seatType')">{{ selected.seatType || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-form v-else label-width="130px" class="fac-form">
          <el-form-item :label="t('rooms.seatsTotal')">
            <el-input-number v-model="form.capacity" :min="1" :max="500" size="small" />
          </el-form-item>
          <el-form-item :label="t('rooms.sockets')">
            <el-switch v-model="form.sockets" :active-text="t('common.yes')" :inactive-text="t('common.no')" />
          </el-form-item>
          <el-form-item :label="t('rooms.seatType')">
            <el-input v-model="form.seatType" size="small" maxlength="60" style="width: 300px"
                      :placeholder="t('rooms.seatTypePlaceholder')" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" :loading="savingFacilities" @click="saveFacilities">
              {{ t('common.save') }}
            </el-button>
            <el-button size="small" @click="editing = false">{{ t('common.cancel') }}</el-button>
            <span class="hint" style="margin-left: 10px">{{ t('rooms.saveHint') }}</span>
          </el-form-item>
        </el-form>

        <h4 class="sec">
          {{ t('rooms.timetable') }}
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" size="small"
                          style="width: 150px; margin-left: 8px" @change="loadTimeline" />
        </h4>
        <el-alert v-if="timelineError" type="error" :closable="false" show-icon :title="timelineError" class="mb" />
        <div class="tl-grid" :style="gridStyle">
          <div v-for="(s, i) in (timeline ? timeline.segments : [])" :key="i"
               :class="['cell', s.type === 'BUSY' ? 'busy' : 'free']"
               :style="s.span > 1 ? { gridColumn: 'span ' + s.span } : null"
               :title="s.type === 'BUSY' ? t('rooms.inUseTip', { from: s.from, to: s.to })
                                         : t('rooms.freeTip', { from: s.from, to: s.to })">
            <span>{{ s.label }}</span>
          </div>
        </div>
        <div class="tl-axis" :style="gridStyle">
          <span v-for="(h, i) in axisHours" :key="i">{{ h }}</span>
        </div>
        <p class="tl-note">{{ tlNote }}</p>
        <div class="legend">
          <span><i class="sw busy"></i>{{ t('find.legendInUse') }}</span>
          <span><i class="sw free"></i>{{ t('find.legendFree') }}</span>
        </div>

        <!-- ------------------------------------------------ 学生评价 -->
        <h4 class="sec">
          {{ t('rooms.reviews') }}
          <span v-if="reviews" class="hint">
            &middot; {{ t('rooms.ratingFrom', { avg: reviews.average, n: reviews.count }, reviews.count) }}
          </span>
        </h4>

        <el-form label-width="130px" class="review-form">
          <el-form-item :label="t('rooms.yourRating')">
            <el-rate v-model="myRating" :max="5" show-score score-template="{value} / 5" />
          </el-form-item>
          <el-form-item :label="t('rooms.yourReview')">
            <el-input v-model="myBody" type="textarea" :rows="3" maxlength="600" show-word-limit
                      :placeholder="t('rooms.reviewPlaceholder')" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingReview" @click="submitReview">
              {{ myExistingId ? t('rooms.updateReview') : t('rooms.postReview') }}
            </el-button>
            <el-button v-if="myExistingId" @click="removeReview(myExistingId)">{{ t('rooms.deleteYours') }}</el-button>
            <span class="hint" style="margin-left: 10px">{{ t('rooms.reviewHint') }}</span>
          </el-form-item>
        </el-form>

        <div class="rev-head">
          <span class="hint">{{ t('rooms.reviewCount', reviews ? reviews.count : 0) }}</span>
          <el-radio-group v-model="reviewSort" size="small" @change="loadReviews">
            <el-radio-button value="time">{{ t('rooms.newest') }}</el-radio-button>
            <el-radio-button value="rating">{{ t('rooms.highest') }}</el-radio-button>
          </el-radio-group>
        </div>

        <el-alert v-if="reviewError" type="error" :closable="false" show-icon :title="reviewError" class="mb" />

        <div v-if="reviews && reviews.items.length">
          <div v-for="it in reviews.items" :key="it.id" class="rev-item">
            <div class="rev-top">
              <el-rate :model-value="it.rating" disabled size="small" />
              <span class="rev-meta">{{ it.author }} &middot; {{ it.updatedAt }}</span>
              <el-button v-if="it.mine" link type="danger" size="small" @click="removeReview(it.id)">
                {{ t('common.delete') }}
              </el-button>
              <el-button v-else-if="auth.isSuper" link type="danger" size="small" @click="removeReview(it.id, true)">
                {{ t('common.delete') }}
              </el-button>
            </div>
            <div class="rev-body">{{ it.body }}</div>
          </div>
        </div>
        <el-empty v-else :image-size="60" :description="t('rooms.noReviews')" />
      </el-card>

      <el-empty v-else :description="t('rooms.pickRoom')" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../stores/auth'
import { useApiError } from '../i18n/apiError'

const { t } = useI18n()
const apiError = useApiError()
const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)

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
    ElMessage.success(t('rooms.facilitiesSaved'))
  } catch (e) {
    ElMessage.error(apiError(e, 'rooms.facilitiesFailed'))
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
        const m = base + k * 60
        hours.push(String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0'))
      }
    })
    axisHours.value = hours
    const busy = segs.filter(s => s.type === 'BUSY' && s.label).map(s => s.label)
    tlNote.value = busy.length ? t('rooms.noteInUse', { ranges: busy.join(', ') }) : t('rooms.noteAllFree')
  } catch (e) {
    timelineError.value = t('rooms.timelineFailed', { status: e.response?.status || e.message })
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
    reviewError.value = t('rooms.reviewsFailed', { status: e.response?.status || e.message })
  }
}

async function submitReview() {
  if (!myRating.value) { ElMessage.warning(t('rooms.needRating')); return }
  if (!myBody.value.trim()) { ElMessage.warning(t('rooms.needBody')); return }
  savingReview.value = true
  try {
    await api.post(`/rooms/${selected.value.code}/reviews`, { rating: myRating.value, body: myBody.value })
    ElMessage.success(t('rooms.reviewSaved'))
    await refreshAll()
  } catch (e) {
    ElMessage.error(apiError(e, 'rooms.reviewFailed'))
  } finally {
    savingReview.value = false
  }
}

async function removeReview(id, others) {
  // 删自己的直接删；删别人的（管理员 / 超级管理员）先确认一下
  if (others) {
    try {
      await ElMessageBox.confirm(t('rooms.confirmDeleteOthers'), t('common.delete'), {
        confirmButtonText: t('common.delete'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      })
    } catch (e) {
      return
    }
  }
  try {
    await api.delete(`/reviews/${id}`)
    ElMessage.success(t('rooms.reviewDeleted'))
    await refreshAll()
  } catch (e) {
    ElMessage.error(apiError(e, 'rooms.reviewDeleteFailed'))
  }
}

async function refreshAll() {
  await loadReviews()
  summary.value = (await api.get('/reviews/summary')).data
}

// 语言切换后，已经拼好的提示文字要用新语言重算
watch(() => t('app.name'), () => {
  if (timeline.value) loadTimeline()
})
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.mb-sm { margin-bottom: 10px; }
.page { display: flex; gap: 14px; align-items: flex-start; }
.rooms-panel { flex: 0 0 268px; width: 268px; }
.main { flex: 1; min-width: 0; }
.room-list { max-height: 640px; overflow: auto; margin: -4px; }
.room-item { padding: 7px 8px; border-radius: 6px; cursor: pointer; }
.room-item:hover { background: #f2f6f4; }
.room-item.active { background: #e4efeb; box-shadow: inset 3px 0 0 #b49764; }
.ri-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; }
.ri-meta { color: #7d8c88; font-size: 11.5px; }
.ri-type { color: #7d8c88; font-size: 11px; }
.ri-stars { color: #E6A23C; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.hint { color: #7d8c88; font-size: 12px; }
.small { font-size: 12px; }
.muted { color: #7d8c88; }
.sec { margin: 18px 0 8px; font-size: 13px; }
.fac-form { max-width: 640px; margin-top: 4px; }
.review-form { max-width: 720px; margin-top: 4px; }
.rev-head { display: flex; justify-content: space-between; align-items: center; margin: 6px 0 10px; }
.rev-item { border-top: 1px solid #e6ebe9; padding: 10px 0; }
.rev-top { display: flex; align-items: center; gap: 10px; }
.rev-meta { color: #7d8c88; font-size: 12px; }
.rev-body { margin-top: 4px; color: #1f2d2a; font-size: 13px; line-height: 1.6; }
.tl-grid { display: grid; gap: 2px; }
.cell { height: 30px; border-radius: 3px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 10px; overflow: hidden; white-space: nowrap; min-width: 0; }
.busy { background: #9fb0aa; }
.free { background: #2f8b6d; }
/* 轴上每个整点标的是「这一格的起点」，所以标签左边缘对齐格线，不居中 */
.tl-axis { display: grid; gap: 2px; margin-top: 7px; color: #7d8c88; font-size: 9px; }
.tl-axis span { position: relative; text-align: left; white-space: nowrap; }
.tl-axis span::before { content: ''; position: absolute; left: 0; top: -6px; width: 1px; height: 4px; background: #dfe6e3; }
.tl-note { margin: 8px 0 0; color: #5c6b67; font-size: 12px; }
.legend { display: flex; gap: 18px; margin-top: 10px; color: #5c6b67; font-size: 12px; }
.sw { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 6px; vertical-align: -2px; }
</style>
