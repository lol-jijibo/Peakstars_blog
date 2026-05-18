<template>
  <div class="bk-imp">
    <aside class="bk-imp-sidebar">
      <div class="bk-imp-brand">
        <div class="bk-imp-brand-icon">B</div>
        <div class="bk-imp-brand-text">书籍导入中心</div>
      </div>

      <nav class="bk-imp-nav">
        <button class="bk-imp-nav-item" :class="{ active: activePanel === 'import' }" type="button" @click="activePanel = 'import'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          <span>导入书籍</span>
        </button>
        <button class="bk-imp-nav-item" :class="{ active: activePanel === 'history' }" type="button" @click="activePanel = 'history'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>导入记录</span>
        </button>
        <button class="bk-imp-nav-item" :class="{ active: activePanel === 'review' }" type="button" :disabled="!activeJob" @click="openReviewPanel">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          <span>审核发布</span>
          <span v-if="activeJob" class="bk-imp-nav-badge">{{ chapters.length }}</span>
        </button>
      </nav>

    </aside>

    <main ref="mainRef" class="bk-imp-main">
      <section class="bk-imp-top-stats" aria-label="书籍导入统计">
        <button
          v-for="item in topStats"
          :key="item.key"
          class="bk-imp-top-stat"
          :class="'tone-' + item.tone"
          type="button"
          @click="openHistory(item.filter)"
        >
          <span class="bk-imp-top-stat-icon">
            <svg v-if="item.icon === 'book'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z"/></svg>
            <svg v-else-if="item.icon === 'check'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6 9 17l-5-5"/></svg>
            <svg v-else-if="item.icon === 'edit'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          </span>
          <span class="bk-imp-top-stat-body">
            <span class="bk-imp-top-stat-label">{{ item.label }}</span>
            <span class="bk-imp-top-stat-value">{{ item.value }}</span>
            <span class="bk-imp-top-stat-desc">{{ item.desc }}</span>
          </span>
        </button>
      </section>

      <div v-if="errorMessage" class="bk-imp-toast bk-imp-toast-error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="errorMessage = ''">&times;</button>
      </div>

      <transition name="bk-imp-toast-fade">
        <div v-if="successMessage" class="bk-imp-toast bk-imp-toast-success bk-imp-toast-floating">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <span>{{ successMessage }}</span>
          <button type="button" @click="successMessage = ''">&times;</button>
        </div>
      </transition>

      <!-- ====== DELETE CONFIRM MODAL ====== -->
      <transition name="bk-imp-modal-fade">
        <div v-if="deleteModal.visible" class="bk-imp-modal-overlay" @click.self="cancelDeleteModal">
          <div class="bk-imp-modal">
            <div class="bk-imp-modal-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#f74f4f" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            </div>
            <div class="bk-imp-modal-title">{{ deleteModal.title }}</div>
            <div class="bk-imp-modal-body">{{ deleteModal.body }}</div>
            <div v-if="deleteModal.items.length" class="bk-imp-modal-items">
              <span v-for="(item, idx) in deleteModal.items.slice(0, 8)" :key="idx" class="bk-imp-modal-item-tag">{{ item }}</span>
              <span v-if="deleteModal.items.length > 8" class="bk-imp-modal-item-tag bk-imp-modal-item-more">等 {{ deleteModal.items.length }} 项</span>
            </div>
            <div class="bk-imp-modal-actions">
              <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="cancelDeleteModal">取消</button>
              <button class="bk-imp-btn bk-imp-btn-danger" type="button" :disabled="deleteModal.loading" @click="confirmDeleteModal">
                {{ deleteModal.loading ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- ====== IMPORT PANEL ====== -->
      <template v-if="activePanel === 'import'">
        <header class="bk-imp-header">
          <h1>导入书籍</h1>
          <p>支持 EPUB、PDF、TXT、Markdown、DOCX、HTML 等主流格式，单文件限制 200MB</p>
        </header>

        <div class="bk-imp-cards">
          <div class="bk-imp-card bk-imp-card-upload" @dragover.prevent @drop.prevent="handleDrop">
            <div class="bk-imp-card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            </div>
            <div class="bk-imp-card-title">本地上传</div>
            <div class="bk-imp-card-desc">拖拽或点击选择文件</div>
            <div class="bk-imp-format-tags">
              <span class="bk-imp-format-tag tag-epub">EPUB</span>
              <span class="bk-imp-format-tag tag-pdf">PDF</span>
              <span class="bk-imp-format-tag">TXT</span>
              <span class="bk-imp-format-tag">MD</span>
              <span class="bk-imp-format-tag">DOCX</span>
              <span class="bk-imp-format-tag">HTML</span>
              <span class="bk-imp-format-tag">ZIP</span>
            </div>
            <input ref="fileInputRef" type="file" accept=".epub,.pdf,.txt,.md,.docx,.html,.htm,.zip" class="bk-imp-hidden-input" @change="handleFileSelect" />
            <button class="bk-imp-btn bk-imp-btn-primary" type="button" :disabled="uploading" @click="openFilePicker">
              <svg v-if="uploading" class="bk-imp-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
              {{ uploading ? '上传解析中...' : '选择文件上传' }}
            </button>
          </div>

          <div class="bk-imp-card bk-imp-card-external">
            <div class="bk-imp-card-icon bk-imp-card-icon-alt">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
            </div>
            <div class="bk-imp-card-title">外部资源</div>
            <div class="bk-imp-card-desc">从 URL 或 API 导入</div>
            <div class="bk-imp-external-form">
              <input v-model.trim="externalUrl" type="url" placeholder="输入资源 URL 地址" class="bk-imp-input" @keydown.enter="importFromExternal" />
              <input v-model.trim="externalTitle" type="text" placeholder="书籍标题（选填）" class="bk-imp-input" />
              <button class="bk-imp-btn bk-imp-btn-accent" type="button" :disabled="importingExternal || !externalUrl" @click="importFromExternal">
                {{ importingExternal ? '拉取中...' : '导入资源' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Active Job Progress -->
        <div v-if="activeJob" class="bk-imp-progress-section">
          <div class="bk-imp-progress-header">
            <div class="bk-imp-progress-info">
              <span class="bk-imp-progress-title">{{ activeJob.title }}</span>
              <span class="bk-imp-progress-status" :class="'status-' + activeJob.status">{{ resolveJobStatus(activeJob.status) }}</span>
            </div>
            <span class="bk-imp-progress-meta">{{ activeJob.originalFormat?.toUpperCase() || 'ZIP' }} · {{ formatFileSize(activeJob.fileSize) }} · {{ activeJob.totalChapters || 0 }} 章</span>
          </div>
          <div class="bk-imp-progress-bar">
            <div class="bk-imp-progress-fill" :style="{ width: (activeJob.progress || 0) + '%' }"></div>
          </div>
          <div class="bk-imp-progress-footer">
            <span>{{ activeJob.message || '处理中' }}</span>
            <div class="bk-imp-progress-actions">
              <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="reloadActiveJob">刷新状态</button>
              <button v-if="activeJob.status === 'await_review'" class="bk-imp-btn bk-imp-btn-primary" type="button" @click="openReviewPanel">前往审核</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ====== HISTORY PANEL ====== -->
      <template v-if="activePanel === 'history'">
        <header class="bk-imp-header">
          <h1>导入记录</h1>
          <div class="bk-imp-header-actions">
            <div class="bk-imp-filter-tabs" aria-label="导入记录筛选">
              <button
                v-for="option in historyFilterOptions"
                :key="option.key"
                class="bk-imp-filter-tab"
                :class="{ active: historyStatusFilter === option.key }"
                type="button"
                @click="historyStatusFilter = option.key"
              >
                <span>{{ option.label }}</span>
                <span class="bk-imp-filter-count">{{ option.count }}</span>
              </button>
            </div>
            <div class="bk-imp-category-delete-wrap">
              <button class="bk-imp-btn bk-imp-btn-danger bk-imp-btn-sm" type="button" @click="showCategoryDeleteDropdown = !showCategoryDeleteDropdown">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                分类删除
              </button>
              <div v-if="showCategoryDeleteDropdown" class="bk-imp-category-delete-dropdown">
                <div class="bk-imp-category-delete-title">选择要删除的分类</div>
                <button
                  v-for="cat in importJobCategories"
                  :key="cat"
                  type="button"
                  class="bk-imp-category-delete-option"
                  @click="confirmCategoryDelete(cat)"
                >
                  {{ cat }}
                </button>
                <button type="button" class="bk-imp-category-delete-option bk-imp-category-delete-cancel" @click="showCategoryDeleteDropdown = false">取消</button>
              </div>
            </div>
            <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="reloadHistory">{{ historyLoading ? '加载中...' : '刷新' }}</button>
          </div>
        </header>

        <div v-if="batchableImportJobs.length" class="bk-imp-batch-bar">
          <label class="bk-imp-select-all">
            <input type="checkbox" :checked="allBatchableSelected" @change="toggleSelectAllVisible($event.target.checked)" />
            <span>已选 {{ selectedJobKeys.length }} / {{ batchableImportJobs.length }}</span>
          </label>
          <div class="bk-imp-batch-actions">
            <button class="bk-imp-btn bk-imp-btn-accent bk-imp-btn-sm" type="button" :disabled="batchOperating || !selectedAwaitReviewKeys.length" @click="batchApproveSelected">
              {{ batchOperating ? '处理中...' : '批量通过' }}
            </button>
            <button class="bk-imp-btn bk-imp-btn-ghost bk-imp-btn-sm" type="button" :disabled="batchOperating || !selectedReviewableKeys.length" @click="batchRejectSelected">
              批量拒绝
            </button>
            <button class="bk-imp-btn bk-imp-btn-primary bk-imp-btn-sm" type="button" :disabled="batchOperating || !selectedPublishableKeys.length" @click="batchPublishSelected">
              批量发布 {{ selectedPublishableKeys.length }}
            </button>
            <button class="bk-imp-btn bk-imp-btn-danger bk-imp-btn-sm" type="button" :disabled="batchOperating || !selectedJobKeys.length" @click="batchDeleteSelected">
              {{ batchOperating ? '删除中...' : '批量删除' }} {{ selectedJobKeys.length }}
            </button>
          </div>
        </div>

        <div v-if="filteredImportJobs.length" class="bk-imp-history-list">
          <div v-for="job in filteredImportJobs" :key="job.jobKey" class="bk-imp-history-item" @click="selectJobAndReview(job)">
            <label v-if="isBatchableJob(job)" class="bk-imp-row-check" @click.stop>
              <input type="checkbox" :checked="isJobSelected(job.jobKey)" @change="toggleJobSelection(job.jobKey, $event.target.checked)" />
            </label>
            <div class="bk-imp-history-left">
              <div v-if="getDisplayCoverUrl(job)" class="bk-imp-history-cover">
                <img :src="getDisplayCoverUrl(job)" :alt="`${job.title || '导入书籍'}封面`" />
              </div>
              <div v-else class="bk-imp-history-format">{{ resolveFormatLabel(job.originalFormat || job.importType) }}</div>
            </div>
            <div class="bk-imp-history-center">
              <div class="bk-imp-history-title">{{ job.title || '未命名' }}</div>
              <div class="bk-imp-history-meta">
                <span>{{ job.author || '未知作者' }}</span>
                <span v-if="job.translator">译者：{{ job.translator }}</span>
                <span>{{ job.totalChapters || 0 }} 章</span>
                <span>{{ formatFileSize(job.fileSize) }}</span>
                <span>{{ resolveFormatLabel(job.originalFormat || job.importType) }}</span>
                <span>{{ job.createdAt }}</span>
              </div>
            </div>
            <div class="bk-imp-history-right">
              <span class="bk-imp-progress-status" :class="'status-' + job.status">{{ resolveJobStatus(job.status) }}</span>
              <div class="bk-imp-history-actions">
                <button v-if="job.status === 'await_review' || job.status === 'approved' || job.status === 'published'" class="bk-imp-btn bk-imp-btn-sm" type="button" @click.stop="selectJobAndReview(job)">
                  {{ job.status === 'published' ? '修改' : '审核' }}
                </button>
                <button v-if="job.status === 'deleted'" class="bk-imp-btn bk-imp-btn-primary bk-imp-btn-sm" type="button" :disabled="restoringJobKey === job.jobKey" @click.stop="handleRestoreJob(job)">
                  {{ restoringJobKey === job.jobKey ? '恢复中...' : '恢复原始状态' }}
                </button>
                <button v-if="job.status !== 'deleted'" class="bk-imp-btn bk-imp-btn-sm" type="button" :disabled="repairingCoverKey === job.jobKey" @click.stop="handleRepairCover(job)">
                  {{ repairingCoverKey === job.jobKey ? '修复中...' : '修复封面' }}
                </button>
                <button v-if="job.status !== 'deleted'" class="bk-imp-btn bk-imp-btn-danger bk-imp-btn-sm" type="button" @click.stop="confirmDeleteJob(job)">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="bk-imp-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <p>{{ importJobs.length ? '当前筛选下暂无记录' : '暂无导入记录' }}</p>
        </div>
      </template>

      <!-- ====== REVIEW PANEL ====== -->
      <template v-if="activePanel === 'review'">
        <header class="bk-imp-header">
          <h1>审核发布</h1>
          <div class="bk-imp-header-actions">
            <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="reloadActiveJob">刷新</button>
            <button class="bk-imp-btn bk-imp-btn-accent" type="button" :disabled="savingPage || !hasPageDrafts" @click="savePageDrafts">
              {{ savingPage ? '保存中...' : savePageButtonText }}
            </button>
            <button class="bk-imp-btn bk-imp-btn-primary" type="button" :disabled="publishing || !canPublish" @click="publishCurrentJob">
              {{ publishing ? '发布中...' : '发布整本书' }}
            </button>
          </div>
        </header>

        <div ref="reviewPanelRef" class="bk-imp-review-shell">
          <transition name="bk-imp-review-fade" mode="out-in">
            <div v-if="activeJob && reviewReady" key="review" class="bk-imp-review">
              <div class="bk-imp-review-meta">
                <div class="bk-imp-review-meta-row">
              <label class="bk-imp-review-label" for="book-title-input">书名</label>
              <div class="bk-imp-combo">
                <input
                  id="book-title-input"
                  v-model.trim="metadataForm.title"
                  class="bk-imp-input"
                  type="text"
                  placeholder="请输入书名"
                  @focus="openMetadataDropdown('title')"
                  @click="openMetadataDropdown('title')"
                  @input="openMetadataDropdown('title')"
                />
                <div v-if="activeMetadataDropdown === 'title' && titleOptions.length" class="bk-imp-combo-menu">
                  <button v-for="option in titleOptions" :key="option" type="button" class="bk-imp-combo-option" @mousedown.prevent="selectMetadataOption('title', option)">
                    {{ option }}
                  </button>
                </div>
              </div>
            </div>
                <div class="bk-imp-review-meta-row">
              <label class="bk-imp-review-label" for="book-author-input">作者</label>
              <div class="bk-imp-combo">
                <input
                  id="book-author-input"
                  v-model.trim="metadataForm.author"
                  class="bk-imp-input"
                  type="text"
                  placeholder="未识别时可手动填写"
                  @focus="openMetadataDropdown('author')"
                  @click="openMetadataDropdown('author')"
                  @input="openMetadataDropdown('author')"
                />
                <div v-if="activeMetadataDropdown === 'author' && authorOptions.length" class="bk-imp-combo-menu">
                  <button v-for="option in authorOptions" :key="option" type="button" class="bk-imp-combo-option" @mousedown.prevent="selectMetadataOption('author', option)">
                    {{ option }}
                  </button>
                </div>
              </div>
            </div>
                <div class="bk-imp-review-meta-row">
              <label class="bk-imp-review-label" for="book-translator-input">译者</label>
              <div class="bk-imp-combo">
                <input
                  id="book-translator-input"
                  v-model.trim="metadataForm.translator"
                  class="bk-imp-input"
                  type="text"
                  placeholder="没有译者就填无"
                  @focus="openMetadataDropdown('translator')"
                  @click="openMetadataDropdown('translator')"
                  @input="openMetadataDropdown('translator')"
                />
                <div v-if="activeMetadataDropdown === 'translator' && translatorOptions.length" class="bk-imp-combo-menu">
                  <button v-for="option in translatorOptions" :key="option" type="button" class="bk-imp-combo-option" @mousedown.prevent="selectMetadataOption('translator', option)">
                    {{ option }}
                  </button>
                </div>
              </div>
            </div>
                <div class="bk-imp-review-meta-row">
              <label class="bk-imp-review-label" for="book-category-select">分类</label>
              <div class="bk-imp-combo">
                <button
                  id="book-category-select"
                  type="button"
                  class="bk-imp-input bk-imp-select-trigger"
                  :class="{ active: activeMetadataDropdown === 'category' }"
                  aria-haspopup="listbox"
                  :aria-expanded="activeMetadataDropdown === 'category' ? 'true' : 'false'"
                  @click="openMetadataDropdown('category')"
                >
                  <span class="bk-imp-select-trigger-value">{{ metadataForm.category }}</span>
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m7 10 5 5 5-5"></path>
                  </svg>
                </button>
                <div v-if="activeMetadataDropdown === 'category'" class="bk-imp-combo-menu" role="listbox" aria-label="分类选项">
                  <button
                    v-for="option in bookCategoryOptions"
                    :key="option"
                    type="button"
                    class="bk-imp-combo-option"
                    :class="{ active: metadataForm.category === option }"
                    @mousedown.prevent="selectMetadataOption('category', option)"
                  >
                    {{ option }}
                  </button>
                </div>
              </div>
            </div>
                <div class="bk-imp-review-meta-row">
              <span class="bk-imp-review-label">章节</span>
              <span>{{ chapters.length }} 章 · {{ totalReviewWords }} 字</span>
            </div>
                <div class="bk-imp-review-meta-row">
              <span class="bk-imp-review-label">格式</span>
              <span>{{ resolveFormatLabel(activeJob.originalFormat || activeJob.importType) }}</span>
            </div>
                <div class="bk-imp-review-meta-row bk-imp-review-cover-row">
              <span class="bk-imp-review-label">封面</span>
              <div class="bk-imp-cover-editor">
                <input
                  ref="coverFileInputRef"
                  class="bk-imp-hidden-input"
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp,image/bmp,image/svg+xml"
                  @change="handleCoverFileSelect"
                />
                <button class="bk-imp-cover-preview" type="button" @click="triggerCoverSelect">
                  <img v-if="validMetadataCoverUrl" :src="validMetadataCoverUrl" alt="书籍封面预览" />
                  <span v-else>自动识别或上传封面</span>
                </button>
                <div class="bk-imp-cover-fields">
                  <div class="bk-imp-cover-file" :class="{ empty: !metadataForm.coverUrl }" :title="metadataForm.coverUrl || '未上传封面'">
                    {{ coverFileName }}
                  </div>
                  <div class="bk-imp-cover-actions">
                    <button class="bk-imp-btn bk-imp-btn-accent bk-imp-btn-sm" type="button" :disabled="coverUploading" @click="triggerCoverSelect">
                      {{ coverUploading ? '上传中...' : '上传封面' }}
                    </button>
                    <button class="bk-imp-btn bk-imp-btn-ghost bk-imp-btn-sm" type="button" :disabled="!metadataForm.coverUrl || coverUploading" @click="removeCover">
                      移除
                    </button>
                  </div>
                </div>
              </div>
            </div>
              </div>

              <div class="bk-imp-review-body">
                <div class="bk-imp-chapter-nav">
                  <div class="bk-imp-chapter-nav-head">章节目录</div>
                  <button
                    v-for="(chapter, idx) in chapters"
                    :key="chapter.tempChapterKey"
                    class="bk-imp-chapter-item"
                    :class="{ active: selectedChapterKey === chapter.tempChapterKey, edited: chapter.reviewStatus === 'edited' }"
                    type="button"
                    @click="selectChapter(chapter.tempChapterKey)"
                  >
                    <span class="bk-imp-chapter-idx">{{ String(idx + 1).padStart(2, '0') }}</span>
                    <div class="bk-imp-chapter-info">
                      <span class="bk-imp-chapter-title">{{ chapter.title }}</span>
                      <span class="bk-imp-chapter-sub">{{ chapter.wordCount || 0 }} 字</span>
                    </div>
                    <span v-if="pendingChapterSaves[chapter.tempChapterKey]" class="bk-imp-chapter-draft-dot"></span>
                    <span v-if="chapter.reviewStatus === 'edited'" class="bk-imp-chapter-edited-dot"></span>
                  </button>
                </div>

                <div class="bk-imp-editor-panel">
                  <div v-if="selectedChapter" class="bk-imp-editor">
                    <div class="bk-imp-editor-top">
                      <span class="bk-imp-editor-chapter-label">CH {{ String(selectedChapter.chapterNo).padStart(2, '0') }}</span>
                      <div class="bk-imp-editor-top-actions">
                        <button class="bk-imp-btn bk-imp-btn-ghost bk-imp-btn-sm" type="button" @click="resetChapterForm">恢复</button>
                        <button class="bk-imp-btn bk-imp-btn-ghost bk-imp-btn-sm" type="button" @click="toggleChapterContentView">
                          {{ chapterContentToggleText }}
                        </button>
                        <button class="bk-imp-btn bk-imp-btn-accent bk-imp-btn-sm" type="button" :disabled="savingChapter || !hasChapterDrafts" @click="savePendingChapters">
                          {{ savingChapter ? '保存中...' : '保存修改' }}
                        </button>
                        <button class="bk-imp-btn bk-imp-btn-primary bk-imp-btn-sm" type="button" :disabled="savingChapter" @click="goNextChapter">
                          下一章
                        </button>
                      </div>
                    </div>
                    <div class="bk-imp-editor-fields">
                      <input v-model.trim="chapterForm.title" type="text" placeholder="章节标题" class="bk-imp-input" />
                      <input v-model.trim="chapterForm.subtitle" type="text" placeholder="副标题（选填）" class="bk-imp-input" />
                    </div>
                    <div v-if="chapterContentView === 'preview'" class="bk-imp-content-preview">
                      <div v-if="previewContentHtml" class="bk-imp-content-preview-body" v-html="previewContentHtml"></div>
                      <div v-else class="bk-imp-content-preview-empty">暂无正文内容</div>
                    </div>
                    <textarea v-else v-model="chapterForm.contentHtml" rows="20" class="bk-imp-textarea" placeholder="正文 HTML"></textarea>

                    <div v-if="selectedChapter.warnings?.length" class="bk-imp-warnings">
                      <div class="bk-imp-warnings-head">预处理告警</div>
                      <div v-for="w in selectedChapter.warnings" :key="w" class="bk-imp-warning-item">{{ w }}</div>
                    </div>
                  </div>
                  <div v-else class="bk-imp-empty bk-imp-empty-sm">
                    <p>选择左侧章节进行审核编辑</p>
                  </div>
                </div>
              </div>
            </div>
            <div v-else-if="activeJob" key="loading" class="bk-imp-review-skeleton">
              <div class="bk-imp-skeleton-meta">
                <span class="bk-imp-skeleton-line w-25"></span>
                <span class="bk-imp-skeleton-line w-38"></span>
                <span class="bk-imp-skeleton-line w-20"></span>
                <span class="bk-imp-skeleton-line w-22"></span>
                <span class="bk-imp-skeleton-line w-18"></span>
                <span class="bk-imp-skeleton-line w-30"></span>
              </div>
              <div class="bk-imp-skeleton-body">
                <div class="bk-imp-skeleton-panel">
                  <span class="bk-imp-skeleton-line w-70"></span>
                  <span class="bk-imp-skeleton-line w-90"></span>
                  <span class="bk-imp-skeleton-line w-56"></span>
                </div>
                <div class="bk-imp-skeleton-panel">
                  <span class="bk-imp-skeleton-line w-48"></span>
                  <span class="bk-imp-skeleton-line w-92"></span>
                  <span class="bk-imp-skeleton-line w-84"></span>
                  <span class="bk-imp-skeleton-line w-60"></span>
                </div>
              </div>
            </div>
            <div v-else key="empty" class="bk-imp-empty">
              <p>请先从导入记录中选择一个待审核任务</p>
            </div>
          </transition>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createBookImportJob,
  createBookImportJobFromFile,
  createBookImportJobFromExternal,
  getBookImportJob,
  getBookImportJobChapters,
  listRecentImportJobs,
  approveBookImportJobs,
  publishBookImportJob,
  publishBookImportJobs,
  rejectBookImportJobs,
  updateBookImportJobMetadata,
  updateBookImportJobChapter,
  updateBookImportJobChapters,
  getAdminBooks,
  deleteImportJob,
  restoreImportJob,
  batchDeleteImportJobs,
  deleteImportJobsByCategory,
  repairImportJobCover
} from '@/modules/admin/api/bookAdmin'
import { uploadCoverImage } from '@/modules/admin/api/admin'

const props = defineProps({
  standalone: { type: Boolean, default: true },
  subModule: { type: String, default: 'import' }
})

const router = useRouter()
const route = useRoute()

const fileInputRef = ref(null)
const coverFileInputRef = ref(null)
const mainRef = ref(null)
const activePanel = ref('import')
const uploading = ref(false)
const coverUploading = ref(false)
const importingExternal = ref(false)
const publishing = ref(false)
const savingPage = ref(false)
const savingMetadata = ref(false)
const savingChapter = ref(false)
const historyLoading = ref(false)
const batchOperating = ref(false)
const deletingJob = ref(false)
const repairingCoverKey = ref('')
const restoringJobKey = ref('')
const showCategoryDeleteDropdown = ref(false)
const deleteModal = reactive({
  visible: false,
  title: '',
  body: '',
  items: [],
  loading: false,
  onConfirm: null
})
const reviewPanelRef = ref(null)
const reviewReady = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
let successMessageTimer = null
const activeJob = ref(null)
const chapters = ref([])
const selectedChapterKey = ref('')
const selectedJobKeys = ref([])
const importJobs = ref([])
const books = ref([])
const historyStatusFilter = ref('all')
const externalUrl = ref('')
const externalTitle = ref('')
const activeMetadataDropdown = ref('')
const bookCategoryOptions = ['精品书籍', '历史', '文学', '悬疑', '人物传记', '名家代表']
const metadataForm = reactive({ title: '', author: '', translator: '无', category: '精品书籍', coverUrl: '' })
const chapterForm = reactive({ title: '', subtitle: '', contentHtml: '' })
const pendingChapterSaves = reactive({})
const chapterContentView = ref('preview')

const selectedChapter = computed(() =>
  chapters.value.find((c) => c.tempChapterKey === selectedChapterKey.value) || null
)

const chapterChanged = computed(() => {
  if (!selectedChapter.value) return false
  return chapterForm.title !== (selectedChapter.value.title || '') ||
    chapterForm.subtitle !== (selectedChapter.value.subtitle || '') ||
    chapterForm.contentHtml !== (selectedChapter.value.contentHtml || '')
})

const pendingChapterSaveCount = computed(() => Object.keys(pendingChapterSaves).length)

const chapterSaveButtonCount = computed(() =>
  pendingChapterSaveCount.value + (chapterChanged.value && !pendingChapterSaves[selectedChapter.value?.tempChapterKey] ? 1 : 0)
)

const hasChapterDrafts = computed(() => pendingChapterSaveCount.value > 0 || chapterChanged.value)

const hasPageDrafts = computed(() => metadataChanged.value || hasChapterDrafts.value)

const chapterContentToggleText = computed(() =>
  chapterContentView.value === 'preview' ? '查看源码' : '查看效果'
)

const previewContentHtml = computed(() =>
  normalizePreviewChapterHtml(chapterForm.contentHtml)
)

const validMetadataCoverUrl = computed(() => normalizeCoverUrl(metadataForm.coverUrl))

const coverFileName = computed(() => {
  const coverUrl = String(metadataForm.coverUrl || '').trim()
  if (!coverUrl) return '未上传封面'
  if (!normalizeCoverUrl(coverUrl)) return '封面地址无效，请修复或重新上传'
  const normalizedUrl = coverUrl.split('?')[0].split('#')[0]
  const segments = normalizedUrl.split('/').filter(Boolean)
  return segments.length ? decodeURIComponent(segments[segments.length - 1]) : '封面文件'
})

const savePageButtonText = computed(() => {
  if (!hasPageDrafts.value) return '无需保存'
  const parts = []
  if (metadataChanged.value) parts.push('书籍信息')
  if (hasChapterDrafts.value) parts.push(`${chapterSaveButtonCount.value} 章`)
  return `保存修改 ${parts.join(' + ')}`
})

const canPublish = computed(() =>
  Boolean(activeJob.value?.jobKey && isPublishableStatus(activeJob.value?.status))
)

const totalReviewWords = computed(() =>
  chapters.value.reduce((sum, c) => sum + (c.wordCount || 0), 0)
)

const pendingReviewJobs = computed(() =>
  importJobs.value.filter((item) => item.status === 'await_review' || item.status === 'approved')
)

const activeImportJobs = computed(() =>
  importJobs.value.filter((item) => item.status !== 'deleted')
)

const deletedJobs = computed(() =>
  importJobs.value.filter((item) => item.status === 'deleted')
)

const batchableImportJobs = computed(() =>
  filteredImportJobs.value.filter((item) => isBatchableJob(item))
)

const selectedJobs = computed(() =>
  importJobs.value.filter((item) => selectedJobKeys.value.includes(item.jobKey))
)

const selectedAwaitReviewKeys = computed(() =>
  selectedJobs.value.filter((item) => item.status === 'await_review').map((item) => item.jobKey)
)

const selectedReviewableKeys = computed(() =>
  selectedJobs.value.filter((item) => isReviewableStatus(item.status)).map((item) => item.jobKey)
)

const selectedPublishableKeys = computed(() =>
  selectedJobs.value.filter((item) => isPublishableStatus(item.status)).map((item) => item.jobKey)
)

const allBatchableSelected = computed(() =>
  batchableImportJobs.value.length > 0 &&
  batchableImportJobs.value.every((item) => selectedJobKeys.value.includes(item.jobKey))
)

const failedJobs = computed(() =>
  importJobs.value.filter((item) => item.status === 'failed')
)

const publishedJobs = computed(() =>
  importJobs.value.filter((item) => item.status === 'published')
)

const topStats = computed(() => [
  { key: 'all', label: '总导入', value: activeImportJobs.value.length, tone: 'total', filter: 'all', icon: 'book', desc: '全部导入任务' },
  { key: 'published', label: '已发布', value: publishedJobs.value.length, tone: 'published', filter: 'published', icon: 'check', desc: '已上线书籍' },
  { key: 'review', label: '待审核', value: pendingReviewJobs.value.length, tone: 'review', filter: 'await_review', icon: 'edit', desc: '等待审核发布' },
  { key: 'failed', label: '失败', value: failedJobs.value.length, tone: 'failed', filter: 'failed', icon: 'alert', desc: '需要重新处理' }
])

const historyFilterOptions = computed(() => [
  { key: 'all', label: '全部', count: activeImportJobs.value.length },
  { key: 'published', label: '已发布', count: publishedJobs.value.length },
  { key: 'await_review', label: '待审核', count: pendingReviewJobs.value.length },
  { key: 'rejected', label: '已拒绝', count: importJobs.value.filter((item) => item.status === 'rejected').length },
  { key: 'failed', label: '失败', count: failedJobs.value.length },
  { key: 'deleted', label: '已删除', count: deletedJobs.value.length }
])

const importJobCategories = computed(() => {
  const categories = new Set(importJobs.value.map((item) => item.category).filter(Boolean))
  return [...categories].sort()
})

const importJobStatusOrder = {
  await_review: 0,
  approved: 0,
  published: 1,
  rejected: 3
}

const filteredImportJobs = computed(() => {
  let visibleJobs = activeImportJobs.value
  if (historyStatusFilter.value === 'all') {
    visibleJobs = activeImportJobs.value
  } else if (historyStatusFilter.value === 'await_review') {
    visibleJobs = importJobs.value.filter((item) => item.status === 'await_review' || item.status === 'approved')
  } else {
    visibleJobs = importJobs.value.filter((item) => item.status === historyStatusFilter.value)
  }
  return [...visibleJobs].sort((current, next) => {
    const currentOrder = importJobStatusOrder[current.status] ?? 2
    const nextOrder = importJobStatusOrder[next.status] ?? 2
    return currentOrder - nextOrder
  })
})

const titleOptions = computed(() =>
  buildUniqueOptions([
    metadataForm.title,
    activeJob.value?.title,
    activeJob.value?.bookKey,
    ...importJobs.value.map((item) => item.title),
    ...books.value.map((item) => item.title)
  ])
)

const authorOptions = computed(() =>
  buildUniqueOptions([
    metadataForm.author,
    activeJob.value?.author,
    '未知作者',
    ...importJobs.value.map((item) => item.author),
    ...books.value.map((item) => item.author)
  ])
)

const translatorOptions = computed(() =>
  buildUniqueOptions([
    metadataForm.translator,
    activeJob.value?.translator,
    '无',
    ...importJobs.value.map((item) => item.translator),
    ...books.value.map((item) => item.translator)
  ])
)

const metadataChanged = computed(() => {
  if (!activeJob.value) return false
  return metadataForm.title !== (activeJob.value.title || '') ||
    metadataForm.author !== (activeJob.value.author || '未知作者') ||
    normalizeTranslator(metadataForm.translator) !== normalizeTranslator(activeJob.value.translator) ||
    metadataForm.category !== normalizeBookCategoryLabel(activeJob.value.category) ||
    metadataForm.coverUrl !== (activeJob.value.coverUrl || '')
})

onMounted(() => {
  reloadHistory()
  reloadBooks()
  openJobFromRouteQuery()
  document.addEventListener('pointerdown', handleMetadataOutsidePointerDown, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleMetadataOutsidePointerDown, true)
  clearSuccessMessageTimer()
})

watch(() => props.subModule, (val) => {
  activePanel.value = val === 'review' ? 'review' : val === 'history' ? 'history' : 'import'
}, { immediate: true })

watch(() => route.fullPath, () => {
  openJobFromRouteQuery()
})

watch(historyStatusFilter, () => {
  selectedJobKeys.value = []
})

watch(activeJob, (job) => {
  if (job) {
    fillMetadataForm(job)
  } else {
    resetMetadataFormValues()
    reviewReady.value = false
  }
})

watch(successMessage, (message) => {
  clearSuccessMessageTimer()
  if (!message) return
  successMessageTimer = window.setTimeout(() => {
    successMessage.value = ''
    successMessageTimer = null
  }, 1200)
})

function openFilePicker() {
  fileInputRef.value?.click()
}

function clearSuccessMessageTimer() {
  if (successMessageTimer) {
    window.clearTimeout(successMessageTimer)
    successMessageTimer = null
  }
}

function showSuccessMessage(message) {
  successMessage.value = message
}

async function openJobFromRouteQuery() {
  const jobKey = typeof route.query.jobKey === 'string' ? route.query.jobKey : ''
  const panel = typeof route.query.panel === 'string' ? route.query.panel : ''
  if (!jobKey && panel !== 'review') return
  if (jobKey && activeJob.value?.jobKey !== jobKey) {
    try {
      activeJob.value = await getBookImportJob(jobKey)
      await loadJobChapters(jobKey)
    } catch (error) {
      errorMessage.value = error.message || '加载导入任务失败'
    }
  }
  activePanel.value = 'review'
  reviewReady.value = Boolean(activeJob.value?.jobKey)
}

async function handleFileSelect(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  await uploadFile(file)
}

async function handleDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (!file) return
  await uploadFile(file)
}

async function uploadFile(file) {
  if (file.size > 200 * 1024 * 1024) {
    errorMessage.value = '文件大小超过 200MB 限制'
    return
  }

  uploading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const ext = file.name.split('.').pop()?.toLowerCase() || ''
    const isZip = ext === 'zip'
    const job = isZip
      ? await createBookImportJob(file)
      : await createBookImportJobFromFile(file)

    activeJob.value = job
    await loadJobChapters(job.jobKey)
    await reloadHistory()
    await reloadBooks()
    successMessage.value = `${file.name} 解析完成，共 ${job.totalChapters || 0} 章`
    activePanel.value = 'review'
  } catch (error) {
    errorMessage.value = error.message || '书籍导入失败'
  } finally {
    uploading.value = false
  }
}

async function importFromExternal() {
  if (!externalUrl.value) return
  importingExternal.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const job = await createBookImportJobFromExternal({
      sourceUrl: externalUrl.value,
      title: externalTitle.value || '',
      importType: 'api'
    })
    activeJob.value = job
    await loadJobChapters(job.jobKey)
    await reloadHistory()
    successMessage.value = '外部资源导入完成'
    externalUrl.value = ''
    externalTitle.value = ''
    activePanel.value = 'review'
  } catch (error) {
    errorMessage.value = error.message || '外部资源导入失败'
  } finally {
    importingExternal.value = false
  }
}

