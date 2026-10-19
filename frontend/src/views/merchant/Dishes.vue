<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">菜品管理</h3>
        <el-button type="primary" @click="openDishDialog()">新增菜品</el-button>
      </div>

      <div class="toolbar">
        <el-select v-model="filterCategoryId" placeholder="全部分类" clearable style="width: 160px" @change="loadDishes">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索菜品名称" clearable style="width: 200px" @keyup.enter="loadDishes" />
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 130px" @change="loadDishes">
          <el-option label="在售" :value="1" />
          <el-option label="已下架" :value="0" />
          <el-option label="售罄" :value="2" />
        </el-select>
        <el-button type="primary" plain @click="loadDishes">搜索</el-button>
        <el-button plain @click="openCategoryManager">分类管理</el-button>
        <el-button v-if="selection.length" plain type="warning" @click="batchOffline">
          批量下架（{{ selection.length }}）
        </el-button>
      </div>

      <el-table v-loading="loading" :data="dishes" size="default" @selection-change="(rows) => (selection = rows)">
        <el-table-column type="selection" width="46" />
        <el-table-column label="菜品" min-width="220">
          <template #default="{ row }">
            <div class="dish-cell">
              <div class="dish-img">
                <img v-if="row.image" :src="row.image" alt="" />
                <span v-else>🍚</span>
              </div>
              <div>
                <div class="dish-name">{{ row.name }}</div>
                <div class="ew-muted">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column prop="monthSales" label="月销" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="DISH_STATUS[row.status]?.type">
              {{ DISH_STATUS[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDishDialog(row)">编辑</el-button>
            <el-button
              size="small"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" link type="danger" @click="removeDish(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && dishes.length === 0" description="还没有菜品，先加一道招牌吧" />
    </div>

    <!-- 菜品表单 -->
    <el-dialog v-model="dishVisible" :title="dishForm.id ? '编辑菜品' : '新增菜品'" width="560px">
      <el-form ref="dishFormRef" :model="dishForm" :rules="dishRules" label-width="90px">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="dishForm.name" maxlength="100" placeholder="例如：宫保鸡丁" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="dishForm.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="dishForm.price" :min="0.01" :precision="2" :step="1" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="dishForm.stock" :min="0" :step="10" style="width: 180px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dishForm.sort" :min="0" style="width: 180px" />
        </el-form-item>
        <el-form-item label="菜品图片">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="uploadImage"
            accept="image/*"
          >
            <div class="upload-box">
              <img v-if="dishForm.image" :src="dishForm.image" alt="" />
              <span v-else>点击上传</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dishForm.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dishVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDish">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类管理 -->
    <el-dialog v-model="categoryVisible" title="分类管理" width="520px">
      <div class="category-add">
        <el-input v-model="newCategoryName" placeholder="输入分类名称" maxlength="50" />
        <el-button type="primary" @click="addCategory">新增</el-button>
      </div>
      <el-table :data="categories" size="small">
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="renameCategory(row)">重命名</el-button>
            <el-button size="small" link type="danger" @click="removeCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi, fileApi } from '@/api'
import { money } from '@/utils/format'
import { DISH_STATUS } from '@/utils/orderStatus'

const loading = ref(false)
const submitting = ref(false)
const dishes = ref([])
const categories = ref([])
const selection = ref([])
const keyword = ref('')
const filterCategoryId = ref(null)
const filterStatus = ref(null)

const dishVisible = ref(false)
const dishFormRef = ref()
const dishForm = reactive({
  id: null,
  name: '',
  categoryId: null,
  price: 10,
  stock: 100,
  image: '',
  description: '',
  sort: 0
})

const categoryVisible = ref(false)
const newCategoryName = ref('')

const dishRules = {
  name: [{ required: true, message: '请填写菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请填写价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请填写库存', trigger: 'blur' }]
}

function categoryName(id) {
  return categories.value.find((c) => c.id === id)?.name || '—'
}

async function loadCategories() {
  categories.value = (await merchantApi.categories()) || []
}

async function loadDishes() {
  loading.value = true
  try {
    const params = {}
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (keyword.value) params.keyword = keyword.value
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = filterStatus.value
    dishes.value = (await merchantApi.dishes(params)) || []
  } finally {
    loading.value = false
  }
}

function openDishDialog(row) {
  Object.assign(dishForm, {
    id: null,
    name: '',
    categoryId: categories.value[0]?.id || null,
    price: 10,
    stock: 100,
    image: '',
    description: '',
    sort: 0
  })
  if (row) Object.assign(dishForm, row)
  dishVisible.value = true
}

async function submitDish() {
  await dishFormRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      categoryId: dishForm.categoryId,
      name: dishForm.name,
      price: dishForm.price,
      stock: dishForm.stock,
      image: dishForm.image || null,
      description: dishForm.description || null,
      sort: dishForm.sort
    }
    if (dishForm.id) {
      await merchantApi.updateDish(dishForm.id, payload)
    } else {
      await merchantApi.addDish(payload)
    }
    ElMessage.success('已保存')
    dishVisible.value = false
    await loadDishes()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await merchantApi.updateDishStatus(row.id, target)
  ElMessage.success(target === 1 ? '已上架' : '已下架')
  await loadDishes()
}

async function batchOffline() {
  await ElMessageBox.confirm(`确定下架选中的 ${selection.value.length} 道菜品吗？`, '批量下架', {
    type: 'warning'
  })
  await merchantApi.batchStatus(
    selection.value.map((d) => d.id),
    0
  )
  ElMessage.success('已批量下架')
  await loadDishes()
}

async function removeDish(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除菜品', { type: 'warning' })
  await merchantApi.deleteDish(row.id)
  ElMessage.success('已删除')
  await loadDishes()
}

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function uploadImage(options) {
  const data = await fileApi.upload(options.file)
  dishForm.image = data.url
  ElMessage.success('图片已上传')
}

function openCategoryManager() {
  newCategoryName.value = ''
  categoryVisible.value = true
}

async function addCategory() {
  if (!newCategoryName.value.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  await merchantApi.addCategory({ name: newCategoryName.value.trim(), sort: categories.value.length + 1 })
  ElMessage.success('分类已新增')
  newCategoryName.value = ''
  await loadCategories()
}

async function renameCategory(row) {
  const { value } = await ElMessageBox.prompt('新的分类名称', '重命名分类', {
    inputValue: row.name,
    inputValidator: (v) => (v && v.trim() ? true : '分类名称不能为空')
  })
  await merchantApi.updateCategory(row.id, { name: value.trim(), sort: row.sort })
  ElMessage.success('已重命名')
  await loadCategories()
}

async function removeCategory(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？分类下有菜品时无法删除。`, '删除分类', {
    type: 'warning'
  })
  await merchantApi.deleteCategory(row.id)
  ElMessage.success('已删除')
  await loadCategories()
}

onMounted(async () => {
  await loadCategories()
  await loadDishes()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  overflow: hidden;
  flex-shrink: 0;
}

.dish-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-name {
  font-weight: 600;
  font-size: 14px;
}

.upload-box {
  width: 96px;
  height: 96px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  cursor: pointer;
}

.upload-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.unit {
  margin-left: 8px;
  color: #909399;
}

.category-add {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
