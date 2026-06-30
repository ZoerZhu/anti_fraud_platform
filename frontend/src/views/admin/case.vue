<template>
  <div class="admin-case">
    <div class="admin-case__header">
      <h2 class="admin-case__title">案例管理</h2>
      <el-button type="primary" @click="handleCreate">新增案例</el-button>
    </div>

    <div class="admin-case__toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索案例标题..."
        clearable
        style="width: 240px"
        @clear="fetchCases"
        @keyup.enter="fetchCases"
      >
        <template #prefix>
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
        </template>
      </el-input>
      <el-select v-model="selectedTagId" placeholder="按标签筛选" clearable style="width: 180px" @change="handleSearch" @clear="handleSearch">
        <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width: 140px" @change="handleSearch" @clear="handleSearch">
        <el-option label="已发布" :value="1" />
        <el-option label="草稿/禁用" :value="0" />
      </el-select>
      <el-button @click="handleSearch">查询</el-button>
      <el-button @click="openTagDialog">标签管理</el-button>
    </div>

    <el-table :data="caseList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <div class="case-title">
            <span v-if="row.isFeatured" class="featured-badge">精选</span>
            {{ row.title }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="caseType" label="类型" width="120" />
      <el-table-column label="标签" min-width="150">
        <template #default="{ row }">
          <div class="tag-list">
            <el-tag v-for="tag in row.tags" :key="tag.id" size="small" effect="plain">
              {{ tag.name }}
            </el-tag>
            <span v-if="!row.tags?.length" class="muted">未设置</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="difficultyLevel" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="getDifficultyType(row.difficultyLevel)" size="small">
            {{ row.difficultyName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="riskScore" label="风险" width="90">
        <template #default="{ row }">
          {{ Number(row.riskScore ?? 0).toFixed(1) }}
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" width="160">
        <template #default="{ row }">
          {{ row.publishTime ? new Date(row.publishTime).toLocaleString() : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="handlePublish(row)">发布</el-button>
          <el-button v-else link type="warning" @click="handleDisable(row)">禁用</el-button>
          <el-button link :type="row.isFeatured ? 'warning' : 'primary'" @click="toggleFeatured(row)">
            {{ row.isFeatured ? '取消精选' : '设为精选' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      layout="total, prev, pager, next"
      @current-change="fetchCases"
      style="margin-top: 20px; justify-content: flex-end"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑案例' : '新增案例'" width="920px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="类型" prop="caseType">
          <el-input v-model="form.caseType" placeholder="请输入案例类型" />
        </el-form-item>
        <el-form-item label="难度等级" prop="difficultyLevel">
          <el-rate v-model="form.difficultyLevel" :max="5" show-text :texts="['入门', '简单', '中等', '困难', '噩梦']" />
        </el-form-item>
        <el-form-item label="风险评分" prop="riskScore">
          <el-input-number v-model="form.riskScore" :min="0" :max="10" :step="0.5" />
        </el-form-item>
        <el-form-item label="目标年级" prop="targetGrades">
          <el-checkbox-group v-model="form.targetGrades">
            <el-checkbox label="大一" />
            <el-checkbox label="大二" />
            <el-checkbox label="大三" />
            <el-checkbox label="大四" />
            <el-checkbox label="all">全部</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="目标专业" prop="targetMajors">
          <el-select
            v-model="form.targetMajors"
            multiple
            allow-create
            filterable
            default-first-option
            placeholder="可选择或输入专业，all 表示全部"
            style="width: 100%"
          >
            <el-option label="全部" value="all" />
            <el-option label="计算机类" value="计算机类" />
            <el-option label="经管类" value="经管类" />
            <el-option label="文法类" value="文法类" />
            <el-option label="艺术类" value="艺术类" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tagIds">
          <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="案例内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入案例详细内容" />
        </el-form-item>
        <el-form-item label="剧本结构">
          <ScenarioScriptEditor v-model="scriptModel" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">发布</el-radio>
            <el-radio :label="0">草稿/禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="精选">
          <el-switch v-model="form.isFeatured" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="handleSubmit(0)" :loading="submitting">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit(1)" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tagDialogVisible" title="标签管理" width="720px">
      <el-form :model="tagForm" inline class="tag-form">
        <el-form-item label="名称">
          <el-input v-model="tagForm.name" placeholder="标签名称" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="tagForm.category" placeholder="分类" clearable />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="tagSubmitting" @click="handleSaveTag">
            {{ tagForm.id ? '更新' : '新增' }}
          </el-button>
          <el-button v-if="tagForm.id" @click="resetTagForm">取消编辑</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tags" size="small" max-height="360">
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="颜色" width="100">
          <template #default="{ row }">
            <span class="color-swatch" :style="{ backgroundColor: row.color || '#111111' }"></span>
            {{ row.color || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editTag(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteTag(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminCasePage,
  getAllTags,
  createCase,
  updateCase,
  deleteCase,
  publishCase,
  setCaseFeatured,
  createTag,
  updateTag,
  deleteTag
} from '@/api/case'
import ScenarioScriptEditor from '@/components/admin/ScenarioScriptEditor.vue'
import { emptyScenarioScript, parseScenarioScriptJson } from '@/types/scenario-script'
import type { ScenarioScriptModel } from '@/types/scenario-script'
import type { CaseVO, TagVO } from '@/api/case'

const loading = ref(false)
const submitting = ref(false)
const tagSubmitting = ref(false)
const caseList = ref<CaseVO[]>([])
const tags = ref<TagVO[]>([])
const searchKeyword = ref('')
const selectedTagId = ref<number | null>(null)
const filterStatus = ref<number | null>(null)
const dialogVisible = ref(false)
const tagDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const scriptModel = ref<ScenarioScriptModel>(emptyScenarioScript())

const form = ref({
  id: null as number | null,
  title: '',
  caseType: '',
  content: '',
  targetGrades: [] as string[],
  targetMajors: [] as string[],
  difficultyLevel: 1,
  riskScore: 0,
  tagIds: [] as number[],
  status: 0,
  isFeatured: 0
})

const tagForm = ref({
  id: null as number | null,
  name: '',
  category: '',
  description: '',
  color: '#111111'
})

const rules = {
  title: [{ required: true, message: '请输入案例标题', trigger: 'blur' }],
  caseType: [{ required: true, message: '请输入案例类型', trigger: 'blur' }],
  content: [{ required: true, message: '请输入案例内容', trigger: 'blur' }],
  difficultyLevel: [{ required: true, message: '请选择难度等级', trigger: 'change' }]
}

const fetchCases = async () => {
  loading.value = true
  try {
    const res = await getAdminCasePage({
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
      tagId: selectedTagId.value || undefined,
      status: filterStatus.value ?? undefined,
      keyword: searchKeyword.value || undefined
    })
    caseList.value = res.records ?? []
    pagination.value.total = res.total ?? 0
  } catch (error) {
    console.error('获取案例列表失败:', error)
    ElMessage.error('获取案例列表失败')
  } finally {
    loading.value = false
  }
}

const fetchTags = async () => {
  try {
    const res = await getAllTags()
    tags.value = res ?? []
  } catch (error) {
    console.error('获取标签失败:', error)
    ElMessage.error('获取标签失败')
  }
}

const handleSearch = () => {
  pagination.value.current = 1
  fetchCases()
}

const handleCreate = () => {
  scriptModel.value = emptyScenarioScript()
  form.value = {
    id: null,
    title: '',
    caseType: '',
    content: '',
    targetGrades: [],
    targetMajors: [],
    difficultyLevel: 1,
    riskScore: 0,
    tagIds: [],
    status: 0,
    isFeatured: 0
  }
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row: CaseVO) => {
  scriptModel.value = parseScenarioScriptJson(row.scripts)
  form.value = {
    id: row.id,
    title: row.title,
    caseType: row.caseType,
    content: row.content,
    targetGrades: row.targetGrades || [],
    targetMajors: row.targetMajors || [],
    difficultyLevel: row.difficultyLevel,
    riskScore: Number(row.riskScore),
    tagIds: row.tags?.map((t) => t.id) || [],
    status: row.status,
    isFeatured: row.isFeatured
  }
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async (statusOverride?: number) => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      ...form.value,
      status: statusOverride ?? form.value.status,
      scripts: JSON.stringify(scriptModel.value)
    }
    if (isEdit.value) {
      await updateCase(form.value.id!, payload)
      ElMessage.success('更新成功')
    } else {
      await createCase(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchCases()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const handlePublish = async (row: CaseVO) => {
  try {
    await publishCase(row.id)
    ElMessage.success('发布成功')
    fetchCases()
  } catch (error) {
    console.error('发布失败:', error)
  }
}

const handleDisable = async (row: CaseVO) => {
  try {
    await ElMessageBox.confirm(`确定禁用案例"${row.title}"吗？禁用后学生端不可见。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateCase(row.id, { status: 0 })
    ElMessage.success('已禁用')
    fetchCases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('禁用失败:', error)
    }
  }
}

const handleDelete = async (row: CaseVO) => {
  try {
    await ElMessageBox.confirm(`确定删除案例"${row.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCase(row.id)
    ElMessage.success('删除成功')
    fetchCases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const toggleFeatured = async (row: CaseVO) => {
  try {
    const newStatus = row.isFeatured ? 0 : 1
    await setCaseFeatured(row.id, newStatus)
    ElMessage.success(newStatus ? '已设为精选' : '已取消精选')
    fetchCases()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const openTagDialog = () => {
  resetTagForm()
  tagDialogVisible.value = true
}

const resetTagForm = () => {
  tagForm.value = {
    id: null,
    name: '',
    category: '',
    description: '',
    color: '#111111'
  }
}

const editTag = (row: TagVO) => {
  tagForm.value = {
    id: row.id,
    name: row.name,
    category: row.category || '',
    description: row.description || '',
    color: row.color || '#111111'
  }
}

const handleSaveTag = async () => {
  if (!tagForm.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (!tagForm.value.category.trim()) {
    ElMessage.warning('请输入标签分类')
    return
  }
  tagSubmitting.value = true
  try {
    const payload = {
      name: tagForm.value.name.trim(),
      category: tagForm.value.category.trim(),
      description: tagForm.value.description.trim() || undefined,
      color: tagForm.value.color || '#111111'
    }
    if (tagForm.value.id) {
      await updateTag(tagForm.value.id, payload)
      ElMessage.success('标签已更新')
    } else {
      await createTag(payload)
      ElMessage.success('标签已新增')
    }
    resetTagForm()
    await fetchTags()
    await fetchCases()
  } catch (error) {
    console.error('保存标签失败:', error)
  } finally {
    tagSubmitting.value = false
  }
}

const handleDeleteTag = async (row: TagVO) => {
  try {
    await ElMessageBox.confirm(`确定删除标签"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTag(row.id)
    ElMessage.success('标签已删除')
    if (selectedTagId.value === row.id) {
      selectedTagId.value = null
    }
    await fetchTags()
    await fetchCases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除标签失败:', error)
    }
  }
}

const getDifficultyType = (level: number) => {
  const types = ['', 'success', 'primary', 'warning', 'danger', 'danger']
  return types[level] || 'info'
}

onMounted(() => {
  fetchCases()
  fetchTags()
})
</script>

<style scoped lang="scss">
.admin-case {
  padding: 24px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  &__toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }
}

.case-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.featured-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  font-size: 11px;
  color: #fff;
  background: #f59e0b;
  border-radius: 4px;
}

.tag-form {
  margin-bottom: 16px;
}

.color-swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  vertical-align: -1px;
}
</style>