async function reloadActiveJob() {
  if (!activeJob.value?.jobKey) return
  try {
    activeJob.value = await getBookImportJob(activeJob.value.jobKey)
    await loadJobChapters(activeJob.value.jobKey)
  } catch (error) {
    errorMessage.value = error.message || '刷新任务失败'
  }
}

function openMetadataDropdown(field) {
  activeMetadataDropdown.value = field
}

function handleMetadataOutsidePointerDown(event) {
  if (!event.target?.closest?.('.bk-imp-combo')) {
    activeMetadataDropdown.value = ''
  }
  if (!event.target?.closest?.('.bk-imp-category-delete-wrap')) {
    showCategoryDeleteDropdown.value = false
  }
}

function selectMetadataOption(field, option) {
  metadataForm[field] = option
  activeMetadataDropdown.value = ''
}

function getDisplayCoverUrl(job) {
  return normalizeCoverUrl(job?.coverUrl)
}

function normalizeCoverUrl(rawUrl) {
  const coverUrl = String(rawUrl || '').trim()
  if (!coverUrl || /[\r\n<>"']/.test(coverUrl)) return ''
  const lowerUrl = coverUrl.toLowerCase()
  if (lowerUrl.startsWith('data:image/')) return coverUrl
  if (lowerUrl.startsWith('http://') || lowerUrl.startsWith('https://') || coverUrl.startsWith('/')) return coverUrl
  return ''
}

function triggerCoverSelect() {
  coverFileInputRef.value?.click()
}

async function handleCoverFileSelect(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '请选择图片文件'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    errorMessage.value = '封面图片不能超过 10MB'
    return
  }
  coverUploading.value = true
  errorMessage.value = ''
  try {
    const result = await uploadCoverImage(file)
    metadataForm.coverUrl = result.url || ''
    successMessage.value = '封面图片已上传'
  } catch (error) {
    errorMessage.value = error.message || '封面图片上传失败'
  } finally {
    coverUploading.value = false
  }
}

function removeCover() {
  metadataForm.coverUrl = ''
}

function fillMetadataForm(job) {
  metadataForm.title = job.title || ''
  metadataForm.author = job.author || '未知作者'
  metadataForm.translator = normalizeTranslator(job.translator)
  metadataForm.category = normalizeBookCategoryLabel(job.category)
  metadataForm.coverUrl = job.coverUrl || ''
}

function resetMetadataFormValues() {
  metadataForm.title = ''
  metadataForm.author = '未知作者'
  metadataForm.translator = '无'
  metadataForm.category = '精品书籍'
  metadataForm.coverUrl = ''
}

async function saveMetadata(options = {}) {
  if (!activeJob.value?.jobKey) return
  if (!metadataChanged.value) return
  if (!metadataForm.title || !metadataForm.author) {
    errorMessage.value = '书名和作者不能为空'
    return
  }
  savingMetadata.value = true
  errorMessage.value = ''
  try {
    const updated = await updateBookImportJobMetadata(activeJob.value.jobKey, {
      title: metadataForm.title,
      author: metadataForm.author,
      translator: normalizeTranslator(metadataForm.translator),
      category: metadataForm.category,
      coverUrl: metadataForm.coverUrl
    })
    activeJob.value = updated
    await reloadHistory()
    if (!options.silent) {
      successMessage.value = '书籍信息已保存'
    }
  } catch (error) {
    errorMessage.value = error.message || '保存书籍署名失败'
  } finally {
    savingMetadata.value = false
  }
}

async function savePageDrafts(options = {}) {
  if (!activeJob.value?.jobKey) return
  savingPage.value = true
  errorMessage.value = ''
  try {
    const changedMetadata = metadataChanged.value
    const changedChapters = hasChapterDrafts.value
    if (changedMetadata) {
      await saveMetadata({ silent: true })
      if (errorMessage.value) return
    }
    if (changedChapters) {
      await savePendingChapters({ silent: true })
      if (errorMessage.value) return
    }
    if (!options.silent) {
      if (changedMetadata && changedChapters) {
        showSuccessMessage('书籍信息和章节修改已保存')
      } else if (changedMetadata) {
        showSuccessMessage('书籍信息已保存')
      } else if (changedChapters) {
        showSuccessMessage('章节修改已保存')
      }
    }
  } finally {
    savingPage.value = false
  }
}

async function loadJobChapters(jobKey) {
  const list = await getBookImportJobChapters(jobKey)
  chapters.value = Array.isArray(list) ? list : []
  clearPendingChapterSaves()
  if (chapters.value.length) {
    selectedChapterKey.value = chapters.value[0].tempChapterKey
    fillChapterForm(chapters.value[0])
  } else {
    selectedChapterKey.value = ''
    resetChapterFormValues()
  }
}

async function selectChapter(tempChapterKey) {
  if (tempChapterKey === selectedChapterKey.value) return
  if (chapterChanged.value) {
    queueCurrentChapterSave()
  }
  selectedChapterKey.value = tempChapterKey
  const chapter = chapters.value.find((c) => c.tempChapterKey === tempChapterKey)
  if (chapter) fillChapterForm(chapter)
}

function fillChapterForm(chapter) {
  const draft = pendingChapterSaves[chapter.tempChapterKey]
  chapterForm.title = draft?.title ?? chapter.title ?? ''
  chapterForm.subtitle = draft?.subtitle ?? chapter.subtitle ?? ''
  chapterForm.contentHtml = draft?.contentHtml ?? chapter.contentHtml ?? ''
}

function resetChapterForm() {
  if (selectedChapter.value) {
    fillChapterForm(selectedChapter.value)
  } else {
    resetChapterFormValues()
  }
}

function resetChapterFormValues() {
  chapterForm.title = ''
  chapterForm.subtitle = ''
  chapterForm.contentHtml = ''
}

function toggleChapterContentView() {
  chapterContentView.value = chapterContentView.value === 'preview' ? 'source' : 'preview'
}

function normalizePreviewChapterHtml(contentHtml) {
  const rawHtml = String(contentHtml || '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .trim()

  return applyCoverFallbackToPreviewHtml(rawHtml, metadataForm.coverUrl)
}

function applyCoverFallbackToPreviewHtml(contentHtml, coverUrlFallback) {
  const coverUrl = normalizeCoverUrl(coverUrlFallback)
  if (!coverUrl) return String(contentHtml || '')

  return String(contentHtml || '').replace(
    /(\s(?:src|href|xlink:href)\s*=\s*)(["'])([^"']+)\2/gi,
    (fullMatch, prefix, quote, rawUrl) => {
      const url = String(rawUrl || '').trim()
      if (!url) return fullMatch
      const lowerUrl = url.toLowerCase()
      if (lowerUrl.startsWith('data:image/')) return fullMatch

      // 只对“裸文件名”做兜底，避免误伤已经解析好的图片路径
      if (/[\\/]/.test(url) || lowerUrl.startsWith('http://') || lowerUrl.startsWith('https://') || url.startsWith('/')) {
        return fullMatch
      }

      if (!looksLikeCoverFileName(url)) return fullMatch
      return `${prefix}${quote}${coverUrl}${quote}`
    }
  )
}

function looksLikeCoverFileName(rawUrl) {
  const url = String(rawUrl || '').trim()
  if (!url) return false
  if (/[\r\n<>"']/.test(url)) return false
  if (!/^[^\\/]+?\.(png|jpe?g|gif|webp|svg)$/i.test(url)) return false

  const lower = url.toLowerCase().split('?')[0].split('#')[0]
  const name = lower.replace(/\.(png|jpe?g|gif|webp|svg)$/i, '')
  // 仅对封面相关文件名做兜底，避免把其它章节图片错误替换成封面
  return name === 'cover' || name.startsWith('cover')
}

function queueCurrentChapterSave() {
  if (!selectedChapter.value || !chapterChanged.value) return
  pendingChapterSaves[selectedChapter.value.tempChapterKey] = {
    tempChapterKey: selectedChapter.value.tempChapterKey,
    title: chapterForm.title,
    subtitle: chapterForm.subtitle,
    contentHtml: chapterForm.contentHtml,
    sortOrder: selectedChapter.value.sortOrder
  }
}

function clearPendingChapterSaves() {
  for (const key of Object.keys(pendingChapterSaves)) {
    delete pendingChapterSaves[key]
  }
}

async function savePendingChapters(options = {}) {
  if (!activeJob.value?.jobKey) return
  if (chapterChanged.value) {
    queueCurrentChapterSave()
  }
  const drafts = Object.values(pendingChapterSaves)
  if (!drafts.length) return
  savingChapter.value = true
  errorMessage.value = ''
  try {
    const updatedList = drafts.length === 1
      ? [await updateBookImportJobChapter(activeJob.value.jobKey, drafts[0].tempChapterKey, drafts[0])]
      : await updateBookImportJobChapters(activeJob.value.jobKey, drafts)
    const updatedMap = new Map(updatedList.map((item) => [item.tempChapterKey, item]))
    chapters.value = chapters.value.map((chapter) => updatedMap.get(chapter.tempChapterKey) || chapter)
    clearPendingChapterSaves()
    if (selectedChapter.value) {
      fillChapterForm(selectedChapter.value)
    }
    if (!options.silent) {
      successMessage.value = `已保存 ${updatedList.length} 章修改`
    }
  } catch (error) {
    errorMessage.value = error.message || '保存章节失败'
  } finally {
    savingChapter.value = false
  }
}

async function goNextChapter() {
  if (!selectedChapter.value) return
  if (chapterChanged.value) {
    queueCurrentChapterSave()
  }
  const currentIndex = chapters.value.findIndex((item) => item.tempChapterKey === selectedChapterKey.value)
  const nextChapter = chapters.value[currentIndex + 1]
  if (nextChapter) {
    selectedChapterKey.value = nextChapter.tempChapterKey
    fillChapterForm(nextChapter)
  } else {
    successMessage.value = '已经是最后一章'
  }
}

function isReviewableStatus(status) {
  return status === 'await_review' || status === 'approved'
}

function isPublishableStatus(status) {
  return status === 'await_review' || status === 'approved'
}

function isBatchableJob(job) {
  return Boolean(job?.jobKey && isReviewableStatus(job.status))
}

function isJobSelected(jobKey) {
  return selectedJobKeys.value.includes(jobKey)
}

function toggleJobSelection(jobKey, checked) {
  if (!jobKey) return
  const next = new Set(selectedJobKeys.value)
  if (checked) {
    next.add(jobKey)
  } else {
    next.delete(jobKey)
  }
  selectedJobKeys.value = Array.from(next)
}

function toggleSelectAllVisible(checked) {
  const next = new Set(selectedJobKeys.value)
  for (const job of batchableImportJobs.value) {
    if (checked) {
      next.add(job.jobKey)
    } else {
      next.delete(job.jobKey)
    }
  }
  selectedJobKeys.value = Array.from(next)
}

async function batchApproveSelected() {
  await runBatchOperation(
    selectedAwaitReviewKeys.value,
    approveBookImportJobs,
    (count) => `已通过 ${count} 本待审核书籍`
  )
}

async function batchRejectSelected() {
  if (!selectedReviewableKeys.value.length) return
  const reason = window.prompt('请输入拒绝原因（可留空）', '内容暂不适合发布') || ''
  await runBatchOperation(
    selectedReviewableKeys.value,
    (keys) => rejectBookImportJobs(keys, reason),
    (count) => `已拒绝 ${count} 本书籍`
  )
}

async function batchPublishSelected() {
  await runBatchOperation(
    selectedPublishableKeys.value,
    publishBookImportJobs,
    (count) => `已发布 ${count} 本书籍`
  )
}

function openDeleteModal({ title, body, items, onConfirm }) {
  deleteModal.visible = true
  deleteModal.title = title || '确认删除'
  deleteModal.body = body || ''
  deleteModal.items = items || []
  deleteModal.loading = false
  deleteModal.onConfirm = onConfirm
}

function cancelDeleteModal() {
  deleteModal.visible = false
  deleteModal.loading = false
  deleteModal.onConfirm = null
}

async function confirmDeleteModal() {
  if (!deleteModal.onConfirm) return
  deleteModal.loading = true
  try {
    await deleteModal.onConfirm()
  } finally {
    deleteModal.visible = false
    deleteModal.loading = false
    deleteModal.onConfirm = null
  }
}

async function batchDeleteSelected() {
  const keys = selectedJobKeys.value
  if (!keys.length) return
  const jobNames = selectedJobs.value.map((j) => j.title || '未命名')
  openDeleteModal({
    title: '批量删除导入记录',
    body: `确认删除以下 ${keys.length} 条导入记录？已发布的书籍将同时被删除，此操作不可撤销。`,
    items: jobNames,
    onConfirm: async () => {
      batchOperating.value = true
      errorMessage.value = ''
      successMessage.value = ''
      try {
        const result = await batchDeleteImportJobs(keys)
        selectedJobKeys.value = []
        await reloadBooks()
        await reloadHistory()
        if (activeJob.value?.jobKey) {
          try {
            activeJob.value = await getBookImportJob(activeJob.value.jobKey)
          } catch {
            activeJob.value = null
          }
        }
        const successCount = result?.successCount || 0
        const failedCount = result?.failedCount || 0
        successMessage.value = failedCount
          ? `已移入已删除 ${successCount} 条记录，${failedCount} 条处理失败`
          : `已移入已删除 ${successCount} 条记录`
      } catch (error) {
        errorMessage.value = error.message || '批量删除失败'
      } finally {
        batchOperating.value = false
      }
    }
  })
}

async function handleRepairCover(job) {
  repairingCoverKey.value = job.jobKey
  errorMessage.value = ''
  try {
    const updated = await repairImportJobCover(job.jobKey)
    if (activeJob.value?.jobKey === job.jobKey) {
      activeJob.value = updated
      fillMetadataForm(updated)
    }
    await reloadHistory()
    successMessage.value = '封面已从源文件恢复'
  } catch (error) {
    errorMessage.value = error.message || '封面恢复失败'
  } finally {
    repairingCoverKey.value = ''
  }
}

async function handleRestoreJob(job) {
  restoringJobKey.value = job.jobKey
  errorMessage.value = ''
  try {
    const restored = await restoreImportJob(job.jobKey)
    if (activeJob.value?.jobKey === job.jobKey) {
      activeJob.value = restored
      fillMetadataForm(restored)
    }
    await reloadBooks()
    await reloadHistory()
    successMessage.value = `已恢复「${job.title || '未命名'}」的原始状态`
  } catch (error) {
    errorMessage.value = error.message || '恢复原始状态失败'
  } finally {
    restoringJobKey.value = ''
  }
}

async function confirmDeleteJob(job) {
  const title = job.title || '未命名'
  const hasBook = job.bookKey && job.status === 'published'
  const body = hasBook
    ? '该记录已发布，删除后会进入已删除列表并暂时下线前台书籍，可在已删除中恢复原始状态。'
    : '删除后会进入已删除列表，可在已删除中恢复原始状态。'
  openDeleteModal({
    title: `删除「${title}」`,
    body,
    items: [],
    onConfirm: async () => {
      deletingJob.value = true
      errorMessage.value = ''
      try {
        await deleteImportJob(job.jobKey)
        if (activeJob.value?.jobKey === job.jobKey) {
          activeJob.value = null
        }
        await reloadBooks()
        await reloadHistory()
        successMessage.value = `已将「${title}」移入已删除`
      } catch (error) {
        errorMessage.value = error.message || '删除失败'
      } finally {
        deletingJob.value = false
      }
    }
  })
}

async function confirmCategoryDelete(category) {
  showCategoryDeleteDropdown.value = false
  const count = activeImportJobs.value.filter((item) => item.category === category).length
  if (!count) {
    errorMessage.value = `分类「${category}」下暂无导入记录`
    return
  }
  openDeleteModal({
    title: `按分类删除`,
    body: `确认将分类「${category}」下的所有 ${count} 条导入记录移入已删除？已发布书籍会暂时下线，可在已删除中恢复原始状态。`,
    items: [category],
    onConfirm: async () => {
      batchOperating.value = true
      errorMessage.value = ''
      successMessage.value = ''
      try {
        const deletedCount = await deleteImportJobsByCategory(category)
        selectedJobKeys.value = selectedJobKeys.value.filter((key) => {
          const job = importJobs.value.find((j) => j.jobKey === key)
          return job?.category !== category
        })
        if (activeJob.value?.jobKey) {
          try {
            activeJob.value = await getBookImportJob(activeJob.value.jobKey)
          } catch {
            activeJob.value = null
          }
        }
        await reloadHistory()
        successMessage.value = `已将分类「${category}」下 ${deletedCount} 条记录移入已删除`
      } catch (error) {
        errorMessage.value = error.message || '分类删除失败'
      } finally {
        batchOperating.value = false
      }
    }
  })
}

async function runBatchOperation(jobKeys, handler, buildMessage) {
  if (!jobKeys.length) return
  batchOperating.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await handler(jobKeys)
    selectedJobKeys.value = selectedJobKeys.value.filter((key) => !jobKeys.includes(key))
    await reloadBooks()
    await reloadHistory()
    if (activeJob.value?.jobKey) {
      activeJob.value = await getBookImportJob(activeJob.value.jobKey)
    }
    const successCount = result?.successCount || 0
    const failedCount = result?.failedCount || 0
    successMessage.value = failedCount
      ? `${buildMessage(successCount)}，${failedCount} 本处理失败`
      : buildMessage(successCount)
  } catch (error) {
    errorMessage.value = error.message || '批量处理失败'
  } finally {
    batchOperating.value = false
  }
}

async function publishCurrentJob() {
  if (!activeJob.value?.jobKey) return
  publishing.value = true
  errorMessage.value = ''
  try {
    await savePageDrafts({ silent: true })
    if (errorMessage.value) return
    await publishBookImportJob(activeJob.value.jobKey)
    activeJob.value = await getBookImportJob(activeJob.value.jobKey)
    await reloadBooks()
    await reloadHistory()
    successMessage.value = '书籍发布成功'
  } catch (error) {
    errorMessage.value = error.message || '发布书籍失败'
  } finally {
    publishing.value = false
  }
}

async function reloadHistory() {
  historyLoading.value = true
  try {
    const list = await listRecentImportJobs(50)
    importJobs.value = Array.isArray(list) ? list : []
    const availableKeys = new Set(importJobs.value.filter((item) => isBatchableJob(item)).map((item) => item.jobKey))
    selectedJobKeys.value = selectedJobKeys.value.filter((key) => availableKeys.has(key))
  } catch (error) {
    errorMessage.value = error.message || '加载导入记录失败'
  } finally {
    historyLoading.value = false
  }
}

async function reloadBooks() {
  try {
    const list = await getAdminBooks()
    books.value = Array.isArray(list) ? list : []
  } catch {
    // silent
  }
}

async function selectJobAndReview(job) {
  activePanel.value = 'review'
  activeJob.value = job
  reviewReady.value = Boolean(job?.jobKey)
  scrollMainToTop()
  void loadJobChapters(job.jobKey)
}

function openReviewPanel() {
  activePanel.value = 'review'
  reviewReady.value = Boolean(activeJob.value?.jobKey)
  scrollMainToTop()
}

function scrollMainToTop() {
  window.requestAnimationFrame(() => {
    if (mainRef.value) {
      mainRef.value.scrollTop = 0
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  })
}

function openHistory(status = 'all') {
  historyStatusFilter.value = status || 'all'
  activePanel.value = 'history'
}

function normalizeTranslator(value) {
  const text = String(value || '').trim()
  return text || '无'
}

function normalizeBookCategoryLabel(value) {
  const text = String(value || '').trim()
  return bookCategoryOptions.includes(text) ? text : '精品书籍'
}

function buildUniqueOptions(values) {
  const seen = new Set()
  return values
    .map((value) => String(value || '').trim())
    .filter(Boolean)
    .filter((value) => {
      if (seen.has(value)) return false
      seen.add(value)
      return true
    })
    .slice(0, 12)
}

function resolveJobStatus(status) {
  const map = {
    uploaded: '已上传',
    parsing: '解析中',
    await_review: '待审核',
    approved: '待发布',
    rejected: '已拒绝',
    published: '已发布',
    failed: '失败',
    deleted: '已删除'
  }
  return map[status] || status || '未知'
}

function resolveFormatLabel(format) {
  const map = {
    epub: 'EPUB',
    pdf: 'PDF',
    txt: 'TXT',
    md: 'MD',
    markdown: 'MD',
    docx: 'DOCX',
    html: 'HTML',
    htm: 'HTML',
    zip: 'ZIP',
    external: 'URL'
  }
  return map[format] || (format || '').toUpperCase()
}

function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.bk-imp {
  display: flex;
  min-height: 100vh;
  color: #e8eff8;
  background: linear-gradient(180deg, #070e1a 0%, #0d1829 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
}

/* ===== Sidebar ===== */
.bk-imp-sidebar {
  width: 220px;
  min-height: 100vh;
  padding: 28px 16px;
  border-right: 1px solid rgba(134, 163, 196, 0.1);
  background: rgba(6, 12, 22, 0.6);
  display: flex;
  flex-direction: column;
  gap: 28px;
  flex-shrink: 0;
}

.bk-imp-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
}

.bk-imp-brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #7de8ff, #6af7b3);
  color: #082032;
  font-weight: 800;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bk-imp-brand-text {
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
}

.bk-imp-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bk-imp-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: #9eb4d1;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  transition: all 0.15s;
  position: relative;
}

.bk-imp-nav-item:hover {
  background: rgba(125, 232, 255, 0.06);
  color: #e8eff8;
}

.bk-imp-nav-item.active {
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
}

.bk-imp-nav-item:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.bk-imp-nav-item svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.bk-imp-nav-badge {
  margin-left: auto;
  background: rgba(125, 232, 255, 0.15);
  color: #7de8ff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 99px;
}

/* ===== Main ===== */
.bk-imp-main {
  flex: 1;
  padding: 18px 32px 28px;
  overflow-y: auto;
  min-width: 0;
}

.bk-imp-top-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.bk-imp-top-stat {
  min-height: 76px;
  border: 1px solid rgba(112, 130, 165, 0.2);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(17, 27, 45, 0.98), rgba(13, 23, 39, 0.98));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04), 0 16px 38px rgba(0, 0, 0, 0.18);
  color: inherit;
  cursor: pointer;
  padding: 13px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  transition: border-color 0.15s, background 0.15s, transform 0.15s;
}

.bk-imp-top-stat:hover {
  border-color: rgba(125, 232, 255, 0.34);
  background: linear-gradient(180deg, rgba(20, 33, 55, 1), rgba(13, 26, 45, 1));
  transform: translateY(-1px);
}

.bk-imp-top-stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bk-imp-top-stat-icon svg {
  width: 21px;
  height: 21px;
}

.bk-imp-top-stat-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.bk-imp-top-stat.tone-total .bk-imp-top-stat-icon {
  background: rgba(50, 113, 230, 0.24);
  color: #73a8ff;
}

.bk-imp-top-stat.tone-published .bk-imp-top-stat-icon {
  background: rgba(37, 176, 104, 0.2);
  color: #55e79c;
}

.bk-imp-top-stat.tone-review .bk-imp-top-stat-icon {
  background: rgba(180, 125, 33, 0.22);
  color: #ffc05a;
}

.bk-imp-top-stat.tone-failed .bk-imp-top-stat-icon {
  background: rgba(210, 66, 84, 0.2);
  color: #ff7d8b;
}

.bk-imp-top-stat-value {
  font-size: 25px;
  line-height: 1;
  font-weight: 800;
  color: #ffffff;
}

.bk-imp-top-stat-label {
  font-size: 12px;
  font-weight: 700;
  color: #7f91ab;
}

.bk-imp-top-stat-desc {
  font-size: 12px;
  color: #8ea1bf;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bk-imp-filter-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.bk-imp-filter-tab {
  height: 34px;
  border: 1px solid rgba(134, 163, 196, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: #9eb4d1;
  cursor: pointer;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
}

.bk-imp-filter-tab.active {
  border-color: rgba(125, 232, 255, 0.34);
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
}

.bk-imp-filter-count {
  min-width: 18px;
  height: 18px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.08);
  color: #d8e8fb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  box-sizing: border-box;
}

.bk-imp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}

.bk-imp-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.bk-imp-header p {
  margin: 0;
  color: #7b8fa8;
  font-size: 14px;
}

.bk-imp-header-actions {
  display: flex;
  gap: 10px;
}

/* ===== Toast ===== */
.bk-imp-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 14px;
}

.bk-imp-toast svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.bk-imp-toast button {
  margin-left: auto;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 18px;
  opacity: 0.6;
}

.bk-imp-toast-error {
  background: rgba(255, 119, 134, 0.1);
  border: 1px solid rgba(255, 119, 134, 0.2);
  color: #ffd1d7;
}

.bk-imp-toast-success {
  background: rgba(106, 247, 179, 0.1);
  border: 1px solid rgba(106, 247, 179, 0.2);
  color: #c3ffe0;
}

.bk-imp-toast-floating {
  position: fixed;
  z-index: 1000;
  top: 18px;
  left: 50%;
  min-width: 280px;
  max-width: min(560px, calc(100vw - 32px));
  margin: 0;
  transform: translateX(-50%);
  background: rgba(9, 39, 35, 0.94);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.34);
  backdrop-filter: blur(14px);
}

