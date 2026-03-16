<template>
  <div class="unit-display-demo">
    <a-card title="产品单位中英文显示演示">
      <a-row :gutter="16">
        <!-- 中文显示示例 -->
        <a-col :span="12">
          <a-card title="中文显示效果" size="small">
            <div class="display-example">
              <h4>HS商品列表（中文显示）</h4>
              <a-table 
                :dataSource="chineseData" 
                :columns="chineseColumns" 
                :pagination="false"
                size="small"
              >
                <template #unit="{ record }">
                  <span v-if="record.unitName">
                    {{ record.unitName }}({{ record.unitCode }})
                  </span>
                  <span v-else>-</span>
                </template>
              </a-table>
            </div>
          </a-card>
        </a-col>

        <!-- 中英文显示示例 -->
        <a-col :span="12">
          <a-card title="中英文显示效果" size="small">
            <div class="display-example">
              <h4>HS商品列表（中英文显示）</h4>
              <a-table 
                :dataSource=" bilingualData" 
                :columns="bilingualColumns" 
                :pagination="false"
                size="small"
              >
                <template #unit="{ record }">
                  <span v-if="record.unitName && record.unitNameEn">
                    {{ record.unitName }}/{{ record.unitNameEn }}({{ record.unitCode }})
                  </span>
                  <span v-else-if="record.unitName">
                    {{ record.unitName }}({{ record.unitCode }})
                  </span>
                  <span v-else>-</span>
                </template>
              </a-table>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-divider />

      <div class="unit-selector-demo">
        <h4>单位选择器演示</h4>
        <a-form layout="vertical">
          <a-form-item label="选择计量单位">
            <a-select
              v-model:value="selectedUnit"
              placeholder="请选择计量单位"
              show-search
              option-filter-prop="label"
              style="width: 300px"
            >
              <a-select-option 
                v-for="unit in unitOptions" 
                :key="unit.unitCode" 
                :value="unit.unitCode"
                :label="`${unit.unitName}/${unit.unitNameEn}(${unit.unitCode})`"
              >
                {{ unit.unitName }}/{{ unit.unitNameEn }}({{ unit.unitCode }})
                <span style="color: #999; margin-left: 8px;">{{ unit.unitType }}</span>
              </a-select-option>
            </a-select>
          </a-form-item>
          
          <a-form-item v-if="selectedUnit">
            <div class="selected-unit-info">
              <strong>当前选中单位：</strong>
              <div class="unit-display">
                <span class="chinese">{{ selectedUnitDetail.unitName }}</span>
                <span class="divider">/</span>
                <span class="english">{{ selectedUnitDetail.unitNameEn }}</span>
                <span class="code">({{ selectedUnitDetail.unitCode }})</span>
                <span class="type">- {{ selectedUnitDetail.unitType }}</span>
              </div>
            </div>
          </a-form-item>
        </a-form>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

// 响应式数据
const selectedUnit = ref('')

// 单位选项数据
const unitOptions = [
  { unitCode: '01', unitName: '个', unitNameEn: 'PCS', unitType: '数量单位' },
  { unitCode: '02', unitName: '台', unitNameEn: 'SETS', unitType: '数量单位' },
  { unitCode: '03', unitName: '套', unitNameEn: 'SUITS', unitType: '数量单位' },
  { unitCode: '04', unitName: '件', unitNameEn: 'ITEMS', unitType: '数量单位' },
  { unitCode: '05', unitName: '只', unitNameEn: 'ONLY', unitType: '数量单位' },
  { unitCode: '11', unitName: '千克', unitNameEn: 'KILOS', unitType: '重量单位' },
  { unitCode: '12', unitName: '克', unitNameEn: 'GRAMS', unitType: '重量单位' },
  { unitCode: '16', unitName: '米', unitNameEn: 'METERS', unitType: '长度单位' },
  { unitCode: '17', unitName: '厘米', unitNameEn: 'CM', unitType: '长度单位' }
]

// 选中单位详情
const selectedUnitDetail = computed(() => {
  return unitOptions.find(unit => unit.unitCode === selectedUnit.value) || {
    unitCode: '',
    unitName: '',
    unitNameEn: '',
    unitType: ''
  }
})

// 中文显示数据
const chineseData = [
  { id: 1, hsCode: '85235210', chineseName: 'RFID标签', unitName: '个', unitCode: '01' },
  { id: 2, hsCode: '85235290', chineseName: 'RFID扫描器', unitName: '台', unitCode: '02' },
  { id: 3, hsCode: '39269090', chineseName: '吊粒', unitName: '个', unitCode: '01' },
  { id: 4, hsCode: '11000000', chineseName: '测试商品', unitName: '', unitCode: '' }
]

// 中英文显示数据
const bilingualData = [
  { id: 1, hsCode: '85235210', chineseName: 'RFID标签', unitName: '个', unitNameEn: 'PCS', unitCode: '01' },
  { id: 2, hsCode: '85235290', chineseName: 'RFID扫描器', unitName: '台', unitNameEn: 'SETS', unitCode: '02' },
  { id: 3, hsCode: '39269090', chineseName: '吊粒', unitName: '个', unitNameEn: 'PCS', unitCode: '01' },
  { id: 4, hsCode: '11000000', chineseName: '测试商品', unitName: '', unitNameEn: '', unitCode: '' }
]

// 表格列配置
const chineseColumns = [
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 100 },
  { title: '中文名称', dataIndex: 'chineseName', key: 'chineseName', width: 120 },
  { title: '计量单位', key: 'unit', slots: { customRender: 'unit' }, width: 120 }
]

const bilingualColumns = [
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 100 },
  { title: '中文名称', dataIndex: 'chineseName', key: 'chineseName', width: 120 },
  { title: '计量单位', key: 'unit', slots: { customRender: 'unit' }, width: 180 }
]
</script>

<style scoped>
.unit-display-demo {
  padding: 20px;
}

.display-example {
  margin-bottom: 20px;
}

.display-example h4 {
  margin-bottom: 12px;
  color: #333;
}

.unit-selector-demo {
  margin-top: 20px;
}

.selected-unit-info {
  padding: 12px;
  background: #f0f8ff;
  border-radius: 6px;
  border: 1px solid #d1e7ff;
}

.unit-display {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 500;
}

.unit-display .chinese {
  color: #1890ff;
}

.unit-display .divider {
  color: #999;
  margin: 0 4px;
}

.unit-display .english {
  color: #52c41a;
}

.unit-display .code {
  color: #fa8c16;
}

.unit-display .type {
  color: #999;
  font-size: 14px;
  margin-left: 8px;
}
</style>