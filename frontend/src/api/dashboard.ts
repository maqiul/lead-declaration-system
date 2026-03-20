import request from '@/utils/request'

export interface DashboardStats {
  userCount: number
  processInstanceCount: number
  pendingTaskCount: number
  todayNewCount: number
}

export interface PieChartItem {
  value: number
  name: string
}

export interface DashboardCharts {
  processChart: {
    categories: string[]
    seriesData: number[]
  }
  taskPieChart: {
    seriesData: PieChartItem[]
  }
}

// 获取大屏顶部统计指标卡片
export function getDashboardStats() {
  return request({
    url: '/dashboard/stats',
    method: 'get'
  })
}

// 获取大屏 ECharts 图表数据
export function getDashboardCharts() {
  return request({
    url: '/dashboard/charts',
    method: 'get'
  })
}
