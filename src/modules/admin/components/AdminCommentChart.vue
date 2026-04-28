<template>
  <div ref="chartRef" class="admin-chart-canvas"></div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  commentRecords: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
let echartsModule = null
let echartsRuntimePromise = null

// 目的: 把后台评论管理的待跟进量和评论总量压缩成一张对比图，帮助运营快速识别高压内容。
// 逻辑: 仅取评论热度最高的前六条记录做横向条形对比，确保在管理侧栏宽度内也能保持可读性。
const topCommentRecords = computed(() => {
  return [...props.commentRecords]
    .sort((left, right) => Number(right.commentCount || 0) - Number(left.commentCount || 0))
    .slice(0, 6)
})

async function renderChart() {
  if (!chartRef.value) {
    return
  }

  const echarts = await ensureEchartsRuntime()
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const labels = topCommentRecords.value.length
    ? topCommentRecords.value.map((item) => truncateTitle(item.contentTitle))
    : ['暂无评论数据']
  const pendingValues = topCommentRecords.value.length
    ? topCommentRecords.value.map((item) => Number(item.pendingCount || 0))
    : [0]
  const commentValues = topCommentRecords.value.length
    ? topCommentRecords.value.map((item) => Number(item.commentCount || 0))
    : [0]

  chartInstance.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(12, 18, 32, 0.94)',
      borderWidth: 0,
      textStyle: {
        color: '#f8fafc'
      }
    },
    legend: {
      top: 0,
      textStyle: {
        color: '#94a3b8'
      }
    },
    grid: {
      left: 18,
      right: 18,
      top: 38,
      bottom: 12,
      containLabel: true
    },
    xAxis: {
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
    yAxis: {
      type: 'category',
      data: labels,
      axisTick: {
        show: false
      },
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#cbd5e1'
      }
    },
    series: [
      {
        name: '待跟进',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: {
          color: '#f97316',
          borderRadius: [0, 6, 6, 0]
        },
        data: pendingValues
      },
      {
        name: '评论总量',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: {
          color: '#4da3ff',
          borderRadius: [0, 6, 6, 0]
        },
        data: commentValues
      }
    ]
  })
}

function truncateTitle(value) {
  const title = String(value || '')
  return title.length > 12 ? `${title.slice(0, 12)}...` : title
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

watch(
  () => props.commentRecords,
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

// 目的: 将评论图的图表库装载时机延后到组件可见阶段，减少后台首页初始化负担。
// 逻辑: 运行时模块只在首次绘图时异步导入一次，后续刷新直接复用缓存实例。
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
  min-height: 300px;
}
</style>
