<template>
  <div class="page">
    <van-nav-bar title="电子礼簿" />

    <van-card>
      <template #title>{{ activity.title }}</template>
      <template #desc>
        <div>日期：{{ activity.date }}</div>
        <div>地点：{{ activity.location }}</div>
      </template>
    </van-card>

    <van-row gutter="10" class="qr-wrap">
      <van-col span="12">
        <div class="qr-title">微信收款码</div>
        <van-image width="100%" height="140" fit="contain" :src="fullImg(activity.wxQrcodePath)" />
      </van-col>
      <van-col span="12">
        <div class="qr-title">支付宝收款码</div>
        <van-image width="100%" height="140" fit="contain" :src="fullImg(activity.aliQrcodePath)" />
      </van-col>
    </van-row>

    <van-form @submit="submitForm">
      <div v-for="(item, index) in items" :key="index" class="line-box">
        <van-field v-model="item.name" label="姓名" placeholder="必填" required />
        <van-field v-model="item.amount" label="金额" type="number" placeholder="必填" required />
        <van-field v-model="item.relation" label="关系" readonly is-link @click="openRelation(index)" />
        <van-field v-model="item.blessing" label="祝福语" placeholder="可选" />
      </div>

      <van-button block type="primary" plain @click="addItem" :disabled="items.length >= 10">+ 添加一人</van-button>

      <van-field v-model="payerName" label="代付人姓名" placeholder="必填" required />
      <van-field v-model="payerPhone" label="代付人手机号" placeholder="可选" />

      <van-cell title="总金额" :value="total + ' 元'" class="total" />

      <div style="margin: 16px">
        <van-button round block type="success" native-type="submit">提交记录</van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showPicker" position="bottom">
      <van-picker :columns="relations" @confirm="confirmRelation" @cancel="showPicker = false" />
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import axios from 'axios'

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const activity = ref({})
const payerName = ref('')
const payerPhone = ref('')
const items = ref([{ name: '', amount: '', relation: '亲戚', blessing: '' }])
const relations = ['亲戚', '同学', '同事', '朋友', '其他']
const showPicker = ref(false)
const currentIndex = ref(0)

const total = computed(() => items.value.reduce((sum, x) => sum + (Number(x.amount) || 0), 0).toFixed(2))

const fullImg = (path) => (path ? `${apiBase}${path}` : '')

const loadActivity = async () => {
  const { data } = await axios.get(`${apiBase}/api/activity`)
  activity.value = data
}

const openRelation = (index) => {
  currentIndex.value = index
  showPicker.value = true
}

const confirmRelation = ({ selectedValues }) => {
  items.value[currentIndex.value].relation = selectedValues[0]
  showPicker.value = false
}

const addItem = () => {
  if (items.value.length < 10) items.value.push({ name: '', amount: '', relation: '亲戚', blessing: '' })
}

const submitForm = async () => {
  if (!payerName.value) return showFailToast('请填写代付人姓名')
  if (items.value.some((x) => !x.name || !x.amount)) return showFailToast('请填写完整姓名和金额')

  const payload = {
    payerName: payerName.value,
    payerPhone: payerPhone.value,
    items: items.value.map((x) => ({ ...x, amount: Number(x.amount) }))
  }
  const { data } = await axios.post(`${apiBase}/api/records`, payload)
  showSuccessToast(`记录成功！请扫码转账总金额 ${data.total} 元给主人`)
  payerName.value = ''
  payerPhone.value = ''
  items.value = [{ name: '', amount: '', relation: '亲戚', blessing: '' }]
}

onMounted(loadActivity)
</script>

<style scoped>
.page { padding-bottom: 20px; background: #f7f8fa; min-height: 100vh; }
.qr-wrap { padding: 12px; }
.qr-title { text-align: center; margin-bottom: 4px; font-size: 13px; color: #666; }
.line-box { margin: 10px; border-radius: 8px; overflow: hidden; background: #fff; }
.total { font-size: 18px; color: #07c160; font-weight: 700; }
</style>
