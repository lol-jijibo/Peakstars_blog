<template>
  <div ref="chartRef" class="admin-chart-canvas"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  moduleStats: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
let echartsModule = null
let echartsRuntimePromise = null

// 目的: 用 ECharts 输出后台模块级的数据对比图，直观看到内容量、浏览量和评论量的模块差异。
// 逻辑: 统一将模块统计映射成柱线混合图，避免多个维度拆成多张图后破坏后台分析面板的阅读效率。
async function renderChart() {
  if (!chartRef.value) {
    return
  }

  const echarts = await ensureEchartsRuntime()
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const safeData = props.moduleStats.length
    ? props.moduleStats
    : [
        {
          moduleLabel: '暂无数据',
          contentCount: 0,
          viewCount: 0,
          commentCount: 0
        }
      ]

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
      top: 40,
      bottom: 18,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: safeData.map((item) => item.moduleLabel),
      axisTick: {
        show: false
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.24)'
        }
      },
      axisLabel: {
        color: '#94a3b8'
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '内容 / 评论',
        splitLine: {
          lineStyle: {
            color: 'rgba(148, 163, 184, 0.12)'
          }
        },
        axisLabel: {
          color: '#94a3b8'
        }
      },
      {
        type: 'value',
        name: '浏览量',
        splitLine: {
          show: false
        },
        axisLabel: {
          color: '#94a3b8'
        }
      }
    ],
    series: [
      {
        name: '内容量',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: {
          color: '#20d7d4',
          borderRadius: [6, 6, 0, 0]
        },
        data: safeData.map((item) => item.contentCount)
      },
      {
        name: '评论量',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: {
          color: '#f59e0b',
          borderRadius: [6, 6, 0, 0]
        },
        data: safeData.map((item) => item.commentCount)
      },
      {
        name: '浏览量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: '#4da3ff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(77, 163, 255, 0.24)' },
            { offset: 1, color: 'rgba(77, 163, 255, 0.02)' }
          ])
        },
        data: safeData.map((item) => item.viewCount)
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
  () => props.moduleStats,
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

// 目的: 仅在模块图真正渲染时再异步装载 ECharts 运行时，避免后台首页包体无效膨胀。
// 逻辑: 运行时 Promise 只创建一次，后续后台轮询刷新和模块切换直接复用同一份图表库实例。
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
