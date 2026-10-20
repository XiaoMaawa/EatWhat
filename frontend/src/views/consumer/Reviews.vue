<template>
  <div class="ew-page">
    <div class="ew-card">
    <div class="ew-flex-between">
      <h3 class="ew-title">我的评价</h3>
      <span class="ew-muted">共 {{ total }} 条评价</span>
    </div>

    <div v-loading="loading" class="review-list">
      <div v-for="item in list" :key="item.id" class="ew-card review-item">
        <div class="review-head">
          <div class="shop-line">
            <span class="shop-name">{{ item.shopName || '店铺' }}</span>
            <span class="ew-sub">{{ item.createdAt }}</span>
          </div>
          <el-tag v-if="item.reply" type="success" size="small" effect="plain">商家已回复</el-tag>
          <el-tag v-else type="info" size="small" effect="plain">待回复</el-tag>
        </div>

        <div class="ops-line">
          <el-tag v-if="canEdit(item)" size="small" effect="plain" type="warning">
            {{ hoursLeft(item) }} 小时内可修改
          </el-tag>
          <el-tag v-else size="small" effect="plain" type="info">已超过 24 小时</el-tag>
          <div class="ops-btns">
            <el-button link type="primary" size="small" :disabled="!canEdit(item)" @click="openEdit(item)">
              修改
            </el-button>
            <el-button link type="danger" size="small" :disabled="!canEdit(item)" @click="removeReview(item)">
              删除
            </el-button>
          </div>
        </div>

        <div class="score-line">
          <span class="score-label">餐品</span>
          <el-rate :model-value="item.shopScore" disabled size="small" />
          <span class="score-label">配送</span>
          <el-rate :model-value="item.deliveryScore" disabled size="small" />
        </div>

        <p class="review-content">{{ item.content }}</p>

        <div v-if="item.imageList && item.imageList.length" class="image-line">
          <el-image
            v-for="(img, idx) in item.imageList"
            :key="idx"
            :src="img"
            :preview-src-list="item.imageList"
            :initial-index="idx"
            fit="cover"
            class="review-img"
          />
        </div>

        <div v-if="item.reply" class="reply-box">
          <span class="reply-label">商家回复：</span>{{ item.reply }}
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="还没有发表过评价，去订单里评价一下吧" />
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
    </div>

    <el-dialog v-model="dialogVisible" title="修改评价" width="520px">
      <el-form label-width="80px">
        <el-form-item label="餐品评分">
          <el-rate v-model="form.shopScore" />
        </el-form-item>
        <el-form-item label="配送评分">
          <el-rate v-model="form.deliveryScore" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说这次用餐的感受吧（10-500 字）"
          />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="form.isAnonymous" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewApi } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({
  shopScore: 5,
  deliveryScore: 5,
  content: '',
  isAnonymous: false
})

/** 评价创建后 24 小时内可修改或删除 */
const EDIT_HOURS = 24

function canEdit(item) {
  if (!item.createdAt) return true
  const created = new Date(String(item.createdAt).replace(/-/g, '/')).getTime()
  if (Number.isNaN(created)) return true
  return Date.now() - created < EDIT_HOURS * 3600 * 1000
}

function hoursLeft(item) {
  const created = new Date(String(item.createdAt).replace(/-/g, '/')).getTime()
  if (Number.isNaN(created)) return EDIT_HOURS
  const left = EDIT_HOURS - (Date.now() - created) / 3600000
  return Math.max(0, Math.ceil(left))
}

function openEdit(item) {
  editingId.value = item.id
  form.shopScore = item.shopScore || 5
  form.deliveryScore = item.deliveryScore || 5
  form.content = item.content || ''
  form.isAnonymous = item.isAnonymous === 1
  dialogVisible.value = true
}

async function submitEdit() {
  if (!form.content || form.content.trim().length < 10) {
    ElMessage.warning('评价内容至少 10 个字')
    return
  }
  saving.value = true
  try {
    await reviewApi.update(editingId.value, { ...form })
    ElMessage.success('评价已更新')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function removeReview(item) {
  await ElMessageBox.confirm('删除后无法恢复，确定删除这条评价吗？', '提示', { type: 'warning' })
  await reviewApi.remove(item.id)
  ElMessage.success('评价已删除')
  await load()
}

async function load() {
  loading.value = true
  try {
    const data = await reviewApi.mine({ page: page.value, size })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.review-item {
  padding: 16px;
  border: 1px solid var(--ew-border);
  box-shadow: none;
  border-radius: 10px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.shop-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.shop-name {
  font-weight: 600;
  font-size: 15px;
}

.score-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.score-label {
  font-size: 12px;
  color: var(--ew-text-sub);
}

.review-content {
  margin: 0 0 10px;
  line-height: 1.7;
  color: var(--ew-text);
  white-space: pre-wrap;
}

.image-line {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.review-img {
  width: 84px;
  height: 84px;
  border-radius: 8px;
}

.reply-box {
  background: var(--ew-bg-soft, #f7f8fa);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--ew-text-sub);
  line-height: 1.6;
}

.reply-label {
  color: var(--ew-primary);
  font-weight: 600;
}

.ops-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ops-btns {
  display: flex;
  gap: 4px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 16px 0 0;
}
</style>
