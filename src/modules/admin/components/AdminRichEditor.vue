<template>
  <div class="admin-rich-editor">
    <div ref="editorRef" class="admin-rich-editor-shell"></div>
    <p class="admin-rich-editor-tip">
      {{ aiEnabled ? 'AI 已启用，可直接在编辑器气泡菜单中调用润色与续写。' : '当前未配置 AI Key，编辑器可正常使用，补齐配置后会自动启用 AI 能力。' }}
    </p>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入正文内容'
  }
})

const emit = defineEmits(['update:modelValue'])
const editorRef = ref(null)
const aiEnabled = Boolean(import.meta.env.VITE_ADMIN_AI_MODEL && import.meta.env.VITE_ADMIN_AI_API_KEY)
let editorInstance = null
let editorConstructor = null
let editorRuntimePromise = null

// 业务目的：在后台内容弹窗中挂载富文本编辑器，统一承接正文编辑、AI 润色与内容回填。
// 业务逻辑：首次打开时异步加载 AiEditor 运行时与样式，后续继续复用同一份实例构造器，避免把重依赖提前打进管理页主包。
async function initEditor() {
  if (!editorRef.value) {
    return
  }

  await ensureEditorRuntime()
  editorInstance = new editorConstructor({
    element: editorRef.value,
    content: props.modelValue || '<p>请输入正文内容</p>',
    placeholder: props.placeholder,
    theme: 'light',
    onChange(editor) {
      emit('update:modelValue', editor.getHtml())
    },
    ai: buildAiConfig()
  })
}

function buildAiConfig() {
  if (!aiEnabled) {
    return undefined
  }

  return {
    bubblePanelEnable: true,
    bubblePanelModel: 'openai',
    models: {
      openai: {
        apiKey: import.meta.env.VITE_ADMIN_AI_API_KEY,
        model: import.meta.env.VITE_ADMIN_AI_MODEL,
        customUrl: import.meta.env.VITE_ADMIN_AI_URL || undefined
      }
    }
  }
}

watch(
  () => props.modelValue,
  (nextValue) => {
    if (!editorInstance) {
      return
    }

    const currentHtml = editorInstance.getHtml()
    if ((nextValue || '') !== currentHtml) {
      editorInstance.setContent(nextValue || '<p>请输入正文内容</p>')
    }
  }
)

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  editorInstance?.destroy()
  editorInstance = null
})

// 业务目的：仅在真正进入内容编辑时加载编辑器资源，减少管理后台首页的首屏包体积。
// 业务逻辑：首次异步导入成功后缓存 Promise 和构造器，后续弹窗多次打开都走内存复用，不重复拉取资源。
async function ensureEditorRuntime() {
  if (!editorRuntimePromise) {
    editorRuntimePromise = Promise.all([
      import('aieditor'),
      import('aieditor/dist/style.css')
    ]).then(([editorModule]) => {
      editorConstructor = editorModule.AiEditor
    })
  }
  await editorRuntimePromise
}
</script>

<style scoped>
.admin-rich-editor {
  display: grid;
  gap: 10px;
}

.admin-rich-editor-shell {
  min-height: 360px;
  border-radius: 22px;
  overflow: hidden;
}

.admin-rich-editor-tip {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}
</style>
