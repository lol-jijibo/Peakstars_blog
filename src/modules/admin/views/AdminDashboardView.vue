<template>
  <div class="admin-console-page">
    <div class="toast-container">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="['toast-' + toast.type, { 'toast-leaving': toast.leaving }]"
        @click="dismissToast(toast.id)"
      >
        <span class="toast-icon">{{ toast.icon }}</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </div>

    <div v-if="confirmVisible" class="confirm-overlay" @click.self="cancelConfirm">
      <div class="confirm-dialog">
        <div class="confirm-title">{{ confirmTitle }}</div>
        <div class="confirm-body">{{ confirmMessage }}</div>
        <div class="confirm-actions">
          <button class="btn btn-ghost" type="button" @click="cancelConfirm">取消</button>
          <button class="btn btn-primary" type="button" style="background:var(--red);color:#fff" @click="resolveConfirm">确认</button>
        </div>
      </div>
    </div>
    <input
      ref="excelFileInputRef"
      type="file"
      accept=".xlsx,.xls"
      class="admin-console-hidden-input"
      @change="handleExcelImportFile"
    />
    <input
      ref="documentImportInputRef"
      type="file"
      accept=".docx,.html,.htm,.md,.markdown,.txt"
      class="admin-console-hidden-input"
      @change="handleDocumentImportFile"
    />

    <aside class="sidebar">
      <div class="logo">
        <div class="logo-text">PeakStars Blog</div>
      </div>

      <nav class="nav">
        <div class="nav-section">内容</div>
        <button
          v-for="module in moduleOptions"
          :key="module.key"
          class="nav-item"
          :class="{ active: routeSection === module.key }"
          type="button"
          @click="switchType(module.key)"
        >
          <span class="icon icon-emoji" aria-hidden="true">{{ resolveSidebarEmoji(module.key) }}</span>
          {{ module.navLabel }}
          <span class="nav-badge">{{ module.badge }}</span>
        </button>

        <div class="nav-section">数据</div>
        <button class="nav-item" :class="{ active: isStatsPage }" type="button" @click="scrollToAnalytics">
          <span class="icon icon-emoji" aria-hidden="true">📊</span>
          数据分析
        </button>
        <button class="nav-item" :class="{ active: isCommentsPage }" type="button" @click="scrollToCommentSection">
          <span class="icon icon-emoji" aria-hidden="true">💬</span>
          评论管理
          <span class="nav-badge">{{ formatCount(summary.totalComments) }}</span>
        </button>

        <div class="nav-section">书籍</div>
        <button class="nav-item nav-item-book" :class="{ active: isBooksImportPage }" type="button" @click="openBookImportPage">
          <span class="icon icon-emoji" aria-hidden="true">📚</span>
          书籍导入
        </button>
        <button class="nav-item nav-item-book" :class="{ active: isBooksOverviewPage }" type="button" @click="openBookOverviewPage">
          <span class="icon icon-emoji" aria-hidden="true">📖</span>
          书籍总览
        </button>

        <div class="nav-section">系统</div>
        <button v-if="isContentPage" class="nav-item" type="button" @click="openCreateDialog">
          <span class="icon icon-emoji" aria-hidden="true">🆕</span>
          新建内容
        </button>
        <button v-if="isContentPage" class="nav-item" type="button" @click="handleExportCurrentModule">
          <span class="icon icon-emoji" aria-hidden="true">📤</span>
          导出内容
        </button>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <div class="avatar">PS</div>
          <div>
            <div class="user-name">PeakStars 管理员</div>
            <div class="user-role">在线 {{ summary.onlineUsers }} / 最近更新 {{ summary.lastUpdatedAt || '--' }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-title">{{ pageTitle }}</div>
        <div v-if="isContentPage" class="topbar-actions">
          <label class="search-box">
            <!-- 目的: 将后台检索入口替换为更直观的彩色搜索图标; 逻辑: 使用内联 SVG 承载图标样式并保持与输入框同一交互热区。 -->
            <span class="search-box-icon" aria-hidden="true">
              <svg viewBox="0 0 20 20" focusable="false">
                <defs>
                  <linearGradient id="admin-search-icon-ring" x1="3" y1="3" x2="15" y2="15" gradientUnits="userSpaceOnUse">
                    <stop offset="0%" stop-color="#45caff" />
                    <stop offset="55%" stop-color="#3ddc97" />
                    <stop offset="100%" stop-color="#ffd166" />
                  </linearGradient>
                </defs>
                <circle cx="8.5" cy="8.5" r="5.5" fill="#10243d" stroke="url(#admin-search-icon-ring)" stroke-width="2" />
                <path d="M12.6 12.6L16.4 16.4" stroke="#ffb347" stroke-width="2.2" stroke-linecap="round" />
                <circle cx="8.5" cy="8.5" r="1.4" fill="#8be9fd" opacity="0.9" />
              </svg>
            </span>
            <input v-model.trim="keyword" type="text" placeholder="搜索内容…" />
          </label>
          <button class="btn btn-ghost" type="button" @click="triggerDocumentImport">导入文档</button>
          <button class="btn btn-ghost" type="button" @click="triggerBatchImport">Excel 导入</button>
          <button class="btn btn-ghost" type="button" @click="handleExportCurrentModule">导出</button>
          <button class="btn btn-primary" type="button" @click="openCreateDialog">+ 新建{{ activeModule.shortLabel }}</button>
        </div>
        <div v-else class="topbar-actions">
          <span class="topbar-meta">最近更新 {{ summary.lastUpdatedAt || '--' }}</span>
          <button class="btn btn-ghost" type="button" @click="refreshPageData">刷新数据</button>
        </div>
      </header>

      <div class="content">
        <section v-if="errorMessage" class="admin-console-error">
          <strong>后台请求失败</strong>
          <p>{{ errorMessage }}</p>
        </section>

        <div v-if="isContentPage" class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">文</div>
            <div class="stat-label">{{ activeModule.statLabels.total }}</div>
            <div class="stat-value">{{ moduleSummary.contentCount }}</div>
            <div class="stat-trend">↗ 本月新增 {{ moduleSummary.currentMonthCount }} 条</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">阅</div>
            <div class="stat-label">{{ activeModule.statLabels.views }}</div>
            <div class="stat-value stat-value-green">{{ formatCount(moduleSummary.viewCount) }}</div>
            <div class="stat-trend stat-trend-green">↗ 热度内容持续增长</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">评</div>
            <div class="stat-label">{{ activeModule.statLabels.comments }}</div>
            <div class="stat-value stat-value-blue">{{ formatCount(moduleSummary.commentCount) }}</div>
            <div class="stat-trend stat-trend-blue">↗ 今日活跃保持在线</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">待</div>
            <div class="stat-label">{{ activeModule.statLabels.focus }}</div>
            <div class="stat-value stat-value-red">{{ activeModule.focusCount }}</div>
            <div class="stat-trend stat-trend-red">{{ activeModule.focusHint }}</div>
          </div>
        </div>

        <!-- 目的: 在内容管理表格之前先承接后台数据分析总览; 逻辑: 用后端 dashboard 聚合结果驱动 ECharts 图表和热榜卡片，形成可快速扫描的分析层。 -->
        <section v-if="isStatsPage" ref="analyticsSectionRef" class="analytics-grid">
          <div class="panel analytics-panel analytics-panel-wide">
            <div class="panel-header">
              <span class="panel-title">数据分析总览</span>
              <span class="panel-action">实时聚合</span>
            </div>
            <div class="analytics-highlight-grid">
              <div v-for="item in analyticsHighlights" :key="item.label" class="analytics-highlight-card">
                <span class="analytics-highlight-label">{{ item.label }}</span>
                <strong class="analytics-highlight-value">{{ item.value }}</strong>
                <span class="analytics-highlight-note">{{ item.note }}</span>
              </div>
            </div>
            <AdminTrendChart :trend-points="trendPoints" />
          </div>

          <div class="panel analytics-panel">
            <div class="panel-header">
              <span class="panel-title">模块流量对比</span>
              <span class="panel-action">ECharts</span>
            </div>
            <AdminModuleChart :module-stats="moduleStats" />
            <div class="analytics-highlight-grid analytics-highlight-grid-compact">
              <div v-for="item in moduleSnapshotCards" :key="item.key" class="analytics-highlight-card">
                <span class="analytics-highlight-label">{{ item.label }}</span>
                <strong class="analytics-highlight-value">{{ item.value }}</strong>
                <span class="analytics-highlight-note">{{ item.note }}</span>
              </div>
            </div>
          </div>

          <div class="panel analytics-panel analytics-panel-wide">
            <div class="panel-header">
              <span class="panel-title">热点内容排行</span>
              <span class="panel-action">Top {{ hotContentRankings.length }}</span>
            </div>
            <div v-if="hotContentRankings.length" class="hot-ranking-list">
              <div v-for="item in hotContentRankings" :key="`${item.type}-${item.id}`" class="hot-ranking-item">
                <div class="hot-ranking-main">
                  <span class="hot-ranking-index">#{{ item.rank }}</span>
                  <div class="hot-ranking-meta">
                    <div class="hot-ranking-title">{{ item.title }}</div>
                    <div class="hot-ranking-sub">{{ resolveCategory(item) }} / {{ formatCount(item.viewCount) }} 浏览 / {{ formatCount(item.commentCount || 0) }} 评论</div>
                  </div>
                </div>
                <div class="hot-ranking-track">
                  <div class="hot-ranking-fill" :style="{ width: `${item.ratio}%` }"></div>
                </div>
              </div>
            </div>
            <div v-else class="activity-empty">当前暂无热点内容数据。</div>
          </div>
        </section>

        <AdminBookImportView v-if="isBooksImportPage" :key="route.fullPath" :standalone="false" />
        <AdminBookOverviewView v-else-if="isBooksOverviewPage" :key="route.fullPath" />

        <div v-if="isContentPage && currentType !== 'interview'" class="tab-row">
          <button
            v-for="tab in filterTabs"
            :key="tab.key"
            class="tab"
            :class="{ active: currentFilter === tab.key }"
            type="button"
            @click="currentFilter = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
        <!-- 面经模块的特殊过滤逻辑：先按一级分类过滤，再根据选中的一级分类动态展示二级标签进行进一步过滤，最后提供一个独立的草稿过滤入口 -->
        <div v-else-if="isContentPage && currentType === 'interview'" class="interview-filter-stack">
          <div class="tab-row interview-top-tabs">
            <button
              v-for="firstLevel in adminInterviewFirstLevelCategories"
              :key="firstLevel.key"
              class="tab"
              :class="{ active: currentFilter !== 'draft' && adminInterviewFirstLevelKey === firstLevel.key }"
              type="button"
              @click="selectAdminInterviewFirstLevel(firstLevel.key)"
            >
              {{ firstLevel.label }} ({{ resolveInterviewFirstLevelCount(firstLevel.key) }})
            </button>
            <button
              class="tab"
              :class="{ active: currentFilter === 'draft' }"
              type="button"
              @click="currentFilter = 'draft'"
            >
              待编辑{{ drafts.length ? ` (${drafts.length})` : '' }}
            </button>
          </div>

          <div v-if="currentFilter !== 'draft' && adminInterviewTagOptions.length" class="interview-sub-tabs">
            <button
              v-for="tag in adminInterviewTagOptions"
              :key="tag.key"
              class="interview-sub-tab"
              :class="{ active: adminInterviewSelectedTagKey === tag.key }"
              type="button"
              @click="selectAdminInterviewTag(tag.key)"
            >
              {{ tag.label }} <span class="interview-sub-tab-count">{{ tag.count }}</span>
            </button>
          </div>
        </div>

        <div v-if="isContentPage" class="two-col">
          <div ref="tableSectionRef" class="panel">
            <div class="panel-header">
              <span class="panel-title">{{ activeModule.panelTitle }}</span>
              <span class="panel-action">查看全部 →</span>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>标题</th>
                    <th>分类</th>
                    <th>状态</th>
                    <th>阅读</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody v-if="filteredRecords.length">
                  <tr v-for="record in filteredRecords" :key="`${record.type}-${record.id}`" :class="{ 'draft-row': record._isLocalDraft }">
                    <td>
                      <div class="post-title-cell">
                        <span class="post-title">{{ record.title }}</span>
                        <span class="post-meta">{{ resolveMeta(record) }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="tag" :class="{ 'tag-draft': record._isLocalDraft }">{{ record._isLocalDraft ? '草稿' : resolveCategory(record) }}</span>
                    </td>
                    <td>
                      <span v-if="record._isLocalDraft" class="badge badge-draft">{{ formatDraftTime(record._savedAt) }}</span>
                      <span v-else class="badge" :class="resolveStatus(record).className">{{ resolveStatus(record).label }}</span>
                    </td>
                    <td v-if="record._isLocalDraft" class="mono-cell">{{ (record.contentHtml || '').replace(/<[^>]*>/g, '').length }} 字</td>
                    <td v-else class="mono-cell">{{ resolveReadValue(record) }}</td>
                    <td>
                      <div class="row-actions">
                        <template v-if="record._isLocalDraft">
                          <button class="icon-btn" type="button" title="继续编辑" @click="restoreDraft(record)">✏</button>
                          <button class="icon-btn del" type="button" title="删除草稿" @click="removeDraftRecord(record)">删</button>
                        </template>
                        <template v-else>
                          <button class="icon-btn" type="button" title="编辑" @click="openEditDialog(record)">编</button>
                          <button class="icon-btn" type="button" title="刷新" @click="refreshCurrentType">预</button>
                          <button class="icon-btn del" type="button" title="删除" @click="removeRecord(record)">删</button>
                        </template>
                      </div>
                    </td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr>
                    <td colspan="5" class="empty-cell">当前条件下暂无内容。</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div ref="activitySectionRef" class="side-panels">
            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">{{ activeModule.sideTitle }}</span>
                <span class="panel-action">管理</span>
              </div>
              <div v-for="item in sideCategoryItems" :key="item.name" class="cat-item">
                <span class="cat-name">
                  <span class="cat-dot" :style="{ background: item.color }"></span>
                  {{ item.name }}
                </span>
                <span class="cat-count">{{ item.count }}</span>
              </div>
            </div>

            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">最近动态</span>
              </div>
              <div v-if="currentRecentEdits.length">
                <div v-for="item in currentRecentEdits" :key="item.id" class="activity-item">
                  <div class="activity-dot" :style="{ background: resolveActivityColor(item.actionType) }"></div>
                  <div>
                    <div class="activity-text">{{ resolveActivityText(item) }}</div>
                    <div class="activity-time">{{ item.createdAt }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="activity-empty">当前暂无最近动态。</div>
            </div>

            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">本月内容目标</span>
              </div>
              <div class="goal-box">
                <div class="goal-head">
                  <span>已完成</span>
                  <span class="goal-strong">{{ progressInfo.current }} / {{ progressInfo.target }} 条</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: `${progressInfo.percent}%` }"></div>
                </div>
                <div class="goal-note">{{ progressInfo.note }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 目的: 承接后台评论管理模块的核心看板与巡检列表; 逻辑: 先用摘要卡和 ECharts 标出高压内容，再落到逐条内容级的评论运营明细。 -->
        <section v-if="isCommentsPage" ref="commentSectionRef" class="comment-grid">
          <div class="panel comment-panel">
            <div class="panel-header">
              <span class="panel-title">评论管理概览</span>
              <span class="panel-action">统一巡检</span>
            </div>
            <div class="comment-summary-grid">
              <div v-for="item in commentSummaryCards" :key="item.label" class="comment-summary-card">
                <span class="comment-summary-label">{{ item.label }}</span>
                <strong class="comment-summary-value">{{ item.value }}</strong>
                <span class="comment-summary-note">{{ item.note }}</span>
              </div>
            </div>
            <div class="comment-summary-grid comment-summary-grid-wide">
              <div v-for="item in commentModuleCards" :key="item.key" class="comment-summary-card">
                <span class="comment-summary-label">{{ item.label }}</span>
                <strong class="comment-summary-value">{{ item.value }}</strong>
                <span class="comment-summary-note">{{ item.note }}</span>
              </div>
            </div>
            <AdminCommentChart :comment-records="commentManagementList" />
          </div>

          <div class="panel comment-panel">
            <div class="panel-header">
              <span class="panel-title">评论巡检列表</span>
              <span class="panel-action">Top {{ commentManagementList.length }}</span>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>内容</th>
                    <th>模块</th>
                    <th>评论量</th>
                    <th>待跟进</th>
                    <th>互动率</th>
                    <th>优先级</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody v-if="commentManagementList.length">
                  <tr v-for="item in commentManagementList" :key="`${item.contentType}-${item.contentKey}`">
                    <td>
                      <div class="post-title-cell">
                        <span class="post-title">{{ item.contentTitle }}</span>
                        <span class="post-meta">{{ item.authorName }} / {{ item.publishedAt }}</span>
                      </div>
                    </td>
                    <td><span class="tag">{{ item.moduleLabel }}</span></td>
                    <td class="mono-cell">{{ formatCount(item.commentCount) }}</td>
                    <td class="mono-cell">{{ formatCount(item.pendingCount) }}</td>
                    <td class="mono-cell">{{ Number(item.engagementRate || 0).toFixed(1) }}%</td>
                    <td>
                      <span class="badge" :class="resolveCommentPriorityClass(item.priority)">{{ item.priorityLabel }}</span>
                    </td>
                    <td>
                      <div class="comment-status-cell">
                        <span class="badge" :class="resolveCommentStatusClass(item.status)">{{ item.statusLabel }}</span>
                        <span class="comment-status-note">{{ item.actionHint }}</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr>
                    <td colspan="7" class="empty-cell">当前暂无评论管理数据。</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div v-if="dialogVisible" class="modal-overlay" @click.self="closeDialog">
      <div class="modal">
        <div class="modal-title">+ {{ isEditing ? `编辑${activeModule.shortLabel}` : `新建${activeModule.shortLabel}` }}</div>

        <div class="form-group">
          <label class="form-label">标题</label>
          <input v-model.trim="draftForm.title" class="form-input" type="text" placeholder="请输入标题…" />
        </div>

        <div class="form-group">
          <label class="form-label">摘要</label>
          <textarea v-model.trim="draftForm.summary" class="form-input" placeholder="简短描述内容…" rows="3"></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ activeModule.formCategoryLabel }}</label>
            <select v-if="currentType === 'tech'" v-model="draftForm.category" class="form-input">
              <option value="frontend">前端</option>
              <option value="backend">后端</option>
            </select>
            <input
              v-else-if="currentType === 'world'"
              v-model.trim="draftForm.issueLabel"
              class="form-input"
              type="text"
              placeholder="例如 2026.08"
            />
            <select v-else-if="currentType === 'interview'" v-model="draftForm.category" class="form-input">
              <option value="frontend">前端</option>
              <option value="java">Java 后端</option>
              <option value="agent">Agent开发</option>
              <option value="llm">大模型原理</option>
            </select>
            <select v-else-if="currentType === 'ai'" v-model="draftForm.track" class="form-input">
              <option value="agent">Agent</option>
              <option value="multimodal">多模态</option>
              <option value="infra">基础设施</option>
            </select>
            <div v-else class="form-input-placeholder">—</div>
          </div>

          <div class="form-group">
            <label class="form-label">{{ currentType === 'interview' ? '难度' : '状态' }}</label>
            <select v-if="currentType === 'interview'" v-model="draftForm.difficulty" class="form-input">
              <option value="easy">基础</option>
              <option value="medium">中等</option>
              <option value="hard">困难</option>
            </select>
            <select v-else v-model="draftForm.visualStatus" class="form-input">
              <option value="published">已发布</option>
              <option value="draft">草稿</option>
              <option value="review">重点关注</option>
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ activeModule.formTagLabel }}（可选）</label>
            <!-- 面经模块：根据分类动态加载标签，多选模式 -->
            <div v-if="currentType === 'interview'" class="tag-checkbox-group">
              <label v-for="tag in availableTags" :key="tag" class="tag-checkbox-item">
                <input
                  type="checkbox"
                  :value="tag"
                  v-model="draftForm.selectedTags"
                  class="tag-checkbox"
                />
                <span class="tag-checkbox-label">{{ tag }}</span>
              </label>
              <div v-if="availableTags.length === 0" class="tag-empty-tip">
                暂无可用标签，请先在分类下创建一些面经并添加标签
              </div>
            </div>
            <!-- 其他模块：保持原有文本输入 -->
            <input
              v-else
              v-model.trim="draftForm.tagsText"
              class="form-input"
              type="text"
              :placeholder="activeModule.formTagPlaceholder"
            />
          </div>

          <div class="form-group">
            <label class="form-label">发布时间</label>
            <input v-model.trim="draftForm.publishedAt" class="form-input" type="datetime-local" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">作者</label>
            <input v-model.trim="draftForm.authorName" class="form-input" type="text" placeholder="请输入作者…" />
          </div>

          <div v-if="supportsCoverUpload" class="form-group">
            <label class="form-label">封面图片</label>
            <div class="cover-upload-area">
              <input
                ref="coverFileInputRef"
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp,image/bmp,image/svg+xml"
                class="admin-console-hidden-input"
                @change="handleCoverFileSelect"
              />
              <div v-if="draftForm.coverUrl" class="cover-preview">
                <img :src="draftForm.coverUrl" alt="封面预览" />
                <button type="button" class="cover-remove-btn" @click="removeCover">✕</button>
              </div>
              <div v-else class="cover-placeholder" @click="triggerCoverSelect">
                <span class="cover-placeholder-icon">📷</span>
                <span>点击选择封面图片</span>
                <small>支持 JPG / PNG / GIF / WebP / BMP / SVG，最大 10MB</small>
              </div>
              <div v-if="coverUploading" class="cover-uploading">
                <span>上传中…</span>
              </div>
            </div>
            <input v-model.trim="draftForm.coverUrl" class="form-input form-input--cover-url" type="text" placeholder="或手动输入封面地址…" />
          </div>
          <div v-else class="form-group">
            <label class="form-label">封面说明</label>
            <div class="form-input-placeholder">{{ coverFieldNotice }}</div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">外部内容导入</label>
          <div class="admin-import-toolbar">
            <button class="btn btn-ghost" type="button" :disabled="importPreviewLoading" @click="triggerDocumentImport">
              {{ importPreviewLoading ? '文档处理中…' : '一键选择文档' }}
            </button>
            <button class="btn btn-ghost" type="button" :disabled="importPreviewLoading" @click="applyImportedHtmlSafe">
              {{ importPreviewLoading ? '预处理中…' : '导入并预处理 HTML' }}
            </button>
          </div>
          <div class="admin-import-meta">
            {{ importFileName || '支持语雀、飞书、Notion 等工具导出的 docx / html / md / txt，正文会自动回填到编辑器。' }}
          </div>
          <textarea
            v-model.trim="draftForm.importHtml"
            class="form-input"
            rows="6"
            placeholder="粘贴语雀、飞书、Notion 等编辑器导出的 HTML 片段，点击下方按钮后会自动执行白名单清洗并迁移图片/附件。"
          ></textarea>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">来源类型</label>
              <input v-model.trim="draftForm.importSourceType" class="form-input" type="text" placeholder="例如 yuque / feishu / notion / html" />
            </div>
            <div class="form-group">
              <label class="form-label">来源地址</label>
              <input v-model.trim="draftForm.importSourceUrl" class="form-input" type="text" placeholder="可选，填写原文链接可辅助解析相对资源地址" />
            </div>
          </div>
          <div class="modal-footer modal-footer-inline">
            <button class="btn btn-ghost" type="button" :disabled="importPreviewLoading" @click="applyImportedHtmlSafe">
              {{ importPreviewLoading ? '预处理中…' : '导入并预处理 HTML' }}
            </button>
          </div>
        </div>

        <div class="editor-group">
          <label class="form-label">正文内容</label>
          <AdminRichEditor
            v-model="draftForm.contentHtml"
            placeholder="请输入正文内容"
          />
        </div>

        <div class="modal-footer">
          <button class="btn btn-ghost" type="button" @click="closeDialog">取消</button>
          <button class="btn btn-save-draft" type="button" :disabled="saving" @click="saveDraftLocally">
            💾 保存草稿
          </button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitDraft">
            {{ isEditing ? '保存修改' : '发布内容' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { getInterviewTagOptions, interviewFirstLevelCategories } from '@/data/interviewCategories.js'
import { useAdminConsoleStore } from '@/modules/admin/stores/adminConsole'
import { listAdminDrafts, saveAdminDraft, deleteAdminDraft, previewImportedAdminContent, uploadCoverImage } from '@/modules/admin/api/admin'
import AdminTrendChart from '@/modules/admin/components/AdminTrendChart.vue'
import AdminModuleChart from '@/modules/admin/components/AdminModuleChart.vue'
import AdminCommentChart from '@/modules/admin/components/AdminCommentChart.vue'
import AdminBookImportView from '@/modules/admin/views/AdminBookImportView.vue'
import AdminBookOverviewView from '@/modules/admin/views/AdminBookOverviewView.vue'

const AdminRichEditor = defineAsyncComponent(() => import('@/modules/admin/components/AdminRichEditor.vue'))

const route = useRoute()
const router = useRouter()
const adminStore = useAdminConsoleStore()
const { currentSummary, errorMessage, recentEdits, saving, trendPoints, moduleStats, hotContents, commentRecords } = storeToRefs(adminStore)

let xlsxLibraryPromise = null
let markdownRuntimePromise = null
let mammothRuntimePromise = null

// 业务目的：给左侧导航提供彩色模块图标，让每个模块入口都和参考模板一样有明显的视觉识别。
// 业务逻辑：模块图标统一映射为彩色 emoji，模板层只按模块 key 读取，避免继续使用乱码文字占位。
const sidebarEmojiMap = {
  tech: '📝',
  world: '📰',
  ai: '✨',
  interview: '🎙️'
}

// 业务目的：严格贴合模板布局时，所有可变标题、文案和字段映射都从模块配置统一生成。
// 业务逻辑：这样可以保持页面长得和模板一致，但表头、侧栏和表单内容仍然是我们项目自己的业务表达。
const moduleOptions = computed(() => [
  {
    key: 'tech',
    icon: '文',
    badge: `${resolveModuleCount('tech')}`,
    navLabel: '文章管理',
    topbarTitle: '文章管理',
    shortLabel: '文章',
    panelTitle: '最新文章',
    sideTitle: '文章分类',
    formCategoryLabel: '分类',
    formTagLabel: '亮点',
    formTagPlaceholder: '例如 前端, Vue, 工程化',
    statLabels: {
      total: '文章总数',
      views: '总阅读量',
      comments: '评论数',
      focus: '重点内容'
    }
  },
  {
    key: 'world',
    icon: '刊',
    badge: `${resolveModuleCount('world')}`,
    navLabel: '期刊管理',
    topbarTitle: '期刊管理',
    shortLabel: '期刊',
    panelTitle: '最新期刊',
    sideTitle: '期刊结构',
    formCategoryLabel: '期号',
    formTagLabel: '封面信息',
    formTagPlaceholder: '例如 热点观察, 专题聚焦',
    statLabels: {
      total: '期刊总数',
      views: '总阅读量',
      comments: '推荐热度',
      focus: '高推荐'
    }
  },
  {
    key: 'ai',
    icon: 'AI',
    badge: `${resolveModuleCount('ai')}`,
    navLabel: '热点管理',
    topbarTitle: 'AI 热点管理',
    shortLabel: '热点',
    panelTitle: '最新热点',
    sideTitle: '热点赛道',
    formCategoryLabel: '赛道',
    formTagLabel: '标签',
    formTagPlaceholder: '例如 Agent, 工作流, 工具调用',
    statLabels: {
      total: '热点总数',
      views: '总浏览量',
      comments: '讨论量',
      focus: '今日热点'
    }
  },
  {
    key: 'interview',
    icon: '面',
    badge: `${resolveModuleCount('interview')}`,
    navLabel: '面试管理',
    topbarTitle: '面试管理',
    shortLabel: '面试',
    panelTitle: '最新面经',
    sideTitle: '面试分类',
    formCategoryLabel: '分类',
    formTagLabel: '标签',
    formTagPlaceholder: '例如 Java 基础, 集合框架, JVM',
    statLabels: {
      total: '面经总数',
      views: '总阅读量',
      comments: '收藏数',
      focus: '高收藏'
    },
    focusCount: moduleSummary.value.featuredCount,
    focusHint: '↗ 高收藏内容持续受关注'
  }
])

const analyticsSectionRef = ref(null)
const tableSectionRef = ref(null)
const activitySectionRef = ref(null)
const commentSectionRef = ref(null)
const draftVersion = ref(0)
const drafts = ref([])
const toasts = ref([])
let toastIdCounter = 0

function showToast(message, type = 'success', duration = 1500) {
  const id = ++toastIdCounter
  const icon = { success: '✓', error: '✕', info: 'ℹ' }[type] || 'ℹ'
  toasts.value.push({ id, message, type, icon, leaving: false })
  if (duration > 0) {
    setTimeout(() => {
      const toast = toasts.value.find((t) => t.id === id)
      if (toast) toast.leaving = true
      setTimeout(() => dismissToast(id), 300)
    }, duration)
  }
}

function dismissToast(id) {
  const toast = toasts.value.find((t) => t.id === id)
  if (toast && !toast.leaving) {
    toast.leaving = true
    setTimeout(() => {
      toasts.value = toasts.value.filter((t) => t.id !== id)
    }, 300)
    return
  }
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

// ── 自定义确认框 ──────────────────────────────────────

const confirmVisible = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
let confirmResolver = null

function showConfirm(title, message) {
  return new Promise((resolve) => {
    confirmTitle.value = title
    confirmMessage.value = message
    confirmVisible.value = true
    confirmResolver = resolve
  })
}

function resolveConfirm() {
  confirmVisible.value = false
  if (confirmResolver) confirmResolver(true)
}

function cancelConfirm() {
  confirmVisible.value = false
  if (confirmResolver) confirmResolver(false)
}
const contentSectionKeys = ['tech', 'world', 'ai', 'interview']
const routeSection = computed(() => String(route.params.section || 'tech'))
const isContentPage = computed(() => contentSectionKeys.includes(routeSection.value))
const isBooksImportPage = computed(() => routeSection.value === 'books-import')
const isBooksOverviewPage = computed(() => routeSection.value === 'books-list')
const isBookSubPage = computed(() => isBooksImportPage.value || isBooksOverviewPage.value)
const isStatsPage = computed(() => routeSection.value === 'stats')
const isCommentsPage = computed(() => routeSection.value === 'comment')
const currentType = ref('tech')
const currentFilter = ref('all')
const keyword = ref('')
const dialogVisible = ref(false)
const isEditing = ref(false)
const excelFileInputRef = ref(null)
const documentImportInputRef = ref(null)
const coverFileInputRef = ref(null)
const coverUploading = ref(false)
const importPreviewLoading = ref(false)
const importFileName = ref('')
const draftForm = reactive(createEmptyDraft())

// 标签相关：根据分类动态加载可选标签
const availableTags = ref([])
const selectedTags = ref([])
const adminInterviewSupportedFirstLevelKeys = ['all', 'backend', 'frontend', 'agent', 'llm']
const adminInterviewFirstLevelKey = ref('all')
const adminInterviewSelectedTagKey = ref('all')
const adminInterviewFirstLevelCategories = computed(() =>
  interviewFirstLevelCategories.filter((item) => adminInterviewSupportedFirstLevelKeys.includes(item.key))
)
const adminInterviewActiveFirstLevel = computed(() =>
  adminInterviewFirstLevelCategories.value.find((item) => item.key === adminInterviewFirstLevelKey.value)
  || adminInterviewFirstLevelCategories.value[0]
  || interviewFirstLevelCategories[0]
)
const adminInterviewTagOptions = computed(() => {
  const activeFirstLevel = adminInterviewActiveFirstLevel.value
  if (!activeFirstLevel || activeFirstLevel.key === 'all') {
    return []
  }

  const configuredTags = Array.isArray(activeFirstLevel.subCategories) ? activeFirstLevel.subCategories : []
  if (configuredTags.length) {
    return configuredTags.map((item) => ({
      key: item.key,
      label: item.label,
      count: item.key === 'all'
        ? resolveInterviewFirstLevelCount(activeFirstLevel.key)
        : resolveInterviewTagCount(activeFirstLevel.key, item.label)
    }))
  }

  const derivedTags = collectInterviewTagsByFirstLevel(activeFirstLevel.key)
  if (!derivedTags.length) {
    return []
  }

  return [
    {
      key: 'all',
      label: '全部',
      count: resolveInterviewFirstLevelCount(activeFirstLevel.key)
    },
    ...derivedTags.map((label) => ({
      key: slugifyInterviewTag(label),
      label,
      count: resolveInterviewTagCount(activeFirstLevel.key, label)
    }))
  ]
})

// 监听分类变化，动态加载标签并清空已选中的标签
watch(() => draftForm.category, (newCategory) => {
  if (currentType.value === 'interview' && newCategory) {
    draftForm.selectedTags = [] // 分类变化时清空已选中的标签
    fetchTagsByCategory(newCategory)
  }
})

// 根据用户端面经分类配置同步生成后台可选标签
function fetchTagsByCategory(category) {
  availableTags.value = getInterviewTagOptions(category)
}

watch(adminInterviewTagOptions, (options) => {
  if (!options.some((item) => item.key === adminInterviewSelectedTagKey.value)) {
    adminInterviewSelectedTagKey.value = 'all'
  }
})

// 初始化时加载一次标签（默认分类）
if (currentType.value === 'interview') {
  fetchTagsByCategory(draftForm.category)
}

const summary = computed(() => currentSummary.value || {
  onlineUsers: 0,
  totalViews: 0,
  totalComments: 0,
  editsToday: 0,
  totalContents: 0,
  lastUpdatedAt: ''
})

const activeModule = computed(() => moduleOptions.value.find((item) => item.key === currentType.value) || moduleOptions.value[0])
/**
 * 只在后端具备封面持久化能力的模块中展示封面上传入口。
 * 面经与期刊当前没有对应封面字段写库链路，前端需要主动收口避免出现"保存成功但回显丢失"的假象。
 */
const supportsCoverUpload = computed(() => ['tech', 'ai'].includes(currentType.value))
/**
 * 给暂不支持封面持久化的模块展示明确提示文案。
 * 直接在表单里说明能力边界，减少运营同学反复保存后误以为系统写库失败。
 */
const coverFieldNotice = computed(() => {
  if (currentType.value === 'interview') {
    return '面经模块当前只支持正文内容展示，暂不支持单独封面图持久化。'
  }
  if (currentType.value === 'world') {
    return '期刊模块当前使用封面文案组合展示，暂不支持单独封面图持久化。'
  }
  return '当前模块支持封面图上传与保存。'
})
const currentRecords = computed(() => adminStore.getContentList(currentType.value))
const pageTitle = computed(() => {
  if (isBooksImportPage.value) {
    return '书籍导入'
  }
  if (isBooksOverviewPage.value) {
    return '书籍总览'
  }
  if (isBookSubPage.value) {
    return '书籍管理'
  }
  if (isStatsPage.value) {
    return '数据分析'
  }
  if (isCommentsPage.value) {
    return '评论管理'
  }
  return activeModule.value.topbarTitle
})

// 目的: 统一沉淀后台数据分析模块的核心概览卡，避免图表说明和摘要数字口径割裂。
// 逻辑: 从后端 dashboard 聚合结果中提取总量、热度和模块峰值，直接服务数据分析面板和评论管理摘要区。
const analyticsHighlights = computed(() => {
  const hottestModule = [...moduleStats.value].sort((left, right) => Number(right.viewCount || 0) - Number(left.viewCount || 0))[0]
  const hottestContent = hotContents.value[0]
  const pendingCount = commentRecords.value.reduce((total, item) => total + Number(item.pendingCount || 0), 0)
  return [
    {
      label: '全站浏览',
      value: formatCount(summary.value.totalViews),
      note: `在线 ${summary.value.onlineUsers} 人`
    },
    {
      label: '评论待跟进',
      value: formatCount(pendingCount),
      note: `总评论 ${formatCount(summary.value.totalComments)}`
    },
    {
      label: '流量峰值模块',
      value: hottestModule?.moduleLabel || '暂无数据',
      note: `${formatCount(hottestModule?.viewCount || 0)} 浏览`
    },
    {
      label: '当前热度内容',
      value: hottestContent?.title || '暂无数据',
      note: `${formatCount(hottestContent?.viewCount || 0)} 浏览`
    }
  ]
})

// 目的: 将三个内容模块的聚合结果拆成独立摘要卡，确保数据分析页能直接看到每个模块的变化数据。
// 逻辑: 统一使用 dashboard 的模块统计结果生成卡片，避免页面层再次分散查询不同模块的数据口径。
const moduleSnapshotCards = computed(() => {
  return moduleStats.value.map((item) => ({
    key: item.moduleKey,
    label: item.moduleLabel,
    value: formatCount(item.viewCount),
    note: `${formatCount(item.contentCount)} 内容 / ${formatCount(item.commentCount)} 评论`
  }))
})

// 业务目的：表格和统计卡都依赖同一份模块聚合结果，保证模板每块数字表达一致。
// 业务逻辑：统一聚合浏览、评论、当月新增和推荐类计数，避免顶部和右侧面板口径不一致。
const moduleSummary = computed(() => {
  return currentRecords.value.reduce((result, record) => {
    result.contentCount += 1
    result.viewCount += Number(record.viewCount || record.todayReads || 0)
    result.commentCount += Number(record.commentCount || 0)
    result.currentMonthCount += isCurrentMonth(record.publishedAt) ? 1 : 0
    result.featuredCount += Number(isFocusRecord(record))
    return result
  }, {
    contentCount: 0,
    viewCount: 0,
    commentCount: 0,
    currentMonthCount: 0,
    featuredCount: 0
  })
})

const filterTabs = computed(() => {
  draftVersion.value // eslint-disable-line no-unused-expressions
  const draftCount = drafts.value.length
  const totalCount = currentRecords.value.length
  const draftTab = { key: 'draft', label: `待编辑${draftCount ? ` (${draftCount})` : ''}` }
  const allTab = { key: 'all', label: `全部${totalCount ? ` (${totalCount})` : ''}` }

  if (currentType.value === 'tech') {
    return [
      allTab,
      { key: 'featured', label: '精选' },
      { key: 'vip', label: 'VIP' },
      { key: 'history', label: '最近浏览' },
      draftTab
    ]
  }

  if (currentType.value === 'world') {
    return [
      allTab,
      { key: 'recommended', label: '高推荐' },
      { key: 'latest', label: '最新期号' },
      { key: 'cover', label: '封面重点' },
      draftTab
    ]
  }

  if (currentType.value === 'interview') {
    return [
      allTab,
      { key: 'easy', label: '基础' },
      { key: 'medium', label: '中等' },
      { key: 'hard', label: '困难' },
      draftTab
    ]
  }

  return [
    allTab,
    { key: 'recommended', label: '推荐' },
    { key: 'today', label: '今日热点' },
    { key: 'high-heat', label: '高热度' },
    draftTab
  ]
})

const filteredRecords = computed(() => {
  draftVersion.value // eslint-disable-line no-unused-expressions
  if (currentFilter.value === 'draft') {
    return drafts.value.map((d) => ({
      ...d.formData,
      _draftId: d.draftKey,
      _type: d.contentType,
      _savedAt: d.savedAt,
      _isLocalDraft: true,
      id: d.draftKey,
      type: d.contentType,
      title: d.title,
      contentHtml: d.formData?.contentHtml || '',
      summary: d.formData?.summary || ''
    }))
  }

  const baseList = currentRecords.value
    .filter((record) => matchFilter(record, currentFilter.value))
    .filter((record) => matchInterviewBrowser(record))
  const searchValue = keyword.value.trim().toLowerCase()
  if (!searchValue) {
    return baseList
  }

  return baseList.filter((record) => {
    return [
      record.title,
      record.summary,
      record.coverSummary,
      record.authorName,
      record.issueLabel,
      record.track,
      record.difficulty,
      ...(record.highlights || []),
      ...(record.tags || [])
    ]
      .filter(Boolean)
      .some((item) => String(item).toLowerCase().includes(searchValue))
  })
})

const currentRecentEdits = computed(() => {
  const scopedLogs = recentEdits.value.filter((item) => item.contentType === currentType.value)
  return (scopedLogs.length ? scopedLogs : recentEdits.value).slice(0, 4)
})

// 目的: 把后台评论管理记录转换成页面可直接渲染的评论巡检列表。
// 逻辑: 优先展示后端已经按热度排序的记录，同时在前端补齐空态兜底，保证评论模块首屏稳定。
const commentManagementList = computed(() => {
  return commentRecords.value.length
    ? commentRecords.value
    : []
})

// 目的: 生成评论管理区的摘要卡片，帮助运营先看风险再看明细。
// 逻辑: 对后端返回的评论记录按优先级和状态二次汇总，避免模板层散落多处 reduce 逻辑。
const commentSummaryCards = computed(() => {
  const urgentCount = countBy(commentManagementList.value, (item) => item.priority === 'urgent')
  const trackingCount = countBy(commentManagementList.value, (item) => item.status === 'tracking' || item.status === 'pending')
  const pendingCount = commentManagementList.value.reduce((total, item) => total + Number(item.pendingCount || 0), 0)
  const engagementPeak = [...commentManagementList.value].sort((left, right) => Number(right.engagementRate || 0) - Number(left.engagementRate || 0))[0]
  return [
    {
      label: '高优先级内容',
      value: `${urgentCount}`,
      note: '需要优先安排答疑'
    },
    {
      label: '待跟进评论',
      value: formatCount(pendingCount),
      note: '结合热度折算后的待办量'
    },
    {
      label: '持续跟进项',
      value: `${trackingCount}`,
      note: '适合运营日内复查'
    },
    {
      label: '最高互动率',
      value: engagementPeak ? `${Number(engagementPeak.engagementRate || 0).toFixed(1)}%` : '0%',
      note: engagementPeak?.contentTitle || '暂无数据'
    }
  ]
})

// 目的: 将评论管理页的三模块评论变化拆成独立摘要卡，便于直接横向比较各模块评论压力。
// 逻辑: 复用 dashboard 的模块评论统计，即使某个模块当前评论为 0 也明确展示，避免误判为未加载。
const commentModuleCards = computed(() => {
  return moduleStats.value.map((item) => ({
    key: item.moduleKey,
    label: item.moduleLabel,
    value: formatCount(item.commentCount),
    note: `${formatCount(item.viewCount)} 浏览 / ${formatCount(item.contentCount)} 内容`
  }))
})

const sideCategoryItems = computed(() => {
  if (currentType.value === 'tech') {
    return [
      { name: '前端开发', count: countBy(currentRecords.value, (item) => item.category === 'frontend'), color: '#e8c97e' },
      { name: '后端架构', count: countBy(currentRecords.value, (item) => item.category === 'backend'), color: '#7ab0e0' },
      { name: 'VIP 深读', count: countBy(currentRecords.value, (item) => Boolean(item.vip)), color: '#70c99a' },
      { name: '精选分发', count: countBy(currentRecords.value, (item) => Boolean(item.featured)), color: '#e07070' }
    ]
  }

  if (currentType.value === 'world') {
    return currentRecords.value.slice(0, 4).map((item, index) => ({
      name: item.issueLabel || item.title,
      count: `${Number(item.recommendation || 0).toFixed(1)}%`,
      color: ['#e8c97e', '#7ab0e0', '#70c99a', '#e07070'][index] || '#e8c97e'
    }))
  }

  if (currentType.value === 'interview') {
    return [
      { name: '基础难度', count: countBy(currentRecords.value, (item) => item.track === 'easy'), color: '#4ade80' },
      { name: '中等难度', count: countBy(currentRecords.value, (item) => item.track === 'medium'), color: '#facc15' },
      { name: '困难难度', count: countBy(currentRecords.value, (item) => item.track === 'hard'), color: '#f87171' },
      { name: '面经合计', count: currentRecords.value.length, color: '#7ab0e0' }
    ]
  }

  return [
    { name: 'Agent 落地', count: countBy(currentRecords.value, (item) => item.track === 'agent'), color: '#e8c97e' },
    { name: '多模态交互', count: countBy(currentRecords.value, (item) => item.track === 'multimodal'), color: '#7ab0e0' },
    { name: '模型基础设施', count: countBy(currentRecords.value, (item) => item.track === 'infra'), color: '#70c99a' },
    { name: '今日热点', count: countBy(currentRecords.value, (item) => Boolean(item.today)), color: '#e07070' }
  ]
})

// 目的: 输出后台数据分析区的热点内容榜，辅助运营将图表结论落到具体内容对象。
// 逻辑: 使用后端 dashboard 已经聚合好的热度内容列表，并在前端补充排名序号与进度条宽度。
const hotContentRankings = computed(() => {
  const highestViewCount = Math.max(...hotContents.value.map((item) => Number(item.viewCount || 0)), 0)
  return hotContents.value.map((item, index) => ({
    ...item,
    rank: index + 1,
    ratio: highestViewCount > 0 ? Math.max(12, Math.round((Number(item.viewCount || 0) / highestViewCount) * 100)) : 0
  }))
})

const progressInfo = computed(() => {
  const targetMap = { tech: 20, world: 6, ai: 10, interview: 10 }
  const target = targetMap[currentType.value] || 10
  const current = moduleSummary.value.currentMonthCount
  const percent = Math.min(100, Math.round((current / target) * 100))
  const remain = Math.max(0, target - current)
  return {
    current,
    target,
    percent,
    note: remain > 0 ? `距离目标还差 ${remain} 条，继续补齐内容节奏。` : '本月目标已完成，当前可以继续扩充重点内容。'
  }
})

// ── 草稿管理（MySQL 存储） ─────────────────────────────

async function loadDrafts(type) {
  try {
    drafts.value = await listAdminDrafts(type)
    draftVersion.value += 1
  } catch {
    drafts.value = []
  }
}

let autoSaveTimer = null

function scheduleAutoSave() {
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(async () => {
    if (!dialogVisible.value) return
    const hasTitle = draftForm.title.trim().length > 0
    const hasContent = (draftForm.contentHtml || '').replace(/<[^>]*>/g, '').trim().length > 0
    if (!hasTitle && !hasContent) return
    await saveDraftToServer(false)
  }, 2000)
}

/**
 * 持久化后台草稿
 * 统一提交草稿并按需触发页面提示，避免提示函数被同名参数覆盖
 */
async function saveDraftToServer(shouldShowToast = true) {
  const type = currentType.value
  const data = { ...draftForm }
  delete data._draftId
  const params = {
    draftKey: draftForm._draftId || '',
    contentType: type,
    title: draftForm.title || '',
    contentHtml: draftForm.contentHtml || '',
    data
  }
  try {
    const saved = await saveAdminDraft(params)
    draftForm._draftId = saved.draftKey
    await loadDrafts(type)
    if (shouldShowToast) {
      showToast('草稿已保存', 'success')
    }
  } catch (e) {
    if (shouldShowToast) {
      showToast('草稿保存失败: ' + (e.message || '网络错误'), 'error')
    }
  }
}

watch(
  () => [draftForm.title, draftForm.contentHtml, draftForm.summary],
  () => {
    if (dialogVisible.value) {
      scheduleAutoSave()
    }
  },
  { deep: true }
)

async function saveDraftLocally() {
  await saveDraftToServer(true)
  if (!isEditing.value) {
    setTimeout(() => {
      closeDialog()
    }, 1000)
  }
}

function restoreDraft(draft) {
  isEditing.value = false
  // draft 来自 filteredRecords，字段已从 d.formData 展开到顶层
  const restored = { ...draft }
  delete restored._draftId
  delete restored._type
  delete restored._savedAt
  delete restored._isLocalDraft
  delete restored.id
  delete restored.type
  Object.assign(draftForm, createEmptyDraft(), restored)
  draftForm._draftId = draft._draftId
  dialogVisible.value = true
}

async function removeDraftRecord(draft) {
  const confirmed = await showConfirm('删除草稿', `确认删除草稿《${draft.title || '无标题'}》吗？`)
  if (!confirmed) return
  try {
    await deleteAdminDraft(draft._draftId)
    loadDrafts(currentType.value)
    showToast('草稿已删除', 'success')
  } catch {
    showToast('删除草稿失败', 'error')
  }
}

watch(routeSection, (nextSection) => {
  keyword.value = ''
  currentFilter.value = 'all'
  resetAdminInterviewBrowser()
  if (contentSectionKeys.includes(nextSection)) {
    currentType.value = nextSection
    loadDrafts(nextSection)
  }
  adminStore.stopRealtime()
  adminStore.startRealtime(contentSectionKeys.includes(nextSection) ? nextSection : undefined)
}, { immediate: true })

onBeforeUnmount(() => {
  clearTimeout(autoSaveTimer)
  adminStore.stopRealtime()
})

function switchType(type) {
  router.push(`/admin/${type}`)
}

/**
 * 从管理后台侧边栏进入独立的书籍导入页。
 * 保持书籍导入只属于后台系统能力，避免用户端页面直接暴露后台操作入口。
 */
function openBookImportPage() {
  router.push('/admin/books-import')
}

function openBookOverviewPage() {
  router.push('/admin/books-list')
}

// 业务目的：按模块 key 返回左侧导航的彩色图标字符，保证每个模块都有稳定图标。
// 业务逻辑：已配置模块直接返回对应 emoji，未命中时回退到文章图标，避免导航出现空白占位。
function resolveSidebarEmoji(iconKey) {
  return sidebarEmojiMap[iconKey] || sidebarEmojiMap.tech
}

function scrollToAnalytics() {
  router.push('/admin/stats')
}

function scrollToCommentSection() {
  router.push('/admin/comment')
}

function matchFilter(record, filterKey) {
  if (filterKey === 'all') {
    return true
  }

  if (currentType.value === 'tech') {
    return {
      featured: Boolean(record.featured),
      vip: Boolean(record.vip),
      history: Boolean(record.history)
    }[filterKey]
  }

  if (currentType.value === 'world') {
    return {
      recommended: Number(record.recommendation || 0) >= 80,
      latest: isCurrentMonth(record.publishedAt),
      cover: Boolean(record.coverSummary)
    }[filterKey]
  }

  if (currentType.value === 'interview') {
    return {
      easy: record.track === 'easy',
      medium: record.track === 'medium',
      hard: record.track === 'hard'
    }[filterKey]
  }

  return {
    recommended: Boolean(record.recommended),
    today: Boolean(record.today),
    'high-heat': Number(record.heat || 0) >= 80
  }[filterKey]
}

function matchInterviewBrowser(record) {
  if (currentType.value !== 'interview') {
    return true
  }

  const firstLevelKey = adminInterviewFirstLevelKey.value
  if (firstLevelKey !== 'all' && resolveInterviewRecordFirstLevelKey(record.category) !== firstLevelKey) {
    return false
  }

  if (adminInterviewSelectedTagKey.value === 'all') {
    return true
  }

  const selectedTag = adminInterviewTagOptions.value.find((item) => item.key === adminInterviewSelectedTagKey.value)
  if (!selectedTag) {
    return true
  }

  return interviewRecordHasTag(record, selectedTag.label)
}

function resolveMeta(record) {
  if (record.type === 'tech') {
    return `${record.publishedAt || '--'} / ${record.authorName || '匿名作者'}`
  }
  if (record.type === 'world') {
    return `${record.publishedAt || '--'} / ${record.issueLabel || '未设置期号'}`
  }
  if (record.type === 'interview') {
    return `${record.publishedAt || '--'} / ${(record.tags || []).slice(0, 3).join(', ') || '无标签'}`
  }
  return `${record.publishedAt || '--'} / ${record.authorName || '匿名来源'}`
}

function resolveCategory(record) {
  if (record.type === 'tech') {
    return record.category === 'frontend' ? '前端开发' : '后端架构'
  }
  if (record.type === 'world') {
    return record.issueLabel || '期刊'
  }
  if (record.type === 'interview') {
    record.category = normalizeInterviewCategoryValue(record.category)
    const interviewCategoryMap = {
      frontend: '前端',
      java: 'Java后端',
      agent: 'Agent开发',
      llm: '大模型原理'
    }
    return interviewCategoryMap[record.category] || record.category || '综合'
  }
  return record.track || 'AI'
}

function resolveStatus(record) {
  if (record.type === 'tech') {
    if (record.featured) {
      return { label: '精选', className: 'badge-published' }
    }
    if (record.vip) {
      return { label: 'VIP', className: 'badge-review' }
    }
    return { label: '已发布', className: 'badge-draft' }
  }

  if (record.type === 'world') {
    if (Number(record.recommendation || 0) >= 80) {
      return { label: '重点推荐', className: 'badge-published' }
    }
    if (Number(record.recommendation || 0) >= 75) {
      return { label: '持续关注', className: 'badge-review' }
    }
    return { label: '常规期号', className: 'badge-draft' }
  }

  if (record.type === 'interview') {
    const diffMap = { easy: '基础', medium: '中等', hard: '困难' }
    const diff = diffMap[record.track] || '基础'
    return { label: diff, className: record.track === 'hard' ? 'badge-review' : record.track === 'medium' ? 'badge-published' : 'badge-draft' }
  }

  if (record.today) {
    return { label: '今日热点', className: 'badge-review' }
  }
  if (record.recommended) {
    return { label: '推荐', className: 'badge-published' }
  }
  return { label: '已收录', className: 'badge-draft' }
}

function resolveReadValue(record) {
  return formatCount(record.viewCount || record.todayReads || 0)
}

function resolveActivityColor(actionType) {
  return {
    update: '#70c99a',
    delete: '#e07070',
    'batch-import': '#e8c97e'
  }[actionType] || '#7ab0e0'
}

function resolveCommentPriorityClass(priority) {
  return {
    urgent: 'badge-comment-urgent',
    focus: 'badge-comment-focus',
    routine: 'badge-comment-routine'
  }[priority] || 'badge-comment-routine'
}

function resolveCommentStatusClass(status) {
  return {
    pending: 'badge-comment-urgent',
    tracking: 'badge-comment-focus',
    stable: 'badge-comment-routine'
  }[status] || 'badge-comment-routine'
}

function resolveActivityText(item) {
  return `${resolveActionLabel(item.actionType)}《${item.contentTitle}》`
}

function resolveActionLabel(actionType) {
  return {
    update: '内容已更新',
    delete: '内容已删除',
    'batch-import': '批量导入完成'
  }[actionType] || '内容已变更'
}

function isFocusRecord(record) {
  if (record.type === 'tech') {
    return record.featured || record.vip
  }
  if (record.type === 'world') {
    return Number(record.recommendation || 0) >= 80
  }
  if (record.type === 'interview') {
    return record.track === 'hard' || Number(record.collectCount || 0) >= 10
  }
  return record.today || record.recommended
}

function countBy(records, predicate) {
  return records.filter(predicate).length
}

function resetAdminInterviewBrowser() {
  adminInterviewFirstLevelKey.value = 'all'
  adminInterviewSelectedTagKey.value = 'all'
}

function selectAdminInterviewFirstLevel(firstLevelKey) {
  currentFilter.value = 'all'
  adminInterviewFirstLevelKey.value = firstLevelKey
  adminInterviewSelectedTagKey.value = 'all'
}

function selectAdminInterviewTag(tagKey) {
  currentFilter.value = 'all'
  adminInterviewSelectedTagKey.value = tagKey
}

function resolveInterviewFirstLevelCount(firstLevelKey) {
  if (firstLevelKey === 'all') {
    return currentRecords.value.length
  }
  return countBy(currentRecords.value, (record) => resolveInterviewRecordFirstLevelKey(record.category) === firstLevelKey)
}

function resolveInterviewTagCount(firstLevelKey, tagLabel) {
  return countBy(currentRecords.value, (record) =>
    resolveInterviewRecordFirstLevelKey(record.category) === firstLevelKey
    && interviewRecordHasTag(record, tagLabel)
  )
}

function collectInterviewTagsByFirstLevel(firstLevelKey) {
  return Array.from(new Set(
    currentRecords.value
      .filter((record) => resolveInterviewRecordFirstLevelKey(record.category) === firstLevelKey)
      .flatMap((record) => Array.isArray(record.tags) ? record.tags : [])
      .map((tag) => normalizeInterviewTagLabel(tag))
      .filter(Boolean)
  ))
}

function resolveInterviewRecordFirstLevelKey(category) {
  const normalizedCategory = normalizeInterviewCategoryValue(category)
  if (normalizedCategory === 'java') {
    return 'backend'
  }
  if (normalizedCategory === 'frontend') {
    return 'frontend'
  }
  if (normalizedCategory === 'agent') {
    return 'agent'
  }
  if (normalizedCategory === 'llm') {
    return 'llm'
  }
  return 'all'
}

function normalizeInterviewCategoryValue(category) {
  const normalizedCategory = String(category || '').trim().toLowerCase()
  const categoryMap = {
    frontend: 'frontend',
    '前端': 'frontend',
    java: 'java',
    backend: 'java',
    'java后端': 'java',
    'java 后端': 'java',
    'java基础': 'java',
    agent: 'agent',
    'agent开发': 'agent',
    'agent 开发': 'agent',
    llm: 'llm',
    '大模型原理': 'llm'
  }

  return categoryMap[normalizedCategory] || normalizedCategory
}

function interviewRecordHasTag(record, tagLabel) {
  const normalizedTag = normalizeInterviewTagLabel(tagLabel)
  return (record.tags || []).some((tag) => normalizeInterviewTagLabel(tag) === normalizedTag)
}

function normalizeInterviewTagLabel(tag) {
  const normalizedTag = String(tag || '').trim().toLowerCase()
  const tagMap = {
    mysql: 'MySQL',
    java: 'Java',
    redis: 'Redis',
    vue: 'Vue',
    react: 'React',
    typescript: 'TypeScript',
    javascript: 'JavaScript',
    htmlcss: 'HTML/CSS',
    'html/css': 'HTML/CSS',
    jvm: 'JVM',
    'spring boot': 'Spring Boot',
    springboot: 'Spring Boot',
    elasticsearch: 'Elasticsearch'
  }

  return tagMap[normalizedTag] || String(tag || '').trim()
}

function slugifyInterviewTag(tag) {
  return String(tag || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

function isCurrentMonth(value) {
  if (!value) {
    return false
  }
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return false
  }
  const now = new Date()
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth()
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}k`
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return `${count}`
}

// 目的: 统一从 dashboard 模块统计里读取左侧导航的模块数量。
// 逻辑: 优先使用后端聚合结果，若 dashboard 尚未回填则退回当前缓存列表，避免导航徽标出现空值。
function resolveModuleCount(type) {
  const targetModule = moduleStats.value.find((item) => item.moduleKey === type)
  if (targetModule) {
    return Number(targetModule.contentCount || 0)
  }
  return adminStore.getContentList(type).length
}

async function refreshPageData() {
  if (isBookSubPage.value) {
    await router.replace({
      path: route.fullPath,
      query: {
        ...route.query,
        _refresh: `${Date.now()}`
      }
    })
    return
  }
  if (isContentPage.value) {
    await adminStore.refreshAll(currentType.value).catch(() => null)
    return
  }
  await adminStore.refreshAll().catch(() => null)
}

function refreshCurrentType() {
  refreshPageData()
}

function openCreateDialog() {
  isEditing.value = false
  Object.assign(draftForm, createEmptyDraft())
  dialogVisible.value = true
  // 打开创建对话框时，根据当前分类加载标签
  if (currentType.value === 'interview') {
    nextTick(() => {
      fetchTagsByCategory(draftForm.category)
    })
  }
}

function openEditDialog(record) {
  isEditing.value = true
  Object.assign(draftForm, createDraftFromRecord(record))
  dialogVisible.value = true
  // 打开编辑对话框时，根据记录的分类加载标签
  if (currentType.value === 'interview') {
    nextTick(() => {
      fetchTagsByCategory(draftForm.category)
    })
  }
}

function closeDialog() {
  dialogVisible.value = false
}

async function submitDraft() {
  const payload = buildSavePayload(draftForm, currentType.value)
  await adminStore.saveContent(currentType.value, payload)
  // 发布成功后清除草稿
  if (draftForm._draftId) {
    try {
      await deleteAdminDraft(draftForm._draftId)
    } catch {
      // 草稿删除失败不影响发布
    }
    await loadDrafts(currentType.value)
  }
  closeDialog()
  showToast(isEditing.value ? '修改已保存' : '发布成功', 'success')
}

async function removeRecord(record) {
  const confirmed = await showConfirm('删除内容', `确认删除《${record.title}》吗？`)
  if (!confirmed) return
  await adminStore.removeContent(record.type, record.id)
  showToast('内容已删除', 'success')
}

/**
 * 区分批量表格导入与单篇文档导入入口，避免同一个上传框同时承担两套业务语义。
 * Excel 入口继续服务结构化批量写入，文档入口则服务语雀与飞书等编辑器导出的单篇正文导入。
 */
function triggerBatchImport() {
  excelFileInputRef.value?.click()
}

/**
 * 为后台新增一键导入外部文档入口，让运营直接从本地选择语雀或飞书导出的文章文件。
 * 未打开弹窗时先进入新建态，再调起系统文件选择器，保证导入内容有明确落点。
 */
function triggerDocumentImport() {
  if (!dialogVisible.value) {
    openCreateDialog()
  }
  documentImportInputRef.value?.click()
}

function triggerCoverSelect() {
  coverFileInputRef.value?.click()
}

async function handleCoverFileSelect(event) {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件', 'error')
    event.target.value = ''
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    showToast('图片大小不能超过 10MB', 'error')
    event.target.value = ''
    return
  }

  coverUploading.value = true
  try {
    const result = await uploadCoverImage(file)
    draftForm.coverUrl = result.url
    showToast('封面图片上传成功', 'success')
  } catch (error) {
    showToast(error.message || '封面图片上传失败', 'error')
  } finally {
    coverUploading.value = false
    event.target.value = ''
  }
}

function removeCover() {
  draftForm.coverUrl = ''
}

async function handleExcelImportFile(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  const XLSX = await loadXlsxLibrary()
  const buffer = await file.arrayBuffer()
  const workbook = XLSX.read(buffer)
  const worksheet = workbook.Sheets[workbook.SheetNames[0]]
  const rows = XLSX.utils.sheet_to_json(worksheet, { defval: '' })
  const records = rows.map((row) => buildImportRecord(row, currentType.value))
  await adminStore.batchImportContent(currentType.value, records)
  event.target.value = ''
}

/**
 * 把外部编辑工具导出的本地文件转成后台可接收的 HTML 正文，减少人工复制粘贴成本。
 * 先在前端按文件类型做轻量解析，再统一调用后端预处理接口完成 HTML 清洗与 MinIO 图片迁移。
 */
async function handleDocumentImportFile(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  importPreviewLoading.value = true
  importFileName.value = file.name || ''
  try {
    const parsed = await parseImportedDocumentFile(file)
    draftForm.importHtml = parsed.html
    draftForm.importSourceType = parsed.sourceType
    draftForm.importSourceUrl = ''
    if (!draftForm.title && parsed.title) {
      draftForm.title = parsed.title
    }
    if (!draftForm.summary && parsed.summary) {
      draftForm.summary = parsed.summary
    }
    await applyImportedHtmlSafe({
      html: parsed.html,
      sourceType: parsed.sourceType,
      fileName: file.name,
      skipLoading: true
    })
  } catch (error) {
    showToast(error.message || '文档导入失败', 'error')
  } finally {
    importPreviewLoading.value = false
    event.target.value = ''
  }
}

async function handleExportCurrentModule() {
  const XLSX = await loadXlsxLibrary()
  const rows = currentRecords.value.map((record) => buildExportRow(record, currentType.value))
  const worksheet = XLSX.utils.json_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, activeModule.value.shortLabel)
  XLSX.writeFile(workbook, `peakstars-${currentType.value}-content.xlsx`)
}

/**
 * 把 docx、markdown、html 与 txt 等常见导出文件统一转成后台富文本可消费的 HTML。
 * 前端按扩展名选择最合适的解析器，尽量保留原始标题层级、列表、表格与图片结构，再交给服务端做最终清洗。
 */
async function parseImportedDocumentFile(file) {
  const extension = resolveFileExtension(file.name)
  const sourceType = resolveSourceTypeByFileName(file.name)
  if (extension === 'docx') {
    return parseDocxFile(file, sourceType)
  }
  if (extension === 'md' || extension === 'markdown') {
    return parseMarkdownFile(file, sourceType)
  }
  if (extension === 'html' || extension === 'htm') {
    return parseHtmlFile(file, sourceType)
  }
  if (extension === 'txt') {
    return parseTextFile(file, sourceType)
  }
  throw new Error('当前仅支持 docx、html、md、markdown、txt 文档导入')
}

/**
 * 保留飞书与语雀导出的 Word 版式结构，让后台导入后尽量贴近原文排版。
 * 使用 mammoth 把 docx 转成 HTML，并把内嵌图片转成 base64，后续交给后端统一迁移到 MinIO。
 */
async function parseDocxFile(file, sourceType) {
  const mammoth = await loadMammothLibrary()
  const arrayBuffer = await file.arrayBuffer()
  const result = await mammoth.convertToHtml(
    { arrayBuffer },
    {
      convertImage: mammoth.images.inline(async (element) => ({
        src: `data:${element.contentType};base64,${await element.read('base64')}`
      }))
    }
  )
  const html = String(result.value || '').trim()
  const plainText = htmlToPlainText(html)
  return {
    html,
    sourceType,
    title: extractTitleFromHtml(html, file.name),
    summary: buildSummaryFromPlainText(plainText)
  }
}

/**
 * 把 markdown 文档保留为结构化 HTML，避免后台只导入成一整段纯文本。
 * 通过 markdown-it 渲染标题、代码块、表格和任务列表，再继续复用统一的预处理入口。
 */
async function parseMarkdownFile(file, sourceType) {
  const markdown = await loadMarkdownRuntime()
  const rawText = await file.text()
  const normalizedMarkdown = rebuildMarkdownOrderedList(normalizeMarkdownSource(rawText))
  const html = normalizeImportedHtml(markdown.render(normalizedMarkdown))
  return {
    html,
    sourceType,
    title: extractTitleFromMarkdown(normalizedMarkdown, file.name),
    summary: buildSummaryFromPlainText(normalizedMarkdown)
  }
}

/**
 * 兼容语雀与飞书直接导出的 HTML 文件，最大程度保留原始结构。
 * 优先提取 body 区域正文，避免整页样式与脚本被一并带入后台富文本。
 */
async function parseHtmlFile(file, sourceType) {
  const rawText = await file.text()
  const parser = new DOMParser()
  const document = parser.parseFromString(rawText, 'text/html')
  const html = normalizeImportedHtml(document.body?.innerHTML?.trim() || rawText)
  const title = document.querySelector('h1')?.textContent?.trim() || document.title || stripFileExtension(file.name)
  return {
    html,
    sourceType,
    title,
    summary: buildSummaryFromPlainText(document.body?.textContent || htmlToPlainText(html))
  }
}

/**
 * 让纯文本文档导入后仍保留段落节奏，避免直接贴入编辑器变成一整块内容。
 * 按空行拆段并转成标准段落 HTML，再从首段补齐标题与摘要。
 */
async function parseTextFile(file, sourceType) {
  const rawText = await file.text()
  const html = normalizeImportedHtml(textToParagraphHtml(rawText))
  return {
    html,
    sourceType,
    title: extractTitleFromPlainText(rawText, file.name),
    summary: buildSummaryFromPlainText(rawText)
  }
}

/**
 * 让外部文档导入后的正文继续复用服务端清洗与资源迁移链路，保证入库内容口径一致。
 * 支持直接消费文件解析结果，也支持手动粘贴 HTML 后再次执行预处理，避免双入口分叉维护。
 */
async function applyImportedHtml(payload = {}) {
  const rawHtml = String(payload.html || draftForm.importHtml || '').trim()
  if (!rawHtml) {
    showToast('请先粘贴外部 HTML 内容', 'warning')
    return
  }

  importPreviewLoading.value = true
  try {
    const result = await previewImportedAdminContent(currentType.value, {
      contentHtml: rawHtml,
      sourceType: payload.sourceType || draftForm.importSourceType || 'html',
      sourceUrl: draftForm.importSourceUrl || '',
      migrateAssets: true
    })

    draftForm.contentHtml = normalizeImportedHtml(result.normalizedHtml || '')
    if (!draftForm.title) {
      draftForm.title = extractTitleFromHtml(result.normalizedHtml || rawHtml, payload.fileName || importFileName.value)
    }
    if (!draftForm.summary && result.plainText) {
      draftForm.summary = buildSummaryFromPlainText(result.plainText)
    }
    if (!draftForm.coverUrl && result.migratedCoverUrl) {
      draftForm.coverUrl = result.migratedCoverUrl
    }

    const migratedCount = Array.isArray(result.assets)
      ? result.assets.filter((item) => item.status === 'migrated').length
      : 0
    const warningCount = Array.isArray(result.warnings) ? result.warnings.length : 0
    showToast(`导入完成，已处理 ${migratedCount} 个资源${warningCount ? `，${warningCount} 个告警` : ''}`, 'success')
  } catch (error) {
    showToast(error.message || '导入预处理失败', 'error')
  } finally {
    if (shouldManageLoading) {
      importPreviewLoading.value = false
    }
  }
}

/**
 * 兜底接管外部文档预处理入口，确保文件导入与手动粘贴都走稳定的清洗链路。
 * 显式管理加载态、标题摘要回填与提示文案，避免旧函数历史逻辑影响新导入能力。
 */
async function applyImportedHtmlSafe(payload = {}) {
  const rawHtml = String(payload.html || draftForm.importHtml || '').trim()
  if (!rawHtml) {
    showToast('请先选择文档或粘贴外部 HTML 内容', 'warning')
    return
  }

  const shouldManageLoading = !payload.skipLoading
  if (shouldManageLoading) {
    importPreviewLoading.value = true
  }
  try {
    const result = await previewImportedAdminContent(currentType.value, {
      contentHtml: rawHtml,
      sourceType: payload.sourceType || draftForm.importSourceType || 'html',
      sourceUrl: draftForm.importSourceUrl || '',
      migrateAssets: true
    })

    draftForm.contentHtml = normalizeImportedHtml(result.normalizedHtml || '')
    if (!draftForm.title) {
      draftForm.title = extractTitleFromHtml(result.normalizedHtml || rawHtml, payload.fileName || importFileName.value)
    }
    if (!draftForm.summary && result.plainText) {
      draftForm.summary = buildSummaryFromPlainText(result.plainText)
    }
    if (!draftForm.coverUrl && result.migratedCoverUrl) {
      draftForm.coverUrl = result.migratedCoverUrl
    }

    const migratedCount = Array.isArray(result.assets)
      ? result.assets.filter((item) => item.status === 'migrated').length
      : 0
    const warningCount = Array.isArray(result.warnings) ? result.warnings.length : 0
    const fileNameLabel = payload.fileName ? `《${payload.fileName}》` : '当前内容'
    showToast(`已导入 ${fileNameLabel}，处理 ${migratedCount} 个资源${warningCount ? `，${warningCount} 个提醒` : ''}`, 'success')
  } catch (error) {
    showToast(error.message || '导入预处理失败', 'error')
  } finally {
    if (shouldManageLoading) {
      importPreviewLoading.value = false
    }
  }
}

function createEmptyDraft() {
  return {
    id: '',
    title: '',
    category: 'frontend',
    summary: '',
    authorName: '',
    coverUrl: '',
    contentHtml: '',
    publishedAt: formatDateTimeLocal(new Date()),
    issueLabel: '',
    recommendation: 80,
    track: 'agent',
    difficulty: 'easy',
    heat: 80,
    featured: false,
    vip: false,
    history: false,
    recommended: true,
    today: false,
    highlightsText: '',
    tagsText: '',
    selectedTags: [], // 选中的标签数组
    visualStatus: 'published',
    importHtml: '',
    importSourceType: 'html',
    importSourceUrl: ''
  }
}

function createDraftFromRecord(record) {
  const isInterview = record.type === 'interview'
  return {
    id: record.id || '',
    title: record.title || '',
    category: isInterview ? mapInterviewCategory(record.category) : (record.category || 'frontend'),
    summary: record.summary || '',
    authorName: record.authorName || '',
    coverUrl: record.coverUrl || '',
    contentHtml: record.contentHtml || '',
    publishedAt: normalizeDateTimeLocal(record.publishedAt),
    issueLabel: record.issueLabel || '',
    recommendation: Number(record.recommendation || 80),
    track: isInterview ? (record.track || 'easy') : (record.track || 'agent'),
    difficulty: isInterview ? (record.track || 'easy') : 'easy',
    heat: Number(record.heat || 80),
    featured: Boolean(record.featured),
    vip: Boolean(record.vip),
    history: Boolean(record.history),
    recommended: Boolean(record.recommended),
    today: Boolean(record.today),
    highlightsText: [record.coverKicker, ...(record.highlights || [])].filter(Boolean).join(', '),
    tagsText: (record.tags || []).join(', '),
    selectedTags: record.tags || [], // 从 record.tags 初始化选中的标签
    visualStatus: inferVisualStatus(record),
    importHtml: '',
    importSourceType: 'html',
    importSourceUrl: ''
  }
}

function mapInterviewCategory(category) {
  if (category === '前端' || category === 'frontend') return 'frontend'
  if (category === 'Java' || category === 'java' || category === 'Java 后端' || category === 'Java后端') return 'java'
  if (category === 'Agent 开发' || category === 'Agent开发' || category === 'agent') return 'agent'
  if (category === '大模型原理' || category === 'llm') return 'llm'
  return 'frontend'
}

// 业务目的：在页面完全照模板收口后，保存请求仍要兼容现有后台三类内容接口。
// 业务逻辑：表单只暴露模板级基础字段，其余后台所需字段通过模块默认值和可视状态推导补齐。
function buildSavePayload(form, type) {
  if (type === 'tech') {
    const isFeatured = form.visualStatus === 'published' || form.visualStatus === 'review'
    const isVip = form.visualStatus === 'review'
    return {
      id: form.id || '',
      category: form.category,
      title: form.title,
      summary: form.summary,
      essence: form.summary,
      authorName: form.authorName || 'PeakStars',
      authorRole: '后台内容维护',
      coverUrl: form.coverUrl,
      contentHtml: form.contentHtml,
      publishedAt: normalizeDateTimePayload(form.publishedAt),
      viewCount: 0,
      commentCount: 0,
      likeCount: 0,
      collectCount: 0,
      readTime: '6 min',
      featured: isFeatured,
      vip: isVip,
      collected: false,
      liked: false,
      history: form.visualStatus === 'draft' || form.history,
      highlights: splitCommaText(form.tagsText || form.highlightsText)
    }
  }

  if (type === 'world') {
    const tagList = splitCommaText(form.tagsText || form.highlightsText)
    return {
      id: form.id || '',
      title: form.title,
      issueLabel: form.issueLabel,
      summary: form.summary,
      publishedAt: normalizeDateTimePayload(form.publishedAt),
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      recommendation: form.visualStatus === 'review' ? 85 : Number(form.recommendation || 78),
      coverAccent: '#d33b2d',
      coverKicker: tagList[0] || '专题聚焦',
      coverHeadline: form.title,
      coverSummary: form.summary,
      coverFooter: '专题 / 深读 / 评论',
      coverUrl: form.coverUrl,
      contentHtml: form.contentHtml
    }
  }

  if (type === 'interview') {
    return {
      id: form.id || '',
      title: form.title,
      summary: form.summary,
      authorName: form.authorName || '后台编辑',
      category: form.category || 'frontend',
      track: form.difficulty || 'easy',
      // 优先使用选中的标签（多选模式），如果没有选中则使用文本输入
      tags: (Array.isArray(form.selectedTags) && form.selectedTags.length > 0)
        ? form.selectedTags
        : splitCommaText(form.tagsText),
      publishedAt: normalizeDateTimePayload(form.publishedAt),
      viewCount: 0,
      likeCount: 0,
      collectCount: 0,
      contentHtml: form.contentHtml
    }
  }

  return {
    id: form.id || '',
    title: form.title,
    summary: form.summary,
    authorName: form.authorName || 'PeakStars',
    track: form.track,
    hotspotType: form.track,
    publishedAt: normalizeDateTimePayload(form.publishedAt),
    viewCount: 0,
    commentCount: 0,
    likeCount: 0,
    heat: form.visualStatus === 'review' ? 90 : Number(form.heat || 80),
    recommended: form.visualStatus !== 'draft',
    today: form.visualStatus === 'review' || form.today,
    tags: splitCommaText(form.tagsText),
    coverUrl: form.coverUrl,
    contentHtml: form.contentHtml
  }
}

function buildExportRow(record, type) {
  if (type === 'tech') {
    return {
      id: record.id,
      title: record.title,
      category: record.category,
      summary: record.summary,
      authorName: record.authorName,
      publishedAt: record.publishedAt,
      highlights: (record.highlights || []).join(', '),
      featured: record.featured,
      vip: record.vip,
      coverUrl: record.coverUrl,
      contentHtml: record.contentHtml
    }
  }

  if (type === 'world') {
    return {
      id: record.id,
      title: record.title,
      issueLabel: record.issueLabel,
      summary: record.summary,
      recommendation: record.recommendation,
      publishedAt: record.publishedAt,
      coverKicker: record.coverKicker,
      coverSummary: record.coverSummary,
      coverUrl: record.coverUrl,
      contentHtml: record.contentHtml
    }
  }

  return {
    id: record.id,
    title: record.title,
    track: record.track,
    summary: record.summary,
    authorName: record.authorName,
    heat: record.heat,
    recommended: record.recommended,
    today: record.today,
    tags: (record.tags || []).join(', '),
    publishedAt: record.publishedAt,
    coverUrl: record.coverUrl,
    contentHtml: record.contentHtml
  }
}

function buildImportRecord(row, type) {
  if (type === 'tech') {
    return {
      id: row.id,
      title: row.title,
      category: row.category || 'frontend',
      summary: row.summary,
      essence: row.summary,
      authorName: row.authorName || 'PeakStars',
      authorRole: '后台内容维护',
      publishedAt: row.publishedAt,
      viewCount: Number(row.viewCount || 0),
      commentCount: Number(row.commentCount || 0),
      likeCount: Number(row.likeCount || 0),
      collectCount: Number(row.collectCount || 0),
      readTime: row.readTime || '6 min',
      featured: parseBooleanish(row.featured),
      vip: parseBooleanish(row.vip),
      collected: false,
      liked: false,
      history: parseBooleanish(row.history),
      highlights: splitCommaText(row.highlights),
      coverUrl: row.coverUrl,
      contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
    }
  }

  if (type === 'world') {
    return {
      id: row.id,
      title: row.title,
      issueLabel: row.issueLabel,
      summary: row.summary,
      publishedAt: row.publishedAt,
      viewCount: Number(row.viewCount || 0),
      recommendation: Number(row.recommendation || 0),
      coverAccent: row.coverAccent || '#d33b2d',
      coverKicker: row.coverKicker || '专题聚焦',
      coverHeadline: row.coverHeadline || row.title,
      coverSummary: row.coverSummary || row.summary,
      coverFooter: row.coverFooter || '专题 / 深读 / 评论',
      coverUrl: row.coverUrl,
      contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
    }
  }

  return {
    id: row.id,
    title: row.title,
    track: row.track || 'agent',
    hotspotType: row.hotspotType || row.track || 'agent',
    summary: row.summary,
    authorName: row.authorName || 'PeakStars',
    publishedAt: row.publishedAt,
    viewCount: Number(row.viewCount || 0),
    commentCount: Number(row.commentCount || 0),
    likeCount: Number(row.likeCount || 0),
    heat: Number(row.heat || 0),
    recommended: parseBooleanish(row.recommended),
    today: parseBooleanish(row.today),
    tags: splitCommaText(row.tags),
    coverUrl: row.coverUrl,
    contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
  }
}

function inferVisualStatus(record) {
  if (record.type === 'tech') {
    if (record.vip) {
      return 'review'
    }
    if (record.history) {
      return 'draft'
    }
    return 'published'
  }
  if (record.type === 'world') {
    return Number(record.recommendation || 0) >= 80 ? 'review' : 'published'
  }
  if (record.type === 'interview') {
    return record.track === 'hard' ? 'review' : 'published'
  }
  if (record.today) {
    return 'review'
  }
  if (!record.recommended) {
    return 'draft'
  }
  return 'published'
}

function splitCommaText(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function parseBooleanish(value) {
  return ['true', '1', 'yes', 'y', '是'].includes(String(value || '').trim().toLowerCase())
}

function normalizeDateTimeLocal(value) {
  if (!value) {
    return formatDateTimeLocal(new Date())
  }
  const normalizedValue = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(normalizedValue)) {
    return `${normalizedValue}T00:00`
  }
  return normalizedValue.replace(' ', 'T').slice(0, 16)
}

function normalizeDateTimePayload(value) {
  return String(value || '').replace('T', ' ')
}

function formatDateTimeLocal(date) {
  const currentDate = new Date(date)
  const year = currentDate.getFullYear()
  const month = String(currentDate.getMonth() + 1).padStart(2, '0')
  const day = String(currentDate.getDate()).padStart(2, '0')
  const hour = String(currentDate.getHours()).padStart(2, '0')
  const minute = String(currentDate.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}`
}

function formatDraftTime(rawTime) {
  if (!rawTime) return '刚刚'
  const saved = new Date(rawTime.replace(' ', 'T'))
  const diffMs = Date.now() - saved.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay} 天前`
  return saved.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

/**
 * 统一懒加载 markdown 渲染能力，避免后台首页首屏额外引入大体积解析器。
 * 只在用户选择 markdown 文件时加载 markdown-it，并复用同一个运行时实例。
 */
async function loadMarkdownRuntime() {
  if (!markdownRuntimePromise) {
    markdownRuntimePromise = import('markdown-it').then(({ default: MarkdownIt }) => new MarkdownIt({
      html: true,
      linkify: true,
      breaks: true
    }))
  }
  return markdownRuntimePromise
}

/**
 * 懒加载 docx 解析器，保证后台常规浏览与编辑流程不被文档导入能力拖慢。
 * 仅在用户选择 docx 文件时动态引入 mammoth，并复用缓存结果降低二次导入开销。
 */
async function loadMammothLibrary() {
  if (!mammothRuntimePromise) {
    mammothRuntimePromise = import('mammoth').then((module) => module.default || module)
  }
  return mammothRuntimePromise
}

/**
 * 根据文件后缀选择最合适的解析器，提升不同来源文档的识别准确率。
 * 优先读取最后一个扩展名并统一转成小写，避免用户本地文件大小写差异影响导入。
 */
function resolveFileExtension(fileName) {
  const segments = String(fileName || '').toLowerCase().split('.')
  return segments.length > 1 ? segments.pop() : ''
}

/**
 * 从文件名识别语雀、飞书等来源，便于后端记录导入渠道与后续扩展差异化处理。
 * 先按关键词识别常见编辑器，未命中时回落到文件扩展名来源。
 */
function resolveSourceTypeByFileName(fileName) {
  const normalizedName = String(fileName || '').toLowerCase()
  if (normalizedName.includes('语雀') || normalizedName.includes('yuque')) {
    return 'yuque'
  }
  if (normalizedName.includes('飞书') || normalizedName.includes('feishu') || normalizedName.includes('lark')) {
    return 'feishu'
  }
  if (normalizedName.includes('notion')) {
    return 'notion'
  }
  return resolveFileExtension(fileName) || 'html'
}

/**
 * 从解析后的正文中提取后台标题，减少导入后还要手动补标题的重复动作。
 * 优先读取首个标题文本，再回退到纯文本首行与文件名，保证各种来源都能拿到稳定标题。
 */
function extractTitleFromHtml(html, fallbackName = '') {
  const parser = new DOMParser()
  const document = parser.parseFromString(String(html || ''), 'text/html')
  const heading = document.querySelector('h1,h2,h3')?.textContent?.trim()
  if (heading) {
    return heading.slice(0, 80)
  }
  return extractTitleFromPlainText(document.body?.textContent || '', fallbackName)
}

/**
 * 从 markdown 原文中提取最自然的文章标题，尽量贴近作者在编辑器里的结构。
 * 优先识别一级标题，未命中时退回纯文本首行与文件名兜底。
 */
function extractTitleFromMarkdown(markdownText, fallbackName = '') {
  const lines = String(markdownText || '').split(/\r?\n/)
  const headingLine = lines.find((line) => /^#\s+/.test(line.trim()))
  if (headingLine) {
    return headingLine.replace(/^#\s+/, '').trim().slice(0, 80)
  }
  return extractTitleFromPlainText(markdownText, fallbackName)
}

/**
 * 为纯文本导入提供稳定标题兜底，避免空标题阻断后台发布。
 * 优先取首个非空行，若正文为空则退回文件名，最终保证返回可展示标题。
 */
function extractTitleFromPlainText(text, fallbackName = '') {
  const firstLine = String(text || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find(Boolean)
  return (firstLine || stripFileExtension(fallbackName) || '未命名导入内容').slice(0, 80)
}

/**
 * 根据正文纯文本快速生成后台摘要，减少导入后手动整理摘要的运营动作。
 * 统一压缩空白字符后截取前 120 个字，保证卡片摘要简洁且可读。
 */
function buildSummaryFromPlainText(text) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 120)
}

/**
 * 统一整理 markdown 原文里的空行、伪列表和内联 HTML，减少导入后出现的空白段落与未渲染标签。
 * 在进入 markdown-it 前先压平连续空行、清除空列表项，并把常见 font 标签退化成纯文本强调语义。
 */
function normalizeMarkdownSource(markdownText) {
  return String(markdownText || '')
    .replace(/\r\n/g, '\n')
    // 全面剥离所有零宽字符、不可见格式化字符、软连字符
    .replace(/[\u200B-\u200F\u2028-\u202F\u2060-\u206F\uFEFF\u00AD]/g, '')
    .replace(/\u00a0/g, ' ')
    .replace(/\u3000/g, ' ')
    .replace(/<font\b[^>]*>(.*?)<\/font>/gi, '$1')
    // 删除只剩空白字符的空行（之前的不可见字符被剥离后可能变成空白行）
    .replace(/^\s+$/gm, '')
    // 删除空的无序列表项：仅有 - * + 的行
    .replace(/^\s*[-*+]\s*$/gm, '')
    // 删除空的有序列表项：仅有数字编号的行
    .replace(/^\s*\d+[.:：]?\s*$/gm, '')
    // 删除空编号行后面紧跟空行或下一个编号的情况
    .replace(/^\s*\d+[.:：]?\s*\n(?=\s*\n|\s*\d+[.:：]?\s|$)/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

/**
 * 在不破坏嵌套结构的前提下重建有序列表编号。
 * normalizer 可能删除了部分编号行（如空列表项），导致剩余编号不连续，
 * 此函数重新按出现顺序从 1 开始编号，确保 markdown-it 能正确渲染有序列表。
 */
function rebuildMarkdownOrderedList(markdownText) {
  let listCounters = {}
  let indentStack = []
  return String(markdownText || '')
    .split('\n')
    .map((line) => {
      const trimmed = line.trim()
      // 匹配有序列表项：数字 + 英文句号/中文句号/冒号 + 至少一个空白字符 + 剩余内容
      const match = trimmed.match(/^(\d+)[.:：]\s+(.*)$/)
      if (!match) {
        // 非列表行重置列表状态（空行或缩进归零）
        if (trimmed === '' || (!trimmed.startsWith(' ') && !trimmed.startsWith('\t'))) {
          listCounters = {}
          indentStack = []
        }
        return line
      }

      // 剥离不可见字符后判断内容是否为空，避免被 NBSP / 零宽空格等欺骗
      const rawContent = match[2] || ''
      const cleanContent = rawContent
        .replace(/[\u200B-\u200D\uFEFF]/g, '')
        .replace(/\u00a0/g, ' ')
        .replace(/\u3000/g, ' ')
        .trim()

      if (!cleanContent) {
        // 空内容的列表项：重置计数器并保留原行（后续 markdown-it 会跳过）
        listCounters = {}
        indentStack = []
        return ''
      }

      const indent = line.length - line.trimStart().length

      // 找到当前缩进对应的层级
      let level = indentStack.indexOf(indent)
      if (level < 0) {
        // 新层级：栈顶加进去
        indentStack.push(indent)
        level = indentStack.length - 1
        listCounters[level] = 0
      } else {
        // 缩减缩进：弹出多余的栈
        while (indentStack.length > level + 1) {
          indentStack.pop()
          delete listCounters[indentStack.length]
        }
      }

      listCounters[level] = (listCounters[level] || 0) + 1
      return ' '.repeat(indent) + listCounters[level] + '. ' + cleanContent
    })
    .join('\n')
}

/**
 * 把普通文本转换成分段 HTML，保留原文段落结构。
 * 按空行切段并进行最小转义，再输出标准段落标签供服务端继续清洗。
 */
function textToParagraphHtml(text) {
  const paragraphs = String(text || '')
    .split(/\r?\n\s*\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
  return paragraphs
    .map((item) => `<p>${escapeHtml(item).replace(/\r?\n/g, '<br />')}</p>`)
    .join('')
}

/**
 * 为标题提取与摘要生成提供统一纯文本视图，避免重复手写 HTML 清洗逻辑。
 * 通过 DOMParser 读取正文文本，再压缩多余空白字符形成稳定输出。
 */
function htmlToPlainText(html) {
  const parser = new DOMParser()
  const document = parser.parseFromString(String(html || ''), 'text/html')
  return String(document.body?.textContent || '')
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * 统一抹平导入内容里的零宽字符、NBSP 与全角空格，避免看起来空白的编号段落逃过清洗。
 * 先把不可见字符标准化成普通空格，再输出稳定文本供空段落与伪编号判断复用。
 */
function normalizeImportText(text) {
  return String(text || '')
    .replace(/[\u200B-\u200D\uFEFF]/g, '')
    .replace(/\u00a0/g, ' ')
    .replace(/\u3000/g, ' ')
    .trim()
}

/**
 * 判断当前段落是否只剩编号占位，专门清理 1. 2. 3. 这类伪列表空行。
 * 标准化文本后只按纯数字加标点的最小模式识别，避免误删正常正文内容。
 */
function isEmptyNumberMarker(text) {
  return /^\d+[.:：]?$/.test(normalizeImportText(text))
}

/**
 * 统一清理导入 HTML 里的空段落、空列表项和多余换行，保证后台编辑器打开后的版式更整洁。
 * 通过多轮 DOM 级规则删除无内容节点，并把连续空白折叠到最小展示范围，减少截图中的大块留白问题。
 */
function normalizeImportedHtml(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(String(html || ''), 'text/html')
  const body = doc.body

  // 第一步：展开 font 标签，避免内部内容被遮挡
  body.querySelectorAll('font').forEach((node) => {
    node.replaceWith(...Array.from(node.childNodes))
  })

  // 第二步：先把所有不可见字符从 HTML 字符串层面剥离干净
  // 必须在 DOM 操作之前做，否则不可见字符会干扰 textContent 判空
  body.innerHTML = body.innerHTML
    .replace(/[\u200B-\u200F\u2028-\u202F\u2060-\u206F\uFEFF\u00AD]/g, '')
    .replace(/\u00a0/g, ' ')
    .replace(/\u3000/g, ' ')

  // 第三步：多轮 DOM 清理，直到没有空节点可删为止
  // 删除一个空节点可能让父节点也变空，所以需要循环
  let changed = true
  while (changed) {
    changed = false

    // 判断一个元素是否为"空"：没有可见文字、没有媒体内容、只有纯编号
    const isEmpty = (node) => {
      const text = (node.textContent || '').replace(/\s/g, '')
      if (text && !/^\d+[.:：]+$/.test(text)) return false
      const media = node.querySelectorAll('img, table, pre, code, iframe, video, audio')
      return media.length === 0
    }

    // 3a. 删除空的 p / div / span / blockquote（但不删 li 内部的 p，交给 3b 统一处理）
    for (const node of body.querySelectorAll('p, div, span, blockquote')) {
      // 跳过 li 内部的 p，避免先删了 p 再把 li 误判为空
      if (node.parentElement && node.parentElement.tagName === 'LI' && node.tagName === 'P') {
        continue
      }
      if (isEmpty(node)) {
        node.remove()
        changed = true
      }
    }

    // 3b. 删除空的 li：计算 li 自身文字 = 全部文字 - 嵌套列表文字
    for (const li of body.querySelectorAll('li')) {
      const allText = (li.textContent || '').replace(/\s/g, '')
      const nestedText = Array.from(li.querySelectorAll('ul, ol'))
        .map((list) => (list.textContent || '').replace(/\s/g, ''))
        .join('')
      const ownText = nestedText ? allText.replace(nestedText, '') : allText

      const hasMedia = li.querySelectorAll('img, table, pre, code, iframe').length > 0
      const nestedListsHaveContent = Array.from(li.children)
        .filter((child) => child.tagName === 'UL' || child.tagName === 'OL')
        .some((list) => list.querySelector('li'))
      const isNumberOnly = /^\d+[.:：]+$/.test(ownText)

      if ((!ownText || isNumberOnly) && !hasMedia && !nestedListsHaveContent) {
        li.remove()
        changed = true
      }
    }

    // 3c. 删除所有 li 都被清空的 ul/ol
    for (const list of body.querySelectorAll('ul, ol')) {
      if (!list.querySelector('li')) {
        list.remove()
        changed = true
      }
    }
  }

  // 第四步：字符串级别最终清理
  body.innerHTML = body.innerHTML
    .replace(/<div[^>]*>\s*<\/div>/gi, '')
    .replace(/<p[^>]*>(\s|&nbsp;)*<\/p>/gi, '')
    .replace(/<span[^>]*>\s*<\/span>/gi, '')
    .replace(/<li[^>]*>(\s|&nbsp;|<br\s*\/?>)*<\/li>/gi, '')
    .replace(/<(ul|ol)[^>]*>(\s|&nbsp;|<br\s*\/?>)*<\/(ul|ol)>/gi, '')
    .replace(/(?:<br\s*\/?>\s*){3,}/gi, '<br><br>')
    .trim()

  return body.innerHTML
}

/**
 * 移除文件扩展名用于标题兜底，避免后台标题直接携带 docx 或 md 后缀。
 * 只删除最后一个扩展名片段，兼容带点号的复杂文件名。
 */
function stripFileExtension(fileName) {
  return String(fileName || '').replace(/\.[^.]+$/, '')
}

/**
 * 在前端兜底处理纯文本转 HTML 时的特殊字符，避免正文被浏览器误解析。
 * 对 HTML 关键字符做最小必要转义，再把换行保留为可读段落结构。
 */
function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

async function loadXlsxLibrary() {
  if (!xlsxLibraryPromise) {
    xlsxLibraryPromise = import('xlsx')
  }
  return xlsxLibraryPromise
}
</script>

<style scoped src="../styles/AdminDashboard.css"></style>