.bk-imp-toast-fade-enter-active,
.bk-imp-toast-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.bk-imp-toast-fade-enter-from,
.bk-imp-toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}

.bk-imp-toast-fade-enter-to,
.bk-imp-toast-fade-leave-from {
  opacity: 1;
  transform: translate(-50%, 0);
}

/* ===== Delete Confirm Modal ===== */
.bk-imp-modal-overlay {
  position: fixed;
  z-index: 2000;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(2, 8, 16, 0.6);
  backdrop-filter: blur(6px);
}

.bk-imp-modal {
  min-width: 360px;
  max-width: min(480px, calc(100vw - 32px));
  padding: 28px 28px 22px;
  border: 1px solid rgba(247, 79, 79, 0.15);
  border-radius: 20px;
  background: rgba(8, 18, 31, 0.94);
  backdrop-filter: blur(18px);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(247, 79, 79, 0.06);
  text-align: center;
}

.bk-imp-modal-icon {
  margin-bottom: 14px;
}

.bk-imp-modal-icon svg {
  width: 44px;
  height: 44px;
}

.bk-imp-modal-title {
  font-size: 17px;
  font-weight: 700;
  color: #e8eff8;
  margin-bottom: 8px;
}

.bk-imp-modal-body {
  font-size: 14px;
  color: #9eb4d1;
  line-height: 1.6;
  margin-bottom: 14px;
}

