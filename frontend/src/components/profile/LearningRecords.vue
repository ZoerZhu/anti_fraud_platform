<template>
  <div class="learning-records">
    <div v-if="loading" class="learning-records__loading">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="records.length === 0" class="learning-records__empty">
      <el-empty description="暂无学习记录">
        <el-button type="primary" @click="$router.push('/challenge')">去学习</el-button>
      </el-empty>
    </div>

    <div v-else class="learning-records__list">
      <el-timeline>
        <el-timeline-item
          v-for="item in records"
          :key="item.recordKey || `${item.type}-${item.id}`"
          :timestamp="formatRecordTime(item.time)"
          :color="getItemColor(item.type)"
          :icon="getItemIcon(item.type)"
          placement="top"
        >
          <el-card shadow="hover" class="learning-records__item">
            <div class="learning-records__content">
              <span class="learning-records__type" :class="`learning-records__type--${item.type}`">
                {{ getTypeLabel(item.type) }}
              </span>
              <p class="learning-records__desc">{{ item.content }}</p>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <div class="learning-records__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, type Component } from 'vue'
import { get } from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Trophy, Reading, Collection, ChatDotRound } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

interface LearningRecord {
  recordKey?: string
  id: number
  type: 'challenge' | 'case' | 'forum' | 'news' | 'scenario'
  targetId?: number
  title?: string
  content: string
  time: string
  meta?: Record<string, unknown>
}

interface LearningRecordPage {
  records: LearningRecord[]
  total: number
  size: number
  current: number
  pages: number
}

const loading = ref(false)
const records = ref<LearningRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getItemColor = (type: string) => {
  const colors: Record<string, string> = {
    challenge: 'hsl(45, 90%, 55%)',
    case: 'hsl(200, 70%, 50%)',
    forum: 'hsl(280, 60%, 55%)',
    news: 'hsl(150, 60%, 45%)',
    scenario: 'hsl(10, 80%, 55%)'
  }
  return colors[type] || 'hsl(220, 10%, 50%)'
}

const getItemIcon = (type: string) => {
  const icons: Record<string, Component> = {
    challenge: Trophy,
    case: Reading,
    forum: ChatDotRound,
    news: Collection,
    scenario: Trophy
  }
  return icons[type]
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    challenge: '闯关',
    case: '案例',
    forum: '社区',
    news: '资讯',
    scenario: '情景'
  }
  return labels[type] || type
}

const formatRecordTime = (time?: string) => {
  if (!time) return ''
  const parsed = dayjs(time)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : time
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await get<LearningRecordPage>('/learning/records', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    records.value = []
    total.value = 0
    ElMessage.warning('学习记录加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchRecords()
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped lang="scss">
.learning-records {
  &__loading {
    padding: 20px 0;
  }

  &__empty {
    padding: 60px 0;
  }

  &__list {
    :deep(.el-timeline-item__node) {
      background-color: hsl(225, 60%, 55%);
    }

    :deep(.el-timeline-item__timestamp) {
      color: hsl(220, 10%, 55%);
      font-size: 13px;
    }
  }

  &__item {
    border-radius: 8px;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px hsla(220, 10%, 85%, 0.5);
    }
  }

  &__content {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  &__type {
    flex-shrink: 0;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
    color: #fff;

    &--challenge {
      background: hsl(45, 90%, 55%);
    }

    &--case {
      background: hsl(200, 70%, 50%);
    }

    &--forum {
      background: hsl(280, 60%, 55%);
    }

    &--news {
      background: hsl(150, 60%, 45%);
    }

    &--scenario {
      background: hsl(10, 80%, 55%);
    }
  }

  &__desc {
    margin: 0;
    font-size: 14px;
    color: hsl(220, 10%, 30%);
    line-height: 1.6;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px solid hsl(220, 10%, 92%);

    :deep(.el-pagination.is-background .el-pager li.is-active) {
      background-color: hsl(225, 60%, 55%);
    }
  }
}
</style>
