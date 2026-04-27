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
let chartConstructor = null
let g2RuntimePromise = null

// 业务目的：用 G2 绘制模块热度占比图，帮助后台快速识别技术文章、看天下和 AI 热点的流量结构。
// 业务逻辑：每次统计数据变化时销毁旧图重建，保证切换模块和容器尺寸变化后图表仍保持稳定展示。
async function renderChart() {
  if (!chartRef.value) {
    return
  }

  await ensureG2Runtime()
  chartInstance?.destroy()
  chartInstance = new chartConstructor({
    container: chartRef.value,
    autoFit: true,
    height: 320
  })

  const safeData = props.moduleStats.length
    ? props.moduleStats.map((item) => ({
        moduleLabel: item.moduleLabel,
        viewCount: item.viewCount,
        commentCount: item.commentCount,
        contentCount: item.contentCount
      }))
    : [{ moduleLabel: '暂无数据', viewCount: 1, commentCount: 0, contentCount: 0 }]

  chartInstance.coordinate({ type: 'theta', innerRadius: 0.45, outerRadius: 0.88 })
  chartInstance
    .interval()
    .data(safeData)
    .transform({ type: 'stackY' })
    .encode('y', 'viewCount')
    .encode('color', 'moduleLabel')
    .style('stroke', '#ffffff')
    .style('lineWidth', 2)
    .legend('color', {
      position: 'bottom',
      layout: { justifyContent: 'center' }
    })
    .tooltip({
      title: 'moduleLabel',
      items: [
        (datum) => ({ name: '浏览量', value: `${datum.viewCount}` }),
        (datum) => ({ name: '评论量', value: `${datum.commentCount}` }),
        (datum) => ({ name: '内容数', value: `${datum.contentCount}` })
      ]
    })

  chartInstance.render()
}

onMounted(() => {
  renderChart()
})

watch(
  () => props.moduleStats,
  () => {
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  chartInstance?.destroy()
  chartInstance = null
})

// 业务目的：把 G2 运行时延迟到占比图真正渲染时再加载，避免首页管理台包体积膨胀。
// 业务逻辑：首次异步导入后缓存 Chart 构造器，后续趋势轮询和标签切换都直接复用本地运行时。
async function ensureG2Runtime() {
  if (!g2RuntimePromise) {
    g2RuntimePromise = import('@antv/g2').then((module) => {
      chartConstructor = module.Chart
    })
  }
  await g2RuntimePromise
}
</script>

<style scoped>
.admin-chart-canvas {
  width: 100%;
  min-height: 320px;
}
</style>