.bk-imp-modal-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin-bottom: 18px;
}

.bk-imp-modal-item-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 8px;
  background: rgba(247, 79, 79, 0.08);
  border: 1px solid rgba(247, 79, 79, 0.12);
  color: #ffa8a8;
  font-size: 12px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bk-imp-modal-item-more {
  background: rgba(134, 163, 196, 0.08);
  border-color: rgba(134, 163, 196, 0.12);
  color: #9eb4d1;
}

.bk-imp-modal-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.bk-imp-modal-actions .bk-imp-btn {
  min-width: 100px;
  justify-content: center;
}

.bk-imp-modal-fade-enter-active,
.bk-imp-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.bk-imp-modal-fade-enter-active .bk-imp-modal,
.bk-imp-modal-fade-leave-active .bk-imp-modal {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.bk-imp-modal-fade-enter-from,
.bk-imp-modal-fade-leave-to {
  opacity: 0;
}

.bk-imp-modal-fade-enter-from .bk-imp-modal,
.bk-imp-modal-fade-leave-to .bk-imp-modal {
  transform: scale(0.94) translateY(8px);
  opacity: 0;
}

.bk-imp-modal-fade-enter-to,
.bk-imp-modal-fade-leave-from {
  opacity: 1;
}

.bk-imp-modal-fade-enter-to .bk-imp-modal,
.bk-imp-modal-fade-leave-from .bk-imp-modal {
  transform: scale(1) translateY(0);
  opacity: 1;
}

/* ===== Cards ===== */
.bk-imp-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 16px;
}

