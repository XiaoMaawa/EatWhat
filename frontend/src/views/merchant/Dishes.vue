<template>
  <div class="ew-page">
    <div class="ew-card">
      <div class="ew-flex-between">
        <h3 class="ew-title">
          菜品管理
          <el-alert
            v-if="lowStockCount > 0"
            type="warning"
            :closable="false"
            show-icon
            class="low-stock-alert"
            :title="`有 ${lowStockCount} 个菜品库存不足 10 份，请及时补货`"
          />
        </h3>
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
                <div class="dish-name">
                  {{ row.name }}
                  <el-tag v-if="row.isCombo === 1" size="small" type="warning" effect="plain">套餐</el-tag>
                </div>
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
        <el-table-column label="库存" width="110">
          <template #default="{ row }">
            <span :class="{ 'stock-low': row.stock != null && row.stock < 10 }">{{ row.stock }}</span>
            <el-tooltip v-if="row.stock != null && row.stock < 10" content="库存不足 10 份，请及时补货" placement="top">
              <el-tag size="small" type="danger" effect="plain" class="stock-tip">补货</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="monthSales" label="月销" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="DISH_STATUS[row.status]?.type">
              {{ DISH_STATUS[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
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
            <el-button size="small" link type="primary" @click="openSpecDialog(row)">规格加料</el-button>
            <el-button size="small" link type="primary" @click="openComboDialog(row)">
              {{ row.isCombo === 1 ? '套餐配置' : '设为套餐' }}
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
        <el-table-column label="操作" width="220">
          <template #default="{ row, $index }">
            <el-button size="small" link type="primary" :disabled="$index === 0" @click="moveCategory($index, -1)">
              上移
            </el-button>
            <el-button
              size="small"
              link
              type="primary"
              :disabled="$index === categories.length - 1"
              @click="moveCategory($index, 1)"
            >
              下移
            </el-button>
            <el-button size="small" link type="primary" @click="renameCategory(row)">重命名</el-button>
            <el-button size="small" link type="danger" @click="removeCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <!-- 顾客-27/28：规格与加料维护 -->
    <el-dialog v-model="specVisible" :title="`规格与加料 - ${specDish?.name || ''}`" width="620px">
      <el-tabs v-model="specTab">
        <el-tab-pane label="规格" name="spec">
          <div class="spec-toolbar">
            <el-button size="small" type="primary" @click="openSpecForm()">新增规格</el-button>
            <span class="ew-muted">同一菜品可配置多个规格，价格各自独立；默认规格会作为顾客下单时的预选项</span>
          </div>
          <el-table :data="specs" size="small" border>
            <el-table-column prop="name" label="规格名称" min-width="120" />
            <el-table-column label="价格" width="100">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column label="默认" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" size="small" type="success">默认</el-tag>
                <span v-else class="ew-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="openSpecForm(row)">编辑</el-button>
                <el-button size="small" link type="danger" @click="removeSpec(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="加料" name="addon">
          <div class="spec-toolbar">
            <el-button size="small" type="primary" @click="openAddonForm()">新增加料</el-button>
            <span class="ew-muted">顾客下单时最多可选 5 项加料，价格叠加到菜品单价上</span>
          </div>
          <el-table :data="addons" size="small" border>
            <el-table-column prop="name" label="加料名称" min-width="120" />
            <el-table-column label="价格" width="100">
              <template #default="{ row }">+¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="openAddonForm(row)">编辑</el-button>
                <el-button size="small" link type="danger" @click="removeAddon(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="specFormVisible" :title="specForm.id ? '编辑规格' : '新增规格'" width="420px">
      <el-form :model="specForm" label-width="90px">
        <el-form-item label="规格名称">
          <el-input v-model="specForm.name" maxlength="50" placeholder="如 大份 / 中份 / 小份" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="specForm.price" :min="0" :precision="2" :step="0.5" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="specForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="specForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="specFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="specSubmitting" @click="submitSpec">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addonFormVisible" :title="addonForm.id ? '编辑加料' : '新增加料'" width="420px">
      <el-form :model="addonForm" label-width="90px">
        <el-form-item label="加料名称">
          <el-input v-model="addonForm.name" maxlength="50" placeholder="如 加米饭 / 加卤蛋" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="addonForm.price" :min="0" :precision="2" :step="0.5" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="addonForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addonFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="specSubmitting" @click="submitAddon">保存</el-button>
      </template>
    </el-dialog>

    <!-- 商家-28：套餐配置 -->
    <el-dialog v-model="comboVisible" :title="`套餐配置 - ${comboDish?.name || ''}`" width="680px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="combo-tip"
        title="套餐自身是一条菜品记录，价格取该菜品的价格；下单时会同步扣减组成菜品的库存"
      />
      <div class="combo-head">
        <div class="combo-price">
          套餐价 <b>¥{{ money(comboDish?.price) }}</b>
          <span class="ew-muted">
            组成菜品原价合计 ¥{{ money(comboOriginal) }}
            <template v-if="comboOriginal > 0">
              （优惠 ¥{{ money(Math.max(0, comboOriginal - Number(comboDish?.price || 0))) }}）
            </template>
          </span>
        </div>
        <div class="combo-actions">
          <el-select v-model="comboPickId" placeholder="选择本店菜品" filterable style="width: 200px">
            <el-option
              v-for="d in comboPickOptions"
              :key="d.id"
              :label="`${d.name}（¥${money(d.price)}）`"
              :value="d.id"
            />
          </el-select>
          <el-button type="primary" plain :disabled="!comboPickId" @click="addComboItem">加入套餐</el-button>
        </div>
      </div>
      <el-table :data="comboItems" size="small" border>
        <el-table-column prop="dishName" label="菜品" min-width="160" />
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ money(comboDishPrice(row.dishId)) }}</template>
        </el-table-column>
        <el-table-column label="份数" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="20" size="small" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100">
          <template #default="{ row }">
            ¥{{ money(Number(comboDishPrice(row.dishId)) * (row.quantity || 1)) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button size="small" link type="danger" @click="comboItems.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="comboItems.length === 0" description="还没有加入菜品，从上方下拉框选择" :image-size="60" />
      <template #footer>
        <el-button v-if="comboDish?.isCombo === 1" type="danger" plain @click="cancelCombo">取消套餐</el-button>
        <el-button @click="comboVisible = false">关闭</el-button>
        <el-button type="primary" :loading="comboSubmitting" @click="submitCombo">保存套餐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
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

const lowStockCount = computed(
  () => dishes.value.filter((d) => d.stock != null && d.stock < 10).length
)

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

/** 商家-21：分类排序，上移 / 下移后重排 sort 并批量落库 */
async function moveCategory(index, delta) {
  const target = index + delta
  if (target < 0 || target >= categories.value.length) return
  const list = [...categories.value]
  const [moved] = list.splice(index, 1)
  list.splice(target, 0, moved)
  categories.value = list
  try {
    await Promise.all(
      list.map((c, i) => merchantApi.updateCategory(c.id, { name: c.name, sort: i + 1 }))
    )
    ElMessage.success('分类顺序已更新')
    await loadCategories()
  } catch {
    await loadCategories()
  }
}

onMounted(async () => {
  await loadCategories()
  await loadDishes()
})
// 顾客-27/28：规格与加料维护
const specVisible = ref(false)
const specTab = ref('spec')
const specDish = ref(null)
const specs = ref([])
const addons = ref([])
const specFormVisible = ref(false)
const addonFormVisible = ref(false)
const specSubmitting = ref(false)
const specForm = reactive({ id: null, name: '', price: 0, isDefault: 0, sort: 0 })
const addonForm = reactive({ id: null, name: '', price: 0, sort: 0 })

async function openSpecDialog(row) {
  specDish.value = row
  specTab.value = 'spec'
  specVisible.value = true
  await loadSpecs()
  await loadAddons()
}

async function loadSpecs() {
  if (!specDish.value) return
  specs.value = (await merchantApi.specs(specDish.value.id)) || []
}

async function loadAddons() {
  if (!specDish.value) return
  addons.value = (await merchantApi.addons(specDish.value.id)) || []
}

function openSpecForm(row) {
  if (row) {
    Object.assign(specForm, {
      id: row.id,
      name: row.name,
      price: Number(row.price),
      isDefault: row.isDefault,
      sort: row.sort
    })
  } else {
    Object.assign(specForm, { id: null, name: '', price: 0, isDefault: 0, sort: 0 })
  }
  specFormVisible.value = true
}

async function submitSpec() {
  if (!specForm.name.trim()) {
    ElMessage.warning('请填写规格名称')
    return
  }
  specSubmitting.value = true
  try {
    const payload = {
      name: specForm.name.trim(),
      price: specForm.price,
      isDefault: specForm.isDefault,
      sort: specForm.sort
    }
    if (specForm.id) {
      await merchantApi.updateSpec(specDish.value.id, specForm.id, payload)
    } else {
      await merchantApi.createSpec(specDish.value.id, payload)
    }
    ElMessage.success('规格已保存')
    specFormVisible.value = false
    await loadSpecs()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    specSubmitting.value = false
  }
}

async function removeSpec(row) {
  try {
    await ElMessageBox.confirm(`确定要删除规格「${row.name}」吗？`, '删除规格', { type: 'warning' })
  } catch {
    return
  }
  try {
    await merchantApi.removeSpec(specDish.value.id, row.id)
    ElMessage.success('规格已删除')
    await loadSpecs()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

function openAddonForm(row) {
  if (row) {
    Object.assign(addonForm, { id: row.id, name: row.name, price: Number(row.price), sort: row.sort })
  } else {
    Object.assign(addonForm, { id: null, name: '', price: 0, sort: 0 })
  }
  addonFormVisible.value = true
}

async function submitAddon() {
  if (!addonForm.name.trim()) {
    ElMessage.warning('请填写加料名称')
    return
  }
  specSubmitting.value = true
  try {
    const payload = { name: addonForm.name.trim(), price: addonForm.price, sort: addonForm.sort }
    if (addonForm.id) {
      await merchantApi.updateAddon(specDish.value.id, addonForm.id, payload)
    } else {
      await merchantApi.createAddon(specDish.value.id, payload)
    }
    ElMessage.success('加料已保存')
    addonFormVisible.value = false
    await loadAddons()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    specSubmitting.value = false
  }
}

async function removeAddon(row) {
  try {
    await ElMessageBox.confirm(`确定要删除加料「${row.name}」吗？`, '删除加料', { type: 'warning' })
  } catch {
    return
  }
  try {
    await merchantApi.removeAddon(specDish.value.id, row.id)
    ElMessage.success('加料已删除')
    await loadAddons()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

// 商家-28：套餐配置
const comboVisible = ref(false)
const comboSubmitting = ref(false)
const comboDish = ref(null)
const comboItems = ref([])
const comboPickId = ref(null)

const comboOriginal = computed(() =>
  comboItems.value.reduce(
    (sum, item) => sum + Number(comboDishPrice(item.dishId) || 0) * (item.quantity || 1),
    0
  )
)

const comboPickOptions = computed(() =>
  dishes.value.filter((d) => d.id !== comboDish.value?.id && d.isCombo !== 1)
)

function comboDishPrice(dishId) {
  return dishes.value.find((d) => d.id === dishId)?.price ?? 0
}

async function openComboDialog(row) {
  comboDish.value = row
  comboPickId.value = null
  comboVisible.value = true
  try {
    const items = await merchantApi.comboItems(row.id)
    comboItems.value = (items || []).map((item) => ({ ...item, quantity: item.quantity || 1 }))
  } catch {
    comboItems.value = []
  }
}

function addComboItem() {
  const dish = dishes.value.find((d) => d.id === comboPickId.value)
  if (!dish) return
  if (comboItems.value.some((item) => item.dishId === dish.id)) {
    ElMessage.warning('该菜品已在套餐中')
    return
  }
  comboItems.value.push({
    dishId: dish.id,
    dishName: dish.name,
    quantity: 1,
    sort: comboItems.value.length
  })
  comboPickId.value = null
}

async function submitCombo() {
  if (comboItems.value.length === 0) {
    ElMessage.warning('套餐至少需要一个菜品')
    return
  }
  if (comboOriginal.value > 0 && Number(comboDish.value.price) > comboOriginal.value) {
    ElMessage.warning(`套餐价不能高于组成菜品原价合计 ¥${money(comboOriginal.value)}`)
    return
  }
  comboSubmitting.value = true
  try {
    await merchantApi.saveComboItems(comboDish.value.id, {
      items: comboItems.value.map((item, index) => ({
        dishId: item.dishId,
        quantity: item.quantity || 1,
        sort: index
      }))
    })
    ElMessage.success('套餐已保存')
    comboVisible.value = false
    await loadDishes()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    comboSubmitting.value = false
  }
}

async function cancelCombo() {
  try {
    await ElMessageBox.confirm('取消后该菜品将变回普通菜品，套餐组成会被清空，确定吗？', '取消套餐', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await merchantApi.markCombo(comboDish.value.id, 0)
    ElMessage.success('已取消套餐')
    comboVisible.value = false
    await loadDishes()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}
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

.low-stock-alert {
  margin-top: 8px;
  width: auto;
  display: inline-flex;
}

.stock-low {
  color: #f56c6c;
  font-weight: 600;
}

.stock-tip {
  margin-left: 4px;
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
.spec-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.combo-tip {
  margin-bottom: 12px;
}

.combo-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.combo-price {
  font-size: 14px;
}

.combo-price b {
  color: #f56c6c;
  font-size: 18px;
  margin-right: 8px;
}

.combo-actions {
  display: flex;
  gap: 10px;
}
</style>
