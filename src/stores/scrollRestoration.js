/**
 * 业务目的：跨路由保存并恢复滚动位置，弥补 keep-alive 场景下 router scrollBehavior 的不足。
 * 业务逻辑：使用 Map 按 path 存储滚动坐标，App.vue 的路由过渡钩子负责写入/读取。
 */
export const scrollRestorationMap = new Map()
