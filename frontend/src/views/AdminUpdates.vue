<template>
  <div>
    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-head">
          <span>Change room use time</span>
          <span class="hint">Managers may add or release a room's use time. Every change is logged and expires.</span>
        </div>
      </template>

      <el-form label-width="150px" style="max-width: 660px">
        <el-form-item label="Room">
          <el-select v-model="form.roomId" placeholder="Choose a room" style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.code} (${r.building} / ${r.floor})`" :value="r.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="What happens">
          <el-radio-group v-model="form.changeType">
            <el-radio value="USE">Add use</el-radio>
            <el-radio value="RELEASE">Release</el-radio>
          </el-radio-group>
          <div class="subhint">
            <b>Add use</b> takes free hours away, for example an event needs the room.<br />
            <b>Release</b> gives hours back, for example a class is cancelled.
          </div>
        </el-form-item>

        <el-form-item label="Date">
          <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>

        <el-form-item label="From (hour)">
          <el-time-select v-model="form.start" start="08:00" step="01:00" end="21:00" style="width: 100%" />
        </el-form-item>
        <el-form-item label="To (hour)">
          <el-time-select v-model="form.end" start="09:00" step="01:00" end="22:00" style="width: 100%" />
        </el-form-item>

        <el-form-item label="Reason">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="Department meeting, about 25 people" />
        </el-form-item>

        <el-form-item label="Expires at">
          <el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm"
                          placeholder="Leave empty to keep until someone cancels it" style="width: 100%" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">Submit change</el-button>
          <el-button @click="loadAll">Refresh</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="My changes" name="mine">
          <el-table :data="mine" v-loading="loading">
            <el-table-column prop="roomCode" label="Room" width="120" />
            <el-table-column label="Change" width="200">
              <template #default="{ row }">{{ label(row.changeType) }}</template>
            </el-table-column>
            <el-table-column label="Date" width="120" prop="date" />
            <el-table-column label="Interval" width="140">
              <template #default="{ row }"><span class="mono">{{ row.start }} - {{ row.end }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="Reason" min-width="180" />
            <el-table-column label="State" width="110">
              <template #default="{ row }">
                <el-tag :type="row.state === 'Active' ? 'success' : 'info'" effect="light">{{ row.state }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Action" width="100">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.state !== 'Active'" @click="cancel(row)">Cancel</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="All changes from every manager" name="all">
          <el-alert type="info" :closable="false" show-icon class="mb-sm"
                    title="A manager can manage any change, not only their own. Nothing is deleted: cancelling only makes a change inactive." />
          <el-table :data="all" v-loading="loading">
            <el-table-column prop="roomCode" label="Room" width="120" />
            <el-table-column label="Change" width="200">
              <template #default="{ row }">{{ label(row.changeType) }}</template>
            </el-table-column>
            <el-table-column label="Date" width="120" prop="date" />
            <el-table-column label="Interval" width="140">
              <template #default="{ row }"><span class="mono">{{ row.start }} - {{ row.end }}</span></template>
            </el-table-column>
            <el-table-column prop="createdBy" label="Submitted by" width="130" />
            <el-table-column prop="reason" label="Reason" min-width="160" />
            <el-table-column label="State" width="110">
              <template #default="{ row }">
                <el-tag :type="row.state === 'Active' ? 'success' : 'info'" effect="light">{{ row.state }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Action" width="100">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.state !== 'Active'" @click="cancel(row)">Cancel</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="Audit log" name="audit">
          <el-table :data="auditRows" v-loading="loading">
            <el-table-column prop="createdAt" label="When" width="150" />
            <el-table-column prop="action" label="Action" width="160" />
            <el-table-column prop="actor" label="Actor" width="120" />
            <el-table-column prop="detail" label="Detail" min-width="240" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const rooms = ref([])
const mine = ref([])
const all = ref([])
const auditRows = ref([])
const tab = ref('mine')
const saving = ref(false)
const loading = ref(false)

const today = new Date().toISOString().slice(0, 10)
const form = ref({
  roomId: null, changeType: 'REQUISITION', date: today,
  start: '14:00', end: '16:00', reason: '', expiresAt: ''
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [r, m, a, lg] = await Promise.all([
      api.get('/rooms'), api.get('/updates/mine'), api.get('/updates'), api.get('/audit')
    ])
    rooms.value = r.data
    mine.value = m.data
    all.value = a.data
    auditRows.value = lg.data
    if (!form.value.roomId && r.data.length) form.value.roomId = r.data[0].id
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.value.roomId || !form.value.start || !form.value.end) {
    ElMessage.warning('Room, start and end time are required')
    return
  }
  saving.value = true
  try {
    await api.post('/updates', form.value)
    ElMessage.success('Change saved and applied')
    await loadAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to save')
  } finally {
    saving.value = false
  }
}

async function cancel(row) {
  try {
    await api.delete(`/updates/${row.id}`)
    ElMessage.success('Change cancelled, the time is released')
    await loadAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to cancel')
  }
}

function label(t) {
  return ({ USE: 'Add use', RELEASE: 'Release' })[t] || t
}
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.mb-sm { margin-bottom: 10px; }
.card-head { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 12px; }
.card-head > span:first-child { font-weight: 600; white-space: nowrap; }
.hint { color: #8A94A6; font-size: 12px; flex: 1 1 280px; }
.subhint { color: #8A94A6; font-size: 12px; line-height: 1.6; }
.mono { font-variant-numeric: tabular-nums; }
</style>