.bk-imp-card {
  border: 1px solid rgba(134, 163, 196, 0.12);
  border-radius: 20px;
  background: rgba(8, 18, 31, 0.72);
  backdrop-filter: blur(18px);
  padding: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  transition: border-color 0.2s;
}

.bk-imp-card:hover {
  border-color: rgba(134, 163, 196, 0.24);
}

.bk-imp-card-upload {
  cursor: pointer;
}

.bk-imp-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(125, 232, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.bk-imp-card-icon svg {
  width: 24px;
  height: 24px;
  color: #7de8ff;
}

.bk-imp-card-icon-alt {
  background: rgba(106, 247, 179, 0.08);
}

.bk-imp-card-icon-alt svg {
  color: #6af7b3;
}

.bk-imp-card-title {
  font-size: 18px;
  font-weight: 700;
}

.bk-imp-card-desc {
  color: #7b8fa8;
  font-size: 13px;
}

.bk-imp-format-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.bk-imp-format-tag {
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(134, 163, 196, 0.12);
  font-size: 11px;
  font-weight: 600;
  color: #9eb4d1;
  letter-spacing: 0.04em;
}

.bk-imp-format-tag.tag-epub {
  color: #7de8ff;
  border-color: rgba(125, 232, 255, 0.25);
  background: rgba(125, 232, 255, 0.06);
}

.bk-imp-format-tag.tag-pdf {
  color: #ff9f7f;
  border-color: rgba(255, 159, 127, 0.25);
  background: rgba(255, 159, 127, 0.06);
}

.bk-imp-hidden-input {
  display: none;
}

.bk-imp-external-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  margin-top: 4px;
}

