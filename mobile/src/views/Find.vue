<template>
  <div class="page">
    <div class="card">
      <h2>{{ t('find.title') }}</h2>

      <label class="field">
        <span>{{ t('find.building') }}</span>
        <select v-model="building">
          <option value="">{{ allBuildings }}</option>
          <option v-for="b in buildings" :key="b" :value="b">{{ b }}</option>
        </select>
      </label>

      <label class="field">
        <span>{{ t('find.date') }}</span>
        <input v-model="date" type="date" />
      </label>

      <div class="row">
        <label class="field">
          <span>{{ t('find.from') }}</span>
          <select v-model="from">
            <option v-for="h in hours" :key="h" :value="h">{{ h }}</option>
          </select>
        </label>
        <label class="field">
          <span>{{ t('find.to') }}</span>
          <select v-model="to">
            <option v-for="h in toOptions" :key="h" :value="h">{{ h }}</option>
          </select>
        </label>
      </div>

      <button class="btn" :disabled="busy" @click="search">
        {{ busy ? t('common.loading') : t('find.search') }}
      </button>
      <p v-if="error" class="err">{{ error }}</p>
    </div>

    <p v-if="searched && !error" class="hint" style="margin: 0 2px 10px">
      {{ result && result.matchedCount > 0 ? t('find.count', { n: result.matchedCount }) : t('find.none') }}
    </p>

    <router-link
      v-for="r in matched"
      :key="r.room.code"
      class="room"
      :to="{ path: '/room/' + r.room.code, query: { date } }"
    >
      <div class="code">{{ r.room.code }}</div>
      <div class="where">
        {{ r.room.building }} · {{ floorText(r.room) }} · {{ t('find.seats', { n: r.room.capacity }) }}
      </div>
      <div class="meta">
        <span class="chip" v-if="r.room.sockets === true">{{ t('find.sockets') }}</span>
        <span class="chip off" v-else-if="r.room.sockets === false">{{ t('find.noSockets') }}</span>
        <span class="chip off" v-else>{{ t('find.socketsUnknown') }}</span>
        <span class="chip">
          {{ r.nextBusyStart ? t('find.nextBusy', { t: r.nextBusyStart }) : t('find.allDay') }}
        </span>
      </div>
    </router-link>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api, errorText } from '../api.js'
import { t } from '../i18n.js'

const today = () => new Date().toISOString().slice(0, 10)
const pad = (n) => String(n).padStart(2, '0')

// 从下一个整点开始（后端要求 from 是整点）；超出 08:00–21:00 就退回 09:00
function defaultFrom() {
  const h = new Date().getHours() + 1
  return h >= 8 && h <= 20 ? `${pad(h)}:00` : '09:00'
}

const buildings = ref([])
const building = ref('')
const date = ref(today())
const from = ref(defaultFrom())
const to = ref(plusMinutes(from.value, 110))

const busy = ref(false)
const error = ref('')
const searched = ref(false)
const result = ref(null)

const hours = Array.from({ length: 14 }, (_, i) => `${pad(i + 8)}:00`) // 08:00–21:00
const allBuildings = computed(() => (building.value ? 'All buildings' : 'All buildings'))

const toOptions = computed(() => {
  const [h, m] = from.value.split(':').map(Number)
  const start = h * 60 + (m || 0)
  // 只允许 :50 结尾；1–3 个 block
  return [50, 110, 170]
    .map((d) => start + d)
    .filter((mins) => mins <= 21 * 60 + 50)
    .map((mins) => `${pad(Math.floor(mins / 60))}:${pad(mins % 60)}`)
})

function plusMinutes(hhmm, mins) {
  const [h, m] = hhmm.split(':').map(Number)
  const total = h * 60 + m + mins
  return `${pad(Math.floor(total / 60))}:${pad(total % 60)}`
}

const matched = computed(() => (result.value?.rooms || []).filter((r) => r.matches))

function floorText(room) {
  return room.floor ? `${t('room.floor')} ${room.floor}` : ''
}

async function load() {
  try {
    const [b, r] = await Promise.all([api.get('/buildings'), api.get('/rooms')])
    buildings.value = b.data || []
    rooms.value = r.data || []
  } catch (e) {
    // 楼栋列表拿不到不影响查询
  }
}

const rooms = ref([])

async function search() {
  busy.value = true
  error.value = ''
  try {
    const { data } = await api.get('/availability', {
      params: {
        date: date.value,
        building: building.value || undefined,
        from: from.value,
        to: to.value,
      },
    })
    result.value = data
    searched.value = true
  } catch (e) {
    error.value = errorText(e)
    result.value = null
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  await load()
  await search()
})
</script>
