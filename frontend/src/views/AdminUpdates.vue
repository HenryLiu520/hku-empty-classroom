<template>
  <div>
    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-head">
          <span>{{ t('nav.manage') }}</span>
          <span class="hint">{{ t('manage.subtitle') }}</span>
        </div>
      </template>

      <el-form label-width="150px" style="max-width: 660px">
        <el-form-item :label="t('manage.room')">
          <el-select v-model="form.roomId" :placeholder="t('manage.chooseRoom')" style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :value="r.id"
                       :label="`${r.code} (${r.building} / ${r.floor})`" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('manage.whatHappens')">
          <el-radio-group v-model="form.changeType">
            <el-radio value="USE">{{ t('manage.addUse') }}</el-radio>
            <el-radio value="RELEASE">{{ t('manage.release') }}</el-radio>
          </el-radio-group>
          <div class="subhint">
            <b>{{ t('manage.addUse') }}</b> {{ t('manage.addUseHint') }}<br />
            <b>{{ t('manage.release') }}</b> {{ t('manage.releaseHint') }}
          </div>
        </el-form-item>

        <el-form-item :label="t('manage.date')">
          <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="t('manage.fromHour')">
          <el-time-select v-model="form.start" start="08:00" step="01:00" end="21:00" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('manage.toHour')">
          <el-time-select v-model="form.end" start="08:50" step="01:00" end="21:50" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="t('manage.reason')">
          <el-input v-model="form.reason" type="textarea" :rows="2"
                    :placeholder="t('manage.reasonPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('manage.expiresAt')">
          <el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm"
                          :placeholder="t('manage.expiresPlaceholder')" style="width: 100%" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">{{ t('manage.submit') }}</el-button>
          <el-button @click="loadAll">{{ t('common.refresh') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane :label="t('manage.tabMine')" name="mine">
          <el-table :data="mine" v-loading="loading">
            <el-table-column prop="roomCode" :label="t('manage.room')" width="120" />
            <el-table-column :label="t('manage.colChange')" width="200">
              <template #default="{ row }">{{ label(row.changeType) }}</template>
            </el-table-column>
            <el-table-column :label="t('manage.date')" width="120" prop="date" />
            <el-table-column :label="t('manage.colInterval')" width="140">
              <template #default="{ row }"><span class="mono">{{ row.start }} - {{ row.end }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" :label="t('manage.reason')" min-width="180" />
            <el-table-column :label="t('manage.colState')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.state === 'Active' ? 'success' : 'info'" effect="light">
                  {{ stateText(row.state) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('manage.colAction')" width="110">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.state !== 'Active'" @click="cancel(row)">
                  {{ t('manage.cancelAction') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('manage.tabAll')" name="all">
          <el-alert type="info" :closable="false" show-icon class="mb-sm" :title="t('manage.allManagersHint')" />
          <el-table :data="all" v-loading="loading">
            <el-table-column prop="roomCode" :label="t('manage.room')" width="120" />
            <el-table-column :label="t('manage.colChange')" width="200">
              <template #default="{ row }">{{ label(row.changeType) }}</template>
            </el-table-column>
            <el-table-column :label="t('manage.date')" width="120" prop="date" />
            <el-table-column :label="t('manage.colInterval')" width="140">
              <template #default="{ row }"><span class="mono">{{ row.start }} - {{ row.end }}</span></template>
            </el-table-column>
            <el-table-column prop="createdBy" :label="t('manage.colSubmittedBy')" width="130" />
            <el-table-column prop="reason" :label="t('manage.reason')" min-width="160" />
            <el-table-column :label="t('manage.colState')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.state === 'Active' ? 'success' : 'info'" effect="light">
                  {{ stateText(row.state) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('manage.colAction')" width="110">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.state !== 'Active'" @click="cancel(row)">
                  {{ t('manage.cancelAction') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('manage.tabAudit')" name="audit">
          <el-table :data="auditRows" v-loading="loading">
            <el-table-column prop="createdAt" :label="t('manage.colWhen')" width="150" />
            <el-table-column prop="action" :label="t('manage.colAction')" width="170" />
            <el-table-column prop="actor" :label="t('manage.colActor')" width="120" />
            <el-table-column prop="detail" :label="t('manage.colDetail')" min-width="240" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useApiError } from '../i18n/apiError'

const { t } = useI18n()
const apiError = useApiError()

const rooms = ref([])
const mine = ref([])
const all = ref([])
const auditRows = ref([])
const tab = ref('mine')
const saving = ref(false)
const loading = ref(false)

const today = new Date().toISOString().slice(0, 10)
const form = ref({
  roomId: null, changeType: 'USE', date: today,
  start: '14:00', end: '15:50', reason: '', expiresAt: ''
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  // 房间列表单独取：它是这个页面的关键控件，不能被别的请求拖垮
  try {
    const r = await api.get('/rooms')
    rooms.value = r.data
    if (!form.value.roomId && r.data.length) form.value.roomId = r.data[0].id
  } catch (e) {
    ElMessage.error(t('manage.roomListFailed', { status: e.response?.status || e.message }))
  }
  try {
    const [m, a, lg] = await Promise.all([
      api.get('/updates/mine'), api.get('/updates'), api.get('/audit')
    ])
    mine.value = m.data
    all.value = a.data
    auditRows.value = lg.data
  } catch (e) {
    ElMessage.error(apiError(e, 'manage.sessionExpired'))
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.value.roomId || !form.value.start || !form.value.end) {
    ElMessage.warning(t('manage.needFields'))
    return
  }
  saving.value = true
  try {
    await api.post('/updates', form.value)
    ElMessage.success(t('manage.saved'))
    await loadAll()
  } catch (e) {
    ElMessage.error(apiError(e, 'manage.failed'))
  } finally {
    saving.value = false
  }
}

async function cancel(row) {
  try {
    await api.delete(`/updates/${row.id}`)
    ElMessage.success(t('manage.cancelled'))
    await loadAll()
  } catch (e) {
    ElMessage.error(apiError(e, 'manage.failedCancel'))
  }
}

function label(type) {
  return ({ USE: t('manage.addUse'), RELEASE: t('manage.release') })[type] || type
}

function stateText(state) {
  return ({ Active: t('manage.stateActive'), Expired: t('manage.stateExpired') })[state] || state
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