/* ===== Inputs ===== */
.bk-imp-input {
  width: 100%;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: #e8eff8;
  padding: 10px 14px;
  font: inherit;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.bk-imp-input:focus {
  outline: none;
  border-color: rgba(125, 232, 255, 0.4);
}

.bk-imp-input::placeholder {
  color: #5a6d82;
}

.bk-imp-select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  cursor: pointer;
}

.bk-imp-select-trigger:hover {
  border-color: rgba(125, 232, 255, 0.3);
  background: rgba(255, 255, 255, 0.04);
}

.bk-imp-select-trigger.active {
  border-color: rgba(125, 232, 255, 0.4);
  box-shadow: 0 0 0 1px rgba(125, 232, 255, 0.08);
}

.bk-imp-select-trigger-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bk-imp-select-trigger svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  fill: none;
  stroke: #cfe4ff;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 0.18s ease, stroke 0.18s ease;
}

.bk-imp-select-trigger.active svg {
  transform: rotate(180deg);
  stroke: #7de8ff;
}

.bk-imp-hidden-input {
  display: none;
}

.bk-imp-combo {
  position: relative;
  width: 100%;
}

.bk-imp-combo-menu {
  position: absolute;
  z-index: 20;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid rgba(134, 163, 196, 0.18);
  border-radius: 10px;
  background: #161922;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.34);
  padding: 6px;
}

