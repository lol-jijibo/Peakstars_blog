<template>
  <div class="admin-rich-editor">
    <div class="admin-rich-editor-head">
      <strong>正文排版编辑</strong>
      <span>支持标题、字号、加粗、列表、引用、图片、表格、代码块和全屏编辑</span>
    </div>
    <div ref="editorRef" class="admin-rich-editor-shell" :class="{ 'no-ai': !aiEnabled }"></div>
    <p v-if="uploadErrorMessage" class="admin-rich-editor-error">
      {{ uploadErrorMessage }}
    </p>
    <p class="admin-rich-editor-tip">
      {{ aiEnabled ? 'AI 已启用，选中文本后可在气泡菜单中调用润色、续写和改写。' : '当前未配置 AI Key，富文本排版能力可正常使用；补齐配置后会自动启用 AI 辅助写作。' }}
    </p>
    <!-- 复制代码成功弹窗 -->
    <Teleport to="body">
      <Transition name="copy-toast">
        <div v-if="showCopyToast" class="copy-code-toast" @click="showCopyToast = false">
          <div class="copy-code-toast-inner">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            <span>代码已成功复制</span>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { uploadRichTextImage } from '@/modules/admin/api/admin'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  storageType: {
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
const showCopyToast = ref(false)
const uploadErrorMessage = ref('')
let copyToastTimer = null
let uploadErrorTimer = null
let editorInstance = null
let editorConstructor = null
let editorRuntimePromise = null
let exitCodeBlockExtension = null
let exitListExtension = null
let safeCodeCommentExtension = null

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
      allowBase64: false,
      defaultSize: 640,
      bubbleMenuEnable: true,
      uploader: uploadEditorImageToMinio,
      uploaderEvent: {
        onFailed(file, response) {
          showUploadError((response && response.message) || `图片上传失败：${file.name}`)
        },
        onError(file, error) {
          showUploadError(error?.message || `图片上传失败：${file.name}`)
        }
      }
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
      if (safeCodeCommentExtension) {
        extensions.push(safeCodeCommentExtension)
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

/**
 * 业务目的：把后台正文图片统一托管到 MinIO，避免富文本保存时混入本地 base64 图片。
 * 业务逻辑：接管编辑器默认图片上传行为，调用后台正文图片接口并按 AiEditor 约定返回 src。
 */
async function uploadEditorImageToMinio(file) {
  const result = await uploadRichTextImage(file, props.storageType)
  return {
    errorCode: 0,
    data: {
      src: result.url,
      alt: file.name
    }
  }
}

/**
 * 业务目的：在正文图片上传失败时给运营同学明确反馈，避免出现“闪一下就没了”的黑盒体验。
 * 业务逻辑：统一展示上传失败文案并自动消失，方便快速判断是接口异常还是图片本身问题。
 */
function showUploadError(message) {
  uploadErrorMessage.value = message || '图片上传失败'
  if (uploadErrorTimer) clearTimeout(uploadErrorTimer)
  uploadErrorTimer = setTimeout(() => {
    uploadErrorMessage.value = ''
  }, 2600)
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
  if (copyToastTimer) clearTimeout(copyToastTimer)
  if (uploadErrorTimer) clearTimeout(uploadErrorTimer)
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

      // 创建安全扩展：拦截 AiEditor 的代码块 AI 注释/解释按钮点击事件
      // 并注入"复制代码"按钮
      safeCodeCommentExtension = Extension.create({
        name: 'safeCodeComment',
        onCreate() {
          // 延迟到编辑器 DOM 渲染完毕后绑定拦截事件和注入复制按钮
          setTimeout(() => {
            const editorEl = editorInstance?.getNativeElement?.()
              || document.querySelector('.aie-container')
            if (!editorEl) return

            // 拦截"自动注释"按钮点击：阻止原生命令，改用安全逻辑
            editorEl.addEventListener('click', (e) => {
              const commentBtn = e.target.closest('.aie-codeblock-tools-comments')
              if (commentBtn && !aiEnabled) {
                e.stopImmediatePropagation()
                e.preventDefault()
                return
              }
            }, true)

            // 拦截"代码解释"按钮点击
            editorEl.addEventListener('click', (e) => {
              const explainBtn = e.target.closest('.aie-codeblock-tools-explain')
              if (explainBtn && !aiEnabled) {
                e.stopImmediatePropagation()
                e.preventDefault()
                return
              }
            }, true)

            // 监听代码块粘贴事件，自动检测语言
            editorEl.addEventListener('paste', (e) => {
              setTimeout(() => {
                const codeEl = document.activeElement?.closest('.ProseMirror pre code')
                if (!codeEl) return
                const wrapper = codeEl.closest('.aie-codeblock-wrapper')
                if (wrapper) {
                  autoDetectAndSetLanguage(wrapper)
                }
              }, 150)
            }, true)

            // 为每个代码块注入"复制代码"按钮
            injectCopyButtons(editorEl)

            // 观察 DOM 变化，新代码块插入时自动注入复制按钮
            // 使用防抖 + 临时断开 observer 避免注入按钮时触发无限循环
            let observerTimer = null
            let autoDetectTimer = null
            const proseMirror = editorEl.querySelector('.ProseMirror')
            if (proseMirror) {
              const observer = new MutationObserver(() => {
                if (observerTimer) return
                observerTimer = setTimeout(() => {
                  observer.disconnect()
                  injectCopyButtons(editorEl)
                  observerTimer = null
                  observer.observe(proseMirror, { childList: true, subtree: true })
                }, 200)

                // 延迟自动检测语言（在按钮注入之后）
                if (autoDetectTimer) clearTimeout(autoDetectTimer)
                autoDetectTimer = setTimeout(() => {
                  autoDetectAll(editorEl)
                }, 1000)
              })
              observer.observe(proseMirror, { childList: true, subtree: true })
            }
          }, 500)
        },
      })

      /**
       * 根据代码内容自动检测编程语言
       */
      function detectCodeLanguage(code) {
        const text = String(code || '').trim()
        if (!text) return null

        if (/<\/?[a-z][\w-]*(\s[^>]*)?>/i.test(text) && /<\/\w+>/.test(text) && !/^\s*[{[]/.test(text)) return 'HTML'
        if (/^<\?xml/i.test(text) || /xmlns[:=]/i.test(text)) return 'XML'
        try { JSON.parse(text); return 'JSON' } catch {}
        if (/(^|\n)\s*\w[\w.-]*\s*:/m.test(text) && !/[{};]/.test(text) && !/\/\*|\/\/|#include/.test(text)) return 'YAML'
        if (/^#{1,6}\s/m.test(text) || /^\*{3,}$/m.test(text) || /\[.*\]\(.*\)/m.test(text)) return 'Markdown'
        if (/\b(SELECT|INSERT\s+INTO|UPDATE\s+\w+\s+SET|DELETE\s+FROM|CREATE\s+(TABLE|INDEX|VIEW)|ALTER\s+TABLE)\b/i.test(text)) return 'SQL'
        if (/(@media|@import|@keyframes|@supports|@font-face)\b/.test(text)) return 'CSS'
        if (/(^|\n)\s*[.#@][\w-]+(\s+[\w-]+)*\s*\{/m.test(text) && /:\s*[^;]+;/.test(text)) return 'CSS'
        if (/\bdef\s+\w+\s*\(/.test(text) || /\bimport\s+\w+/.test(text) || /\bfrom\s+\w+\s+import\b/.test(text) || /\belif\s+|else:\s*$/m.test(text) || /\bprint\s*\(/.test(text) && !/[{;}]/.test(text)) return 'Python'
        if (/\bpackage\s+\w/.test(text) || /\bimport\s+(static\s+)?[\w.]+\.\*?\s*;/.test(text) || /\bpublic\s+static\s+void\s+main\s*\(/.test(text) || /\bSystem\.out\./.test(text) || /\b@Override\b/.test(text)) return 'Java'
        if (/\bfunc\s+\w+\s*\(/.test(text) || /\bpackage\s+main\b/.test(text) || /:=/.test(text) && /\bfmt\./.test(text) || /\bgo\s+func\b/.test(text) || /\bdefer\s+\w/.test(text)) return 'Go'
        if (/\bfn\s+\w+\s*[<(]/.test(text) || /\blet\s+mut\b/.test(text) || /\bimpl\s+\w/.test(text) || /\bpub\s+fn\b/.test(text) || /println!\s*\(/.test(text)) return 'Rust'
        if (/\busing\s+System\b/.test(text) || /\bnamespace\s+\w/.test(text) || /\bConsole\.Write(Line)?\s*\(/.test(text) || /\bvar\s+\w+\s*=\s*new\s+\w+/.test(text)) return 'C#'
        if (/<\?php/i.test(text) || /\$\w+\s*=\s*/.test(text) || /\$\w+->/.test(text)) return 'PHP'
        if (/\binterface\s+\w+\s*\{/.test(text) || /\btype\s+\w+\s*=\s*/.test(text) || /:\s*(string|number|boolean|void|any|never|Promise)\b/.test(text) || /\benum\s+\w+\s*\{/.test(text)) return 'TypeScript'
        if (/#include\s*[<"]/.test(text)) return /\b(std::|cout|cin|class\s+\w+\s*\{|template\s*<|vector\s*<|unique_ptr)\b/.test(text) ? 'C++' : 'C'
        if (/^#!\/(bin|usr\/bin)\/(bash|sh|zsh|env)/m.test(text) || /\b(echo|cd|mkdir|rm|curl|wget|chmod|grep|sed|awk|npm|yarn|pnpm)\s+/m.test(text) || /\$\{[A-Z_]+\}/.test(text)) return 'Shell'
        if (/\b(const|let|var)\s+\w+\s*=/.test(text) || /\bfunction\s+\w+\s*\(/.test(text) || /\bimport\s+.*\s+from\s+['"]/.test(text) || /\bexport\s+(default|const|function|class)\b/.test(text) || /\bconsole\.log\b/.test(text) || /\bdocument\./.test(text) || /\bwindow\./.test(text) || /\brequire\s*\(/.test(text) || /\baddEventListener\b/.test(text) || /\bnew\s+Promise\b/.test(text) || /\bsetTimeout\b|\bsetInterval\b/.test(text)) return 'JavaScript'

        return null
      }

      /**
       * 尝试为代码块自动设置检测到的编程语言
       */
      function autoDetectAndSetLanguage(wrapper) {
        const codeEl = wrapper.querySelector('pre code') || wrapper.querySelector('pre')
        if (!codeEl) return
        const text = codeEl.textContent || ''
        if (text.trim().length < 3) return

        const langTool = wrapper.querySelector('.aie-codeblock-tools-lang')
        if (!langTool) return

        const select = langTool.querySelector('select')
        if (select) {
          const currentValue = (select.value || '').toLowerCase().trim()
          if (currentValue && currentValue !== 'plain text' && currentValue !== 'plaintext' && currentValue !== 'text' && currentValue !== 'tx' && currentValue !== 'auto' && currentValue !== '') {
            return
          }
        } else {
          const currentText = (langTool.textContent || '').toLowerCase().trim()
          if (currentText && !/plain\s*text|text|tx|plain/i.test(currentText)) {
            return
          }
        }

        const detectedLang = detectCodeLanguage(text)
        if (!detectedLang || detectedLang === 'Plain Text') return

        if (select) {
          const detectedLower = detectedLang.toLowerCase()
          const options = Array.from(select.options)
          const matchByValue = options.find((opt) => (opt.value || '').toLowerCase() === detectedLower)
          const matchByText = options.find((opt) => (opt.textContent || '').toLowerCase().trim() === detectedLower)
          const matchByPartial = options.find((opt) => {
            const v = (opt.value || '').toLowerCase()
            const t = (opt.textContent || '').toLowerCase().trim()
            return v.includes(detectedLower) || detectedLower.includes(v) || t.includes(detectedLower) || detectedLower.includes(t)
          })
          const match = matchByValue || matchByText || matchByPartial
          if (match) {
            select.value = match.value
            select.dispatchEvent(new Event('change', { bubbles: true }))
          }
        } else {
          const options = langTool.querySelectorAll('[data-language], [data-lang], [data-value], option, button')
          const detectedLower = detectedLang.toLowerCase()
          for (const opt of options) {
            const langValue = (opt.dataset?.language || opt.dataset?.lang || opt.dataset?.value || opt.value || opt.textContent || '').toLowerCase().trim()
            if (langValue === detectedLower || langValue.includes(detectedLower) || detectedLower.includes(langValue)) {
              opt.click()
              return
            }
          }
          langTool.click()
          setTimeout(() => {
            const dropdownOptions = document.querySelectorAll('.aie-dropdown-item, .aie-codeblock-tools-lang-list [data-language], .aie-codeblock-lang-item, [data-lang]')
            for (const opt of dropdownOptions) {
              const langValue = (opt.dataset?.language || opt.dataset?.lang || opt.dataset?.value || opt.textContent || '').toLowerCase().trim()
              if (langValue === detectedLower || langValue.includes(detectedLower) || detectedLower.includes(langValue)) {
                opt.click()
                return
              }
            }
            langTool.click()
          }, 100)
        }
      }

      /**
       * 为所有代码块工具栏注入"复制代码"按钮
       */
      function injectCopyButtons(container) {
        const wrappers = container.querySelectorAll('.aie-codeblock-wrapper')
        wrappers.forEach((wrapper) => {
          const toolsBar = wrapper.querySelector('.aie-codeblock-tools')
          // 已有复制按钮则跳过
          if (!toolsBar || toolsBar.querySelector('.aie-codeblock-tools-copy')) return

          const copyBtn = document.createElement('div')
          copyBtn.className = 'aie-codeblock-tools-copy'
          copyBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>复制`
          copyBtn.addEventListener('click', (e) => {
            e.stopPropagation()
            e.preventDefault()
            const codeEl = wrapper.querySelector('pre code')
            const codeText = codeEl ? codeEl.textContent || '' : ''
            if (!codeText.trim()) return

            navigator.clipboard.writeText(codeText).then(() => {
              showCopyToast.value = true
              if (copyToastTimer) clearTimeout(copyToastTimer)
              copyToastTimer = setTimeout(() => { showCopyToast.value = false }, 1800)
              copyBtn.classList.add('copied')
              copyBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>已复制`
              setTimeout(() => {
                copyBtn.classList.remove('copied')
                copyBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>复制`
              }, 2000)
            }).catch(() => {
              const textarea = document.createElement('textarea')
              textarea.value = codeText
              textarea.style.cssText = 'position:fixed;left:-9999px'
              document.body.appendChild(textarea)
              textarea.select()
              document.execCommand('copy')
              document.body.removeChild(textarea)
              showCopyToast.value = true
              if (copyToastTimer) clearTimeout(copyToastTimer)
              copyToastTimer = setTimeout(() => { showCopyToast.value = false }, 1800)
            })
          })

          // 插入到语言选择器前面
          const langSelector = toolsBar.querySelector('.aie-codeblock-tools-lang')
          if (langSelector) {
            toolsBar.insertBefore(copyBtn, langSelector)
          } else {
            toolsBar.appendChild(copyBtn)
          }

          // 自动检测代码块语言
          autoDetectAndSetLanguage(wrapper)
        })
      }

      /**
       * 自动检测所有代码块的语言
       */
      function autoDetectAll(container) {
        const wrappers = container.querySelectorAll('.aie-codeblock-wrapper')
        wrappers.forEach((wrapper) => {
          autoDetectAndSetLanguage(wrapper)
        })
      }
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

/* AI 未启用时，隐藏代码块的"自动注释"和"代码解释"按钮
   这些按钮依赖 AI 接口，点击时 AiEditor 会先删除代码块再调用 AI，
   如果 AI 不可用则代码块内容会永久丢失 */
.admin-rich-editor-shell.no-ai :deep(.aie-codeblock-tools-comments),
.admin-rich-editor-shell.no-ai :deep(.aie-codeblock-tools-explain) {
  display: none !important;
}

/* 代码块复制按钮样式 */
.admin-rich-editor-shell :deep(.aie-codeblock-tools-copy) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  margin-right: 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: #8b949e;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
  white-space: nowrap;
}
.admin-rich-editor-shell :deep(.aie-codeblock-tools-copy:hover) {
  background: rgba(255, 255, 255, 0.2);
  color: #e5e7eb;
}
.admin-rich-editor-shell :deep(.aie-codeblock-tools-copy.copied) {
  color: #10b981;
}

/* 复制成功居中弹窗 */
.copy-code-toast {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  pointer-events: auto;
  background: transparent;
}
.copy-code-toast-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: rgba(15, 23, 42, 0.88);
  backdrop-filter: blur(8px);
  color: #f1f5f9;
  font-size: 15px;
  font-weight: 500;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.24);
  letter-spacing: 0.3px;
}
.copy-code-toast-inner svg {
  flex-shrink: 0;
}

/* 弹窗过渡动画 */
.copy-toast-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.copy-toast-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.copy-toast-enter-from {
  opacity: 0;
  transform: scale(0.85);
}
.copy-toast-leave-to {
  opacity: 0;
  transform: scale(0.9);
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

.admin-rich-editor-error {
  margin: -2px 0 0;
  font-size: 12px;
  color: #dc2626;
}
</style>
