<template>
  <div class="admin-rich-editor">
    <div class="admin-rich-editor-head">
      <strong>正文排版编辑</strong>
      <span>支持标题、字号、加粗、列表、引用、图片、表格、代码块和全屏编辑</span>
    </div>
    <div ref="editorRef" class="admin-rich-editor-shell"></div>
    <p class="admin-rich-editor-tip">
      {{ aiEnabled ? 'AI 已启用，选中文本后可在气泡菜单中调用润色、续写和改写。' : '当前未配置 AI Key，富文本排版能力可正常使用；补齐配置后会自动启用 AI 辅助写作。' }}
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
    default: '输入正文内容，使用标题、引用、列表、图片和表格组织文章结构。'
  }
})

const emit = defineEmits(['update:modelValue'])
const editorRef = ref(null)
const aiEnabled = Boolean(import.meta.env.VITE_ADMIN_AI_MODEL && import.meta.env.VITE_ADMIN_AI_API_KEY)
let editorInstance = null
let editorConstructor = null
let editorRuntimePromise = null
let exitCodeBlockExtension = null
let exitListExtension = null

/**
 * 统一承接后台正文编辑与内容回填。
 * 首次打开时异步加载编辑器，并给新文章注入基础正文骨架。
 */
async function initEditor() {
  if (!editorRef.value) {
    return
  }

  await ensureEditorRuntime()
  editorInstance = new editorConstructor({
    element: editorRef.value,
    content: props.modelValue || createDefaultContent(),
    placeholder: props.placeholder,
    lang: 'zh',
    theme: 'light',
    toolbarSize: 'medium',
    toolbarTipEnable: true,
    draggable: true,
    contentRetention: false,
    toolbarKeys: editorToolbarKeys,
    fontFamily: {
      values: [
        { name: '默认字体', value: '' },
        { name: '苹方 / 微软雅黑', value: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif' },
        { name: '宋体', value: 'SimSun, STSong, serif' },
        { name: '代码字体', value: 'Menlo, Consolas, "Courier New", monospace' }
      ]
    },
    fontSize: {
      defaultValue: 16,
      values: [
        { name: '12', value: 12 },
        { name: '14', value: 14 },
        { name: '16', value: 16 },
        { name: '18', value: 18 },
        { name: '20', value: 20 },
        { name: '24', value: 24 },
        { name: '28', value: 28 },
        { name: '32', value: 32 }
      ]
    },
    lineHeight: {
      values: ['1', '1.25', '1.5', '1.75', '2', '2.5', '3']
    },
    htmlPasteConfig: {
      pasteClean: true,
      removeEmptyParagraphs: true
    },
    image: {
      allowBase64: true,
      defaultSize: 640,
      bubbleMenuEnable: true
    },
    textSelectionBubbleMenu: {
      enable: true,
      items: ['ai', 'bold', 'italic', 'underline', 'strike', 'font-color', 'highlight', 'link']
    },
    onChange(editor) {
      emit('update:modelValue', editor.getHtml())
    },
    onCreateBefore: (editor, extensions) => {
      if (exitCodeBlockExtension) {
        extensions.push(exitCodeBlockExtension)
      }
      if (exitListExtension) {
        extensions.push(exitListExtension)
      }
    },
    ai: buildAiConfig()
  })
}

/**
 * 仅在后台已配置 AI 能力时开启辅助写作。
 * 未配置时返回空配置，保证正文编辑稳定可用。
 */
function buildAiConfig() {
  if (!aiEnabled) {
    return undefined
  }

  return {
    bubblePanelEnable: true,
    bubblePanelModel: 'openai',
    commandsEnable: true,
    bubblePanelMenus: [
      {
        title: '润色表达',
        prompt: '请在不改变原意的前提下，让这段技术文章表达更自然、更专业，并保留原有 HTML 结构。',
        icon: 'sparkles'
      },
      {
        title: '补充细节',
        prompt: '请围绕选中的技术文章片段补充必要背景、实现细节和工程注意事项，保持语气克制专业。',
        icon: 'plus'
      },
      {
        title: '提炼小标题',
        prompt: '请为选中内容提炼一个简洁清晰的小标题，适合技术文章正文结构。',
        icon: 'heading'
      }
    ],
    models: {
      openai: {
        apiKey: import.meta.env.VITE_ADMIN_AI_API_KEY,
        model: import.meta.env.VITE_ADMIN_AI_MODEL,
        customUrl: import.meta.env.VITE_ADMIN_AI_URL || undefined
      }
    }
  }
}

const editorToolbarKeys = [
  'undo',
  'redo',
  'brush',
  'eraser',
  'divider',
  'heading',
  'font-family',
  'font-size',
  'divider',
  'bold',
  'italic',
  'underline',
  'strike',
  'link',
  'code',
  'divider',
  'font-color',
  'highlight',
  'align',
  'line-height',
  'divider',
  'bullet-list',
  'ordered-list',
  'todo',
  'indent-decrease',
  'indent-increase',
  'quote',
  'container',
  'divider',
  'image',
  'table',
  'code-block',
  'hr',
  'divider',
  'fullscreen',
  'ai'
]

/**
 * 编辑器内置 placeholder 已支持灰色占位文字，此处返回空内容即可。
 */
function createDefaultContent() {
  return ''
}

watch(
  () => props.modelValue,
  (nextValue) => {
    if (!editorInstance) {
      return
    }

    const currentHtml = editorInstance.getHtml()
    if ((nextValue || '') !== currentHtml) {
      editorInstance.setContent(nextValue || createDefaultContent())
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

/**
 * 编辑器资源较重，需要按需异步加载。
 * 首次加载后缓存运行时，后续弹窗直接复用。
 * 同时加载 @tiptap/core 用于创建自定义键盘扩展。
 */
async function ensureEditorRuntime() {
  if (!editorRuntimePromise) {
    editorRuntimePromise = Promise.all([
      import('aieditor'),
      import('aieditor/dist/style.css'),
      import('@tiptap/core')
    ]).then(([editorModule, _, tiptapCore]) => {
      editorConstructor = editorModule.AiEditor

      // 创建自定义扩展：在代码块中按 Ctrl+Enter / Cmd+Enter 退出代码块
      const { Extension } = tiptapCore
      exitCodeBlockExtension = Extension.create({
        name: 'aiExitCodeBlock',
        addKeyboardShortcuts() {
          return {
            'Mod-Enter': ({ editor }) => {
              const { selection } = editor.state
              const { $from } = selection
              const node = $from.node()
              if (node.type.name === 'codeBlock') {
                return editor.commands.exitCode()
              }
              return false
            },
          }
        },
      })

      // 创建自定义扩展：在有序/无序列表中按 Ctrl+Enter / Cmd+Enter 退出列表
      // 同时处理标准 Enter 在空列表项上退出列表
      exitListExtension = Extension.create({
        name: 'aiExitList',
        addKeyboardShortcuts() {
          return {
            'Mod-Enter': ({ editor }) => {
              const { selection } = editor.state
              const { $from } = selection
              for (let depth = $from.depth; depth > 0; depth -= 1) {
                if ($from.node(depth).type.name === 'listItem') {
                  return editor.chain().liftListItem('listItem').run()
                }
              }
              return false
            },
            'Shift-Enter': ({ editor }) => {
              const { selection } = editor.state
              const { $from } = selection
              for (let depth = $from.depth; depth > 0; depth -= 1) {
                const nodeAtDepth = $from.node(depth)
                if (nodeAtDepth.type.name === 'listItem') {
                  // Shift+Enter 在列表项内做软换行，但如果是空列表项则退出列表
                  if (nodeAtDepth.textContent.length === 0) {
                    return editor.chain().liftListItem('listItem').run()
                  }
                  return editor.commands.enter()
                }
              }
              return false
            },
            Enter: ({ editor }) => {
              const { selection } = editor.state
              const { $from, empty } = selection
              if (!empty) {
                return false
              }
              for (let depth = $from.depth; depth > 0; depth -= 1) {
                const nodeAtDepth = $from.node(depth)
                if (nodeAtDepth.type.name === 'listItem') {
                  const parent = $from.node(depth - 1)
                  if (parent.type.name !== 'orderedList' && parent.type.name !== 'bulletList') {
                    return false
                  }
                  // 空列表项上按 Enter 直接退出列表
                  if (nodeAtDepth.textContent.length === 0) {
                    return editor.chain().liftListItem('listItem').run()
                  }
                  break
                }
              }
              return false
            },
          }
        },
      })
    })
  }

  await editorRuntimePromise
}
</script>

<style scoped>
.admin-rich-editor {
  display: grid;
  gap: 12px;
}

.admin-rich-editor-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.96), rgba(255, 255, 255, 0.92));
  color: #0f172a;
}

.admin-rich-editor-head strong {
  font-size: 15px;
  font-weight: 700;
}

.admin-rich-editor-head span {
  color: #64748b;
  font-size: 12px;
}

.admin-rich-editor-shell {
  min-height: 560px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 18px;
  background: #eef2f7;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.16);
}

.admin-rich-editor-shell :deep(.aie-container) {
  height: 560px;
  border: 0;
  background: #eef2f7;
}

.admin-rich-editor-shell :deep(aie-header) {
  border-bottom: 1px solid #dbe3ee;
  background: #ffffff;
}

.admin-rich-editor-shell :deep(.aie-content) {
  background: #eef2f7;
  padding: 28px 0 42px;
}

.admin-rich-editor-shell :deep(.ProseMirror) {
  width: min(760px, calc(100% - 64px));
  min-height: 430px;
  margin: 0 auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 56px 64px;
  background: #ffffff;
  color: #111827;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.08);
  font-size: 16px;
  line-height: 1.85;
}

.admin-rich-editor-shell :deep(.ProseMirror h1),
.admin-rich-editor-shell :deep(.ProseMirror h2),
.admin-rich-editor-shell :deep(.ProseMirror h3) {
  color: #0f172a;
  line-height: 1.35;
}

.admin-rich-editor-shell :deep(.ProseMirror blockquote) {
  margin: 18px 0;
  border-left: 4px solid #2563eb;
  border-radius: 8px;
  padding: 12px 16px;
  background: #eff6ff;
  color: #334155;
}

.admin-rich-editor-shell :deep(.ProseMirror pre) {
  border-radius: 10px;
  padding: 16px;
  background: #0f172a;
  color: #e5e7eb;
}

/* 修复代码块在深色背景上的高亮冲突 —— AiEditor 自带的 light 主题 hljs 会叠加浅色背景 */
.admin-rich-editor-shell :deep(.ProseMirror pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.admin-rich-editor-shell :deep(.aie-container .aie-codeblock-wrapper) {
  background: #0f172a;
}

/* 覆盖 hljs 浅色背景，使其在深色 pre 中正确显示 */
.admin-rich-editor-shell :deep(.aie-container .hljs) {
  background: transparent;
  color: #e5e7eb;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-comment),
.admin-rich-editor-shell :deep(.aie-container .hljs-quote) {
  color: #8b949e;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-doctag),
.admin-rich-editor-shell :deep(.aie-container .hljs-keyword),
.admin-rich-editor-shell :deep(.aie-container .hljs-formula) {
  color: #c678dd;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-section),
.admin-rich-editor-shell :deep(.aie-container .hljs-name),
.admin-rich-editor-shell :deep(.aie-container .hljs-selector-tag),
.admin-rich-editor-shell :deep(.aie-container .hljs-subst) {
  color: #e06c75;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-literal) {
  color: #56b6c2;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-string),
.admin-rich-editor-shell :deep(.aie-container .hljs-regexp),
.admin-rich-editor-shell :deep(.aie-container .hljs-addition),
.admin-rich-editor-shell :deep(.aie-container .hljs-attribute),
.admin-rich-editor-shell :deep(.aie-container .hljs-meta .hljs-string) {
  color: #98c379;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-attr),
.admin-rich-editor-shell :deep(.aie-container .hljs-variable),
.admin-rich-editor-shell :deep(.aie-container .hljs-template-variable),
.admin-rich-editor-shell :deep(.aie-container .hljs-type),
.admin-rich-editor-shell :deep(.aie-container .hljs-selector-class),
.admin-rich-editor-shell :deep(.aie-container .hljs-selector-attr),
.admin-rich-editor-shell :deep(.aie-container .hljs-selector-pseudo),
.admin-rich-editor-shell :deep(.aie-container .hljs-number) {
  color: #d19a66;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-symbol),
.admin-rich-editor-shell :deep(.aie-container .hljs-bullet),
.admin-rich-editor-shell :deep(.aie-container .hljs-link),
.admin-rich-editor-shell :deep(.aie-container .hljs-meta),
.admin-rich-editor-shell :deep(.aie-container .hljs-selector-id),
.admin-rich-editor-shell :deep(.aie-container .hljs-title) {
  color: #61aeee;
}

.admin-rich-editor-shell :deep(.aie-container .hljs-built_in),
.admin-rich-editor-shell :deep(.aie-container .hljs-title.class_),
.admin-rich-editor-shell :deep(.aie-container .hljs-class .hljs-title) {
  color: #e6c07b;
}

.admin-rich-editor-shell :deep(.ProseMirror table) {
  width: 100%;
  border-collapse: collapse;
}

.admin-rich-editor-shell :deep(.ProseMirror th),
.admin-rich-editor-shell :deep(.ProseMirror td) {
  border: 1px solid #dbe3ee;
  padding: 10px 12px;
}

.admin-rich-editor-tip {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}
</style>