.bk-imp-combo-menu::before {
  content: '';
  position: absolute;
  top: -7px;
  left: 18px;
  width: 12px;
  height: 12px;
  background: #161922;
  border-left: 1px solid rgba(134, 163, 196, 0.18);
  border-top: 1px solid rgba(134, 163, 196, 0.18);
  transform: rotate(45deg);
}

.bk-imp-combo-option {
  position: relative;
  z-index: 1;
  width: 100%;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #e8eff8;
  cursor: pointer;
  display: block;
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
  padding: 8px 10px;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bk-imp-combo-option:hover {
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
}

.bk-imp-combo-option.active {
  background: rgba(125, 232, 255, 0.12);
  color: #f4fbff;
}

.bk-imp-textarea {
  width: 100%;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  color: #e8eff8;
  padding: 14px;
  font: inherit;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', 'Fira Code', monospace;
  box-sizing: border-box;
  resize: vertical;
  line-height: 1.65;
  transition: border-color 0.15s;
}

.bk-imp-textarea:focus {
  outline: none;
  border-color: rgba(125, 232, 255, 0.4);
}

.bk-imp-content-preview {
  min-height: 520px;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  padding: 20px 22px;
  overflow: auto;
}

.bk-imp-content-preview-body {
  color: #e8eff8;
  font-size: 15px;
  line-height: 1.9;
  word-break: break-word;
}

.bk-imp-content-preview-body :deep(*) {
  max-width: 100%;
}

.bk-imp-content-preview-body :deep(p) {
  margin: 0 0 1em;
}

.bk-imp-content-preview-body :deep(h1),
.bk-imp-content-preview-body :deep(h2),
.bk-imp-content-preview-body :deep(h3),
.bk-imp-content-preview-body :deep(h4),
.bk-imp-content-preview-body :deep(h5),
.bk-imp-content-preview-body :deep(h6) {
  color: #ffffff;
  line-height: 1.4;
  margin: 1.2em 0 0.6em;
}

.bk-imp-content-preview-body :deep(img) {
  display: block;
  height: auto;
  margin: 1.2em auto;
}

.bk-imp-content-preview-body :deep(blockquote) {
  margin: 1.2em 0;
  padding: 0.8em 1em;
  border-left: 3px solid rgba(125, 232, 255, 0.4);
  background: rgba(125, 232, 255, 0.06);
  color: #cfe4ff;
}

.bk-imp-content-preview-body :deep(pre),
.bk-imp-content-preview-body :deep(code) {
  font-family: 'JetBrains Mono', 'Consolas', 'Fira Code', monospace;
}

.bk-imp-content-preview-empty {
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7f9a;
  font-size: 14px;
}

/* ===== Buttons ===== */
.bk-imp-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 18px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s;
  white-space: nowrap;
}

.bk-imp-btn-primary {
  background: linear-gradient(135deg, #7de8ff, #6af7b3);
  color: #082032;
}

.bk-imp-btn-accent {
  background: rgba(106, 247, 179, 0.12);
  border: 1px solid rgba(106, 247, 179, 0.25);
  color: #6af7b3;
}

.bk-imp-btn-accent:hover {
  background: rgba(106, 247, 179, 0.18);
}

.bk-imp-btn-ghost {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(134, 163, 196, 0.15);
  color: #9eb4d1;
}

.bk-imp-btn-ghost:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e8eff8;
}

