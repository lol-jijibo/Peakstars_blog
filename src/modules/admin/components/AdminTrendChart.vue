<template>
  <div ref="chartRef" class="admin-chart-canvas"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  trendPoints: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
let echartsModule = null
let echartsRuntimePromise = null

// 业务目的：把后台趋势点渲染成平滑折线和面积图，直观看到在线、浏览和编辑指标的实时变化。
// 业务逻辑：组件首次挂载时再异步加载 ECharts，后续只更新 option，避免重图表库提前进入页面主包。
async function renderChart() {
  if (!chartRef.value) {
    return
  }

  const echarts = await ensureEchartsRuntime()
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const labels = props.trendPoints.map((item) => item.timeLabel)
  const onlineUsers = props.trendPoints.map((item) => item.onlineUsers)
  const totalViews = props.trendPoints.map((item) => item.totalViews)
  const editsToday = props.trendPoints.map((item) => item.editsToday)

  chartInstance.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(12, 18, 32, 0.92)',
      borderWidth: 0,
      textStyle: {
        color: '#f8fafc'
      }
    },
    grid: {
      left: 18,
      right: 18,
      top: 24,
      bottom: 24,
      containLabel: true
    },
    legend: {
      top: 0,
      right: 0,
      textStyle: {
        color: '#94a3b8'
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.24)'
        }
      },
      axisLabel: {
        color: '#94a3b8'
      }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.12)'
        }
      },
      axisLabel: {
        color: '#94a3b8'
      }
    },
    series: [
      {
        name: '在线人数',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: onlineUsers,
        lineStyle: { width: 3, color: '#f97316' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(249, 115, 22, 0.30)' },
            { offset: 1, color: 'rgba(249, 115, 22, 0.02)' }
          ])
        }
      },
      {
        name: '总浏览',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: totalViews,
        lineStyle: { width: 2, color: '#0ea5e9' }
      },
      {
        name: '今日编辑',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: editsToday,
        lineStyle: { width: 2, color: '#22c55e' }
      }
    ]
  })
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

watch(
  () => props.trendPoints,
  () => {
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})

// 业务目的：只在趋势图真正渲染时装载 ECharts 运行时，控制后台页入口体积。
// 业务逻辑：缓存异步模块 Promise，页面后续轮询刷新只复用同一份图表库实例，不重复请求资源。
async function ensureEchartsRuntime() {
  if (!echartsRuntimePromise) {
    echartsRuntimePromise = import('echarts').then((module) => {
      echartsModule = module
      return module
    })
  }
  return echartsModule || echartsRuntimePromise
}
</script>

<style scoped>
.admin-chart-canvas {
  width: 100%;
  min-height: 320px;
}
</style>
