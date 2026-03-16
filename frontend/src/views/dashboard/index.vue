<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="24">
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="用户总数"
            :value="stats.userCount"
            :value-style="{ color: '#3f8600' }"
            style="margin-right: 50px"
          >
            <template #prefix>
              <user-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="流程实例数"
            :value="stats.processCount"
            :value-style="{ color: '#cf1322' }"
            style="margin-right: 50px"
          >
            <template #prefix>
              <profile-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="待办任务"
            :value="stats.pendingTasks"
            :value-style="{ color: '#1890ff' }"
            style="margin-right: 50px"
          >
            <template #prefix>
              <check-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="今日新增"
            :value="stats.todayNew"
            :value-style="{ color: '#fa8c16' }"
            style="margin-right: 50px"
          >
            <template #prefix>
              <plus-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表和列表区域 -->
    <a-row :gutter="24" style="margin-top: 24px">
      <!-- 流程统计图表 -->
      <a-col :span="12">
        <a-card title="流程类型统计" :bordered="false">
          <div ref="chartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
      
      <!-- 待办任务统计 -->
      <a-col :span="12">
        <a-card title="待办任务分布" :bordered="false">
          <div ref="pieChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
    </a-row>
    
    <a-row :gutter="24" style="margin-top: 24px">
      <!-- 最近流程 -->
      <a-col :span="12">
        <a-card title="最近流程" :bordered="false">
          <a-table :columns="processColumns" :data-source="processData" :pagination="false" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ record.status }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      
      <!-- 待办任务 -->
      <a-col :span="12">
        <a-card title="待办任务" :bordered="false">
          <a-list item-layout="horizontal" :data-source="taskData" :pagination="{ pageSize: 5 }">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :description="item.description">
                  <template #title>
                    <a-button type="link" @click="handleTaskClick(item)">{{ item.title }}</a-button>
                  </template>
                </a-list-item-meta>
                <div>
                  <a-tag :color="getTimeColor(item.time)">{{ item.time }}</a-tag>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
    
    <!-- 快捷操作 -->
    <a-row :gutter="24" style="margin-top: 24px">
      <a-col :span="24">
        <a-card title="快捷操作" :bordered="false">
          <a-space :size="24">
            <a-button type="primary" @click="goToPage('/system/user')">
              <template #icon><user-add-outlined /></template>
              新增用户
            </a-button>
            <a-button @click="goToPage('/workflow/definition')">
              <template #icon><file-add-outlined /></template>
              创建流程
            </a-button>
            <a-button @click="goToPage('/workflow/task')">
              <template #icon><check-circle-outlined /></template>
              处理任务
            </a-button>
            <a-button @click="goToPage('/system/org')">
              <template #icon><apartment-outlined /></template>
              组织管理
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  UserOutlined,
  ProfileOutlined,
  CheckCircleOutlined,
  PlusCircleOutlined,
  UserAddOutlined,
  FileAddOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

// 统计数据
const stats = ref({
  userCount: 112893,
  processCount: 8765,
  pendingTasks: 234,
  todayNew: 12
})

// 图表引用
const chartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()

// 表格列配置
const processColumns = [
  {
    title: '流程名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '发起人',
    dataIndex: 'starter',
    key: 'starter',
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
  },
  {
    title: '开始时间',
    dataIndex: 'startTime',
    key: 'startTime',
  },
]

// 流程数据
const processData = ref([
  {
    key: '1',
    name: '请假审批流程',
    starter: '张三',
    status: '运行中',
    startTime: '2026-03-13 10:00:00',
  },
  {
    key: '2',
    name: '费用报销流程',
    starter: '李四',
    status: '已完成',
    startTime: '2026-03-13 09:30:00',
  },
  {
    key: '3',
    name: '合同审批流程',
    starter: '王五',
    status: '运行中',
    startTime: '2026-03-13 08:45:00',
  },
  {
    key: '4',
    name: '采购申请流程',
    starter: '赵六',
    status: '已终止',
    startTime: '2026-03-13 07:20:00',
  },
  {
    key: '5',
    name: '项目立项流程',
    starter: '钱七',
    status: '运行中',
    startTime: '2026-03-13 06:15:00',
  },
])

// 任务数据
const taskData = ref([
  {
    id: 1,
    title: '部门经理审批 - 请假申请',
    description: '申请人：张三，请假类型：年假，天数：3天',
    time: '2小时前',
    priority: 'high'
  },
  {
    id: 2,
    title: '财务审核 - 费用报销',
    description: '申请人：李四，报销金额：1200元',
    time: '1天前',
    priority: 'medium'
  },
  {
    id: 3,
    title: '法务审查 - 合同审批',
    description: '合同编号：HT20260313001，合同金额：50000元',
    time: '2天前',
    priority: 'low'
  },
  {
    id: 4,
    title: '总经理审批 - 重大项目',
    description: '项目名称：数字化转型项目，预算：200万元',
    time: '3天前',
    priority: 'high'
  },
  {
    id: 5,
    title: '人事专员处理 - 入职手续',
    description: '新员工：孙八，入职岗位：Java开发工程师',
    time: '5天前',
    priority: 'medium'
  }
])

// 方法
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    '运行中': 'green',
    '已完成': 'blue',
    '已终止': 'red',
    '挂起': 'orange'
  }
  return colorMap[status] || 'default'
}

const getTimeColor = (time: string) => {
  if (time.includes('小时')) return 'red'
  if (time.includes('天') && parseInt(time) <= 1) return 'orange'
  return 'default'
}

const handleTaskClick = (task: any) => {
  message.info(`点击了任务: ${task.title}`)
  // 可以跳转到具体任务详情页
  router.push('/workflow/task')
}

const goToPage = (path: string) => {
  router.push(path)
}

// 初始化图表
const initCharts = () => {
  // 柱状图
  if (chartRef.value) {
    const barChart = echarts.init(chartRef.value)
    const barOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: ['请假审批', '费用报销', '合同审批', '采购申请', '项目立项'],
          axisTick: {
            alignWithLabel: true
          }
        }
      ],
      yAxis: [
        {
          type: 'value'
        }
      ],
      series: [
        {
          name: '流程数量',
          type: 'bar',
          barWidth: '60%',
          data: [120, 200, 150, 80, 70],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#1890ff' }
            ])
          }
        }
      ]
    }
    barChart.setOption(barOption)
  }
  
  // 饼图
  if (pieChartRef.value) {
    const pieChart = echarts.init(pieChartRef.value)
    const pieOption = {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '5%',
        left: 'center'
      },
      series: [
        {
          name: '任务分布',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: 48, name: '待审批' },
            { value: 36, name: '处理中' },
            { value: 24, name: '已完成' },
            { value: 12, name: '已拒绝' }
          ]
        }
      ]
    }
    pieChart.setOption(pieOption)
  }
}

// 生命周期
onMounted(() => {
  initCharts()
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    echarts.getInstanceByDom(chartRef.value!)?.resize()
    echarts.getInstanceByDom(pieChartRef.value!)?.resize()
  })
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
}

:deep(.ant-card) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  transition: all 0.3s;
}

:deep(.ant-card:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

:deep(.ant-statistic-title) {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

:deep(.ant-statistic-content) {
  font-size: 24px;
  font-weight: 500;
}

:deep(.ant-list-item) {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-list-item:last-child) {
  border-bottom: none;
}

:deep(.ant-btn-link) {
  padding: 0;
}
</style>