.bk-imp-btn-danger {
  background: rgba(247, 79, 79, 0.12);
  border: 1px solid rgba(247, 79, 79, 0.25);
  color: #f74f4f;
}

.bk-imp-btn-danger:hover {
  background: rgba(247, 79, 79, 0.2);
}

.bk-imp-btn-danger:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.bk-imp-btn-sm {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 8px;
}

.bk-imp-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bk-imp-spin {
  width: 16px;
  height: 16px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Progress ===== */
.bk-imp-progress-section {
  border: 1px solid rgba(134, 163, 196, 0.12);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  backdrop-filter: blur(18px);
  padding: 16px 18px;
}

.bk-imp-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 10px;
  flex-wrap: wrap;
}

.bk-imp-progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bk-imp-progress-title {
  font-weight: 700;
  font-size: 16px;
}

.bk-imp-progress-meta {
  color: #7b8fa8;
  font-size: 13px;
}

.bk-imp-progress-bar {
  height: 8px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
  margin-bottom: 8px;
}

.bk-imp-progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7de8ff, #6af7b3);
  transition: width 0.4s ease;
}

.bk-imp-progress-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #7b8fa8;
  font-size: 13px;
}

.bk-imp-progress-actions {
  display: flex;
  gap: 8px;
}

.bk-imp-progress-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 700;
}

.bk-imp-progress-status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 99px;
  background: currentColor;
}

.status-uploaded { color: #9eb4d1; background: rgba(158, 180, 209, 0.08); }
.status-parsing { color: #7de8ff; background: rgba(125, 232, 255, 0.08); }
.status-await_review { color: #ffd58a; background: rgba(255, 213, 138, 0.08); }
.status-approved { color: #7de8ff; background: rgba(125, 232, 255, 0.08); }
.status-rejected { color: #ffb38a; background: rgba(255, 179, 138, 0.08); }
.status-published { color: #6af7b3; background: rgba(106, 247, 179, 0.08); }
.status-failed { color: #ff8f9f; background: rgba(255, 143, 159, 0.08); }
.status-deleted { color: #b7c3d5; background: rgba(183, 195, 213, 0.08); }

/* ===== History ===== */
.bk-imp-history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bk-imp-batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid rgba(134, 163, 196, 0.12);
  border-radius: 12px;
  background: rgba(8, 18, 31, 0.62);
}

.bk-imp-select-all,
.bk-imp-row-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #9eb4d1;
  font-size: 13px;
  cursor: pointer;
}

.bk-imp-select-all input,
.bk-imp-row-check input {
  width: 16px;
  height: 16px;
  accent-color: #6af7b3;
}

.bk-imp-batch-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.bk-imp-row-check {
  flex-shrink: 0;
}

.bk-imp-history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  border: 1px solid rgba(134, 163, 196, 0.08);
  background: rgba(8, 18, 31, 0.5);
  cursor: pointer;
  transition: all 0.15s;
}

.bk-imp-history-item:hover {
  border-color: rgba(125, 232, 255, 0.2);
  background: rgba(125, 232, 255, 0.03);
}

.bk-imp-history-left {
  flex-shrink: 0;
}

.bk-imp-history-format {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: rgba(125, 232, 255, 0.06);
  border: 1px solid rgba(125, 232, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: #7de8ff;
  letter-spacing: 0.04em;
}

.bk-imp-history-cover {
  position: relative;
  width: 52px;
  height: 64px;
  border-radius: 10px;
  border: 1px solid rgba(125, 232, 255, 0.14);
  background: rgba(125, 232, 255, 0.06);
  overflow: hidden;
  box-shadow: 0 12px 22px rgba(0, 0, 0, 0.22);
}

.bk-imp-history-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.bk-imp-history-center {
  flex: 1;
  min-width: 0;
}

.bk-imp-history-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bk-imp-history-meta {
  display: flex;
  gap: 12px;
  color: #7b8fa8;
  font-size: 12px;
}

.bk-imp-history-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.bk-imp-history-actions {
  display: flex;
  gap: 6px;
}

.bk-imp-category-delete-wrap {
  position: relative;
}

.bk-imp-category-delete-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 50;
  min-width: 180px;
  margin-top: 6px;
  padding: 8px;
  border: 1px solid rgba(247, 79, 79, 0.2);
  border-radius: 12px;
  background: rgba(16, 26, 40, 0.96);
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.bk-imp-category-delete-title {
  padding: 6px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #9eb4d1;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.bk-imp-category-delete-option {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #f74f4f;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}

.bk-imp-category-delete-option:hover {
  background: rgba(247, 79, 79, 0.1);
}

.bk-imp-category-delete-cancel {
  color: #9eb4d1;
  margin-top: 4px;
  border-top: 1px solid rgba(134, 163, 196, 0.1);
  padding-top: 10px;
}

.bk-imp-category-delete-cancel:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e8eff8;
}

/* ===== Review ===== */
.bk-imp-review-shell {
  min-height: 680px;
}

.bk-imp-review-fade-enter-active,
.bk-imp-review-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.bk-imp-review-fade-enter-from,
.bk-imp-review-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.bk-imp-review-fade-enter-to,
.bk-imp-review-fade-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.bk-imp-review {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bk-imp-review-meta {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.bk-imp-review-meta-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(8, 18, 31, 0.5);
  border: 1px solid rgba(134, 163, 196, 0.08);
}

.bk-imp-review-meta-row-wide {
  grid-column: span 2;
}

.bk-imp-review-cover-row {
  grid-column: span 3;
}

.bk-imp-cover-editor {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 10px;
  align-items: stretch;
}

.bk-imp-cover-preview {
  width: 86px;
  min-height: 116px;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: #6b7f9a;
  cursor: pointer;
  padding: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font: inherit;
  font-size: 12px;
  line-height: 1.35;
  text-align: center;
}

.bk-imp-cover-preview img {
  width: 100%;
  height: 100%;
  min-height: 116px;
  object-fit: cover;
  display: block;
}

.bk-imp-cover-fields {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.bk-imp-cover-file {
  min-height: 42px;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: #d8e8fb;
  display: flex;
  align-items: center;
  padding: 0 12px;
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bk-imp-cover-file.empty {
  color: #6b7f9a;
}

.bk-imp-cover-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.bk-imp-review-label {
  font-size: 11px;
  color: #6b7f9a;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.bk-imp-review-body {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
  min-height: 480px;
}

.bk-imp-chapter-nav {
  border: 1px solid rgba(134, 163, 196, 0.1);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.bk-imp-chapter-nav-head {
  padding: 14px 18px;
  font-weight: 700;
  font-size: 13px;
  border-bottom: 1px solid rgba(134, 163, 196, 0.08);
  color: #9eb4d1;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.bk-imp-chapter-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border: none;
  border-bottom: 1px solid rgba(134, 163, 196, 0.04);
  background: transparent;
  color: #c7d5e5;
  cursor: pointer;
  text-align: left;
  transition: all 0.1s;
}

.bk-imp-chapter-item:hover {
  background: rgba(125, 232, 255, 0.04);
}

.bk-imp-chapter-item.active {
  background: rgba(125, 232, 255, 0.08);
  color: #7de8ff;
}

.bk-imp-chapter-idx {
  font-size: 12px;
  font-weight: 700;
  color: #6b7f9a;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
}

.bk-imp-chapter-item.active .bk-imp-chapter-idx {
  color: #7de8ff;
}

.bk-imp-chapter-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bk-imp-chapter-title {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bk-imp-chapter-sub {
  font-size: 11px;
  color: #6b7f9a;
}

.bk-imp-chapter-edited-dot {
  width: 6px;
  height: 6px;
  border-radius: 99px;
  background: #6af7b3;
  flex-shrink: 0;
}

.bk-imp-chapter-draft-dot {
  width: 6px;
  height: 6px;
  border-radius: 99px;
  background: #ffd58a;
  flex-shrink: 0;
}

.bk-imp-editor-panel {
  border: 1px solid rgba(134, 163, 196, 0.1);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.bk-imp-editor {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  flex: 1;
}

.bk-imp-editor-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bk-imp-editor-chapter-label {
  font-size: 12px;
  font-weight: 700;
  color: #7de8ff;
  letter-spacing: 0.1em;
}

.bk-imp-editor-top-actions {
  display: flex;
  gap: 8px;
}

.bk-imp-editor-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

/* ===== Warnings ===== */
.bk-imp-warnings {
  border-radius: 12px;
  background: rgba(255, 196, 87, 0.06);
  border: 1px solid rgba(255, 196, 87, 0.14);
  padding: 14px 16px;
}

.bk-imp-warnings-head {
  font-weight: 700;
  font-size: 12px;
  color: #ffd58a;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.bk-imp-warning-item {
  color: #ffeab4;
  font-size: 13px;
  padding: 4px 0;
}

/* ===== Empty ===== */
.bk-imp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 36px 18px;
  color: #6b7f9a;
}

.bk-imp-empty svg {
  width: 48px;
  height: 48px;
  opacity: 0.3;
}

.bk-imp-empty-sm {
  padding: 28px;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .bk-imp-top-stats {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
  .bk-imp-cards {
    grid-template-columns: 1fr;
  }
  .bk-imp-review-meta {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .bk-imp-sidebar {
    display: none;
  }
  .bk-imp-main {
    padding: 16px 18px 24px;
  }
  .bk-imp-review-body {
    grid-template-columns: 1fr;
  }
  .bk-imp-review-meta {
    grid-template-columns: 1fr 1fr;
  }
  .bk-imp-review-meta-row-wide {
    grid-column: span 2;
  }
  .bk-imp-review-cover-row {
    grid-column: span 2;
  }
}

@media (max-width: 600px) {
  .bk-imp-top-stats {
    grid-template-columns: 1fr;
  }
  .bk-imp-review-meta,
  .bk-imp-editor-fields {
    grid-template-columns: 1fr;
  }
  .bk-imp-review-meta-row-wide {
    grid-column: span 1;
  }
  .bk-imp-review-cover-row {
    grid-column: span 1;
  }
  .bk-imp-cover-editor {
    grid-template-columns: 74px 1fr;
  }
  .bk-imp-cover-preview {
    width: 74px;
    min-height: 102px;
  }
  .bk-imp-cover-preview img {
    min-height: 102px;
  }
  .bk-imp-history-meta {
    flex-wrap: wrap;
  }
  .bk-imp-batch-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
