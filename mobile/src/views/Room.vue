<template>
  <div class="page">
    <div v-if="room" class="card">
      <h2>{{ room.code }}</h2>
      <dl class="kv">
        <dt>{{ t('find.building') }}</dt>
        <dd>{{ room.building }}</dd>
        <dt>{{ t('room.floor') }}</dt>
        <dd>{{ room.floor || t('room.unknown') }}</dd>
        <dt>{{ t('room.capacity') }}</dt>
        <dd>{{ room.capacity }}</dd>
        <dt>{{ t('room.seatType') }}</dt>
        <dd>{{ room.seatType || t('room.unknown') }}</dd>
        <dt>{{ t('find.sockets') }}</dt>
        <dd>{{ room.sockets === true ? '✓' : room.sockets === false ? '✗' : t('room.unknown') }}</dd>
      </dl>
      <div class="meta" style="margin-top: 10px" v-if="room.facilitiesVerifiedAt">
        <span class="chip">{{ t('room.facilities') }}: {{ room.facilitiesVerifiedAt }}</span>
      </div>
    </div>

    <div class="card">
      <h2>{{ t('room.timetable') }} · {{ date }}</h2>
      <div v-if="segments.length">
        <div class="strip">
          <div
            v-for="(s, i) in segments"
            :key="i"
            class="cell"
            :class="s.type === 'BUSY' ? 'busy' : 'free'"
            :style="{ flex: s.span }"
            :title="s.label || ''"
          >
            {{ s.type === 'BUSY' && s.span >= 3 ? s.label || '' : '' }}
          </div>
        </div>
        <div class="ticks">
          <span>{{ segments[0]?.hour || '08:00' }}</span>
          <span>{{ segments[segments.length - 1]?.to || '21:50' }}</span>
        </div>
      </div>
      <p v-else class="hint">{{ t('common.loading') }}</p>
    </div>

    <div class="card">
      <h2>
        {{ t('room.reviews') }}
        <span v-if="reviews && reviews.count" class="hint" style="font-weight: 400">
          · {{ reviews.average }} ★ ({{ reviews.count }})
        </span>
      </h2>
      <div v-for="rv in reviews?.items || []" :key="rv.id" class="review">
        <span class="stars">{{ '★'.repeat(rv.rating) }}{{ '☆'.repeat(5 - rv.rating) }}</span>
        <span class="who">{{ rv.author }}</span>
        <div class="body">{{ rv.body }}</div>
        <div v-if="rv.photos && rv.photos.length" class="photos">
          <img v-for="p in rv.photos" :key="p.id" :src="p.url" alt="" loading="lazy" />
        </div>
      </div>
      <p v-if="reviews && !reviews.count" class="hint">{{ t('room.noReviews') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api.js'
import { t } from '../i18n.js'

const route = useRoute()
const code = route.params.code
const date = route.query.date || new Date().toISOString().slice(0, 10)

const room = ref(null)
const segments = ref([])
const reviews = ref(null)

onMounted(async () => {
  try {
    const { data } = await api.get('/rooms')
    room.value = (data || []).find((r) => r.code === code) || null
  } catch (e) {
    /* 房间信息拿不到也不挡时间轴 */
  }
  try {
    const { data } = await api.get(`/rooms/${code}/timeline`, { params: { date } })
    segments.value = data.segments || []
  } catch (e) {
    segments.value = []
  }
  try {
    const { data } = await api.get(`/rooms/${code}/reviews`, { params: { sort: 'new' } })
    reviews.value = data
  } catch (e) {
    reviews.value = { count: 0, items: [] }
  }
})
</script>
