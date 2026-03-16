<template>
  <div class="simple-shipping-demo">
    <a-card title="发货清单">
      <div class="toolbar">
        <a-button type="primary" @click="showModal">新增产品</a-button>
      </div>
      
      <a-table 
        :dataSource="data" 
        :columns="columns" 
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quantity'">
            {{ record.quantity }} {{ record.unit }}
          </template>
          <template v-else-if="column.key === 'cartons'">
            <span v-if="record.cartons">{{ record.cartons }} CARTONS</span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'weight'">
            <div>
              <div>毛重: {{ record.grossWeight.toFixed(2) }} KGS</div>
              <div>净重: {{ record.netWeight.toFixed(2) }} KGS</div>
            </div>
          </template>
          <template v-else-if="column.key === 'volume'">
            <span v-if="record.volume">{{ record.volume.toFixed(2) }} CBM</span>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="visible" title="产品信息" @ok="handleOk">
      <a-form layout="vertical">
        <a-form-item label="产品名称">
          <a-input v-model:value="form.productName" />
        </a-form-item>
        <a-form-item label="数量">
          <a-input-group compact>
            <a-input-number v-model:value="form.quantity" :min="1" style="width: 70%" />
            <a-select v-model:value="form.unit" style="width: 30%">
              <a-select-option value="PCS">PCS</a-select-option>
              <a-select-option value="SETS">SETS</a-select-option>
              <a-select-option value="KILOS">KILOS</a-select-option>
            </a-select>
          </a-input-group>
        </a-form-item>
        <a-form-item label="箱数(可选)">
          <a-input-number v-model:value="form.cartons" :min="0" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="毛重(KGS)">
              <a-input-number v-model:value="form.grossWeight" :min="0" :step="0.01" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="净重(KGS)">
              <a-input-number v-model:value="form.netWeight" :min="0" :step="0.01" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="体积(CBM)(可选)">
          <a-input-number v-model:value="form.volume" :min="0" :step="0.01" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const visible = ref(false)
const data = ref([
  { id: 1, productName: 'PLASTIC SEAL', quantity: 2000, unit: 'PCS', cartons: 1, grossWeight: 1.20, netWeight: 1.14, volume: 0.03 },
  { id: 2, productName: 'RFID STICKER', quantity: 2500, unit: 'PCS', cartons: null, grossWeight: 2.68, netWeight: 2.56, volume: null },
  { id: 3, productName: 'HANGTAG', quantity: 2000, unit: 'PCS', cartons: null, grossWeight: 8.34, netWeight: 7.96, volume: null },
  { id: 4, productName: 'RFID STICKER', quantity: 80000, unit: 'PCS', cartons: 5, grossWeight: 82.70, netWeight: 81.50, volume: 0.15 }
])

const columns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '数量', key: 'quantity' },
  { title: '箱数', key: 'cartons' },
  { title: '重量', key: 'weight' },
  { title: '体积', key: 'volume' }
]

const form = reactive({
  productName: '',
  quantity: 1,
  unit: 'PCS',
  cartons: null,
  grossWeight: 0,
  netWeight: 0,
  volume: null
})

const showModal = () => {
  visible.value = true
}

const handleOk = () => {
  // 简单的数据验证
  if (!form.productName || !form.quantity || !form.grossWeight || !form.netWeight) {
    alert('请填写完整信息')
    return
  }
  
  const newItem = {
    id: Date.now(),
    ...form
  }
  data.value.push(newItem)
  visible.value = false
  
  // 重置表单
  Object.assign(form, {
    productName: '',
    quantity: 1,
    unit: 'PCS',
    cartons: null,
    grossWeight: 0,
    netWeight: 0,
    volume: null
  })
}
</script>

<style scoped>
.simple-shipping-demo {
  padding: 20px;
}
.toolbar {
  margin-bottom: 16px;
}
</style>