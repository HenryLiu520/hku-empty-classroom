<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>{{ t('accounts.title') }}</span>
          <span class="hint">{{ t('accounts.subtitle') }}</span>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="mb-sm" :title="t('accounts.roleHint')" />

      <div class="toolbar">
        <el-button type="primary" @click="openCreate">{{ t('accounts.newAccount') }}</el-button>
        <el-button @click="load">{{ t('common.refresh') }}</el-button>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" :label="t('accounts.username')" min-width="140" />
        <el-table-column prop="displayName" :label="t('accounts.displayName')" min-width="150" />
        <el-table-column prop="email" :label="t('accounts.email')" min-width="200">
          <template #default="{ row }"><span class="muted">{{ row.email || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('accounts.role')" width="150">
          <template #default="{ row }">
            <el-tag :type="tagType(row.role)" effect="light">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('accounts.actions')" width="290">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('accounts.changeRole') }}</el-button>
            <el-button link type="primary" @click="openPassword(row)">{{ t('accounts.resetPassword') }}</el-button>
            <el-button link type="danger" :disabled="row.username === auth.username" @click="remove(row)">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建账户 -->
    <el-dialog v-model="createOpen" :title="t('accounts.newAccount')" width="460px">
      <el-form label-width="130px">
        <el-form-item :label="t('accounts.username')">
          <el-input v-model="create.username" />
        </el-form-item>
        <el-form-item :label="t('accounts.displayName')">
          <el-input v-model="create.displayName" />
        </el-form-item>
        <el-form-item :label="t('accounts.email')">
          <el-input v-model="create.email" :placeholder="t('accounts.emailOptional')" />
        </el-form-item>
        <el-form-item :label="t('login.password')">
          <el-input v-model="create.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('accounts.role')">
          <el-select v-model="create.role" style="width: 100%">
            <el-option v-for="r in roles" :key="r" :label="roleText(r)" :value="r" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 改角色（提权 / 降权） -->
    <el-dialog v-model="editOpen" :title="t('accounts.changeRole')" width="440px">
      <p class="dlg-who">{{ editingRow?.username }}</p>
      <el-select v-model="editRole" style="width: 100%">
        <el-option v-for="r in roles" :key="r" :label="roleText(r)" :value="r" />
      </el-select>
      <template #footer>
        <el-button @click="editOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 重置口令 -->
    <el-dialog v-model="pwOpen" :title="t('accounts.resetPassword')" width="440px">
      <p class="dlg-who">{{ editingRow?.username }}</p>
      <el-input v-model="newPassword" type="password" show-password :placeholder="t('accounts.newPassword')" />
      <template #footer>
        <el-button @click="pwOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitPassword">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../stores/auth'
import { useApiError } from '../i18n/apiError'

const { t } = useI18n()
const apiError = useApiError()
const auth = useAuthStore()

const roles = ['user', 'admin', 'superadmin']
const rows = ref([])
const loading = ref(false)
const saving = ref(false)

const createOpen = ref(false)
const create = ref({ username: '', displayName: '', email: '', password: '', role: 'user' })

const editOpen = ref(false)
const editingRow = ref(null)
const editRole = ref('user')

const pwOpen = ref(false)
const newPassword = ref('')

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/accounts')
    rows.value = data
  } catch (e) {
    ElMessage.error(apiError(e, 'accounts.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  create.value = { username: '', displayName: '', email: '', password: '', role: 'user' }
  createOpen.value = true
}

async function submitCreate() {
  saving.value = true
  try {
    await api.post('/accounts', create.value)
    ElMessage.success(t('accounts.created'))
    createOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(apiError(e, 'accounts.saveFailed'))
  } finally {
    saving.value = false
  }
}

function openEdit(row) {
  editingRow.value = row
  editRole.value = row.role
  editOpen.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    await api.patch(`/accounts/${editingRow.value.id}`, { role: editRole.value })
    ElMessage.success(t('accounts.roleChanged'))
    editOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(apiError(e, 'accounts.saveFailed'))
  } finally {
    saving.value = false
  }
}

function openPassword(row) {
  editingRow.value = row
  newPassword.value = ''
  pwOpen.value = true
}

async function submitPassword() {
  saving.value = true
  try {
    await api.patch(`/accounts/${editingRow.value.id}`, { password: newPassword.value })
    ElMessage.success(t('accounts.passwordChanged'))
    pwOpen.value = false
  } catch (e) {
    ElMessage.error(apiError(e, 'accounts.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await api.delete(`/accounts/${row.id}`)
    ElMessage.success(t('accounts.deleted'))
    await load()
  } catch (e) {
    ElMessage.error(apiError(e, 'accounts.deleteFailed'))
  }
}

function roleText(role) {
  return ({
    user: t('roles.user'), admin: t('roles.admin'), superadmin: t('roles.superadmin')
  })[role] || role
}

function tagType(role) {
  return ({ user: 'info', admin: 'warning', superadmin: 'danger' })[role] || 'info'
}
</script>

<style scoped>
.card-head { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 12px; }
.card-head > span:first-child { font-weight: 600; }
.hint { color: #8A94A6; font-size: 12px; flex: 1 1 320px; }
.mb-sm { margin-bottom: 10px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 10px; }
.muted { color: #909399; }
.dlg-who { margin: 0 0 10px; font-weight: 600; color: #303133; }
</style>
