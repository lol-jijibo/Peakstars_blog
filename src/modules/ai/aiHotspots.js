export const aiTracks = [
  {
    key: 'all',
    label: '全部热点',
    description: '按时间流查看近期 AI 热点，适合快速扫读和建立整体感知。',
    count: '6',
    signals: ['先看热点密度，再看是否有稳定落地路径。', '优先关注影响开发流程的变化，而不是口号。']
  },
  {
    key: 'agent',
    label: 'Agent 落地',
    description: '关注任务拆解、工具调用、结果验证等链路是否开始工程化。',
    count: '2',
    signals: ['重点看任务稳定完成率。', '看是否形成可复用工作流，而不是单次演示。']
  },
  {
    key: 'multimodal',
    label: '多模态交互',
    description: '关注文本、图像、语音接口变化对产品交互和内容生成的影响。',
    count: '2',
    signals: ['先看接口变化，再看交互成本。', '关注延迟、格式统一和链路复杂度。']
  },
  {
    key: 'infra',
    label: '模型基础设施',
    description: '关注推理成本、模型网关、评测链路和平台侧工程能力。',
    count: '2',
    signals: ['成本曲线会直接影响场景边界。', '平台化能力决定团队是否能长期使用。']
  }
]

export const aiHotspots = [
  {
    id: 'ai-hotspot-agent-workflow',
    track: 'agent',
    hotspotType: 'agent',
    title: 'Agent 工作流从演示阶段转向稳定执行',
    summary: '团队开始把规划、调用工具、结果回收和失败重试收进一条闭环链路，热点已经从“能不能做”切到“能不能稳定交付”。',
    publishedAt: '2026-04-26 09:20',
    tags: ['任务编排', '工具调用', '结果校验'],
    viewCount: 4188,
    commentCount: 56,
    likeCount: 701,
    heat: 92,
    isToday: true
  },
  {
    id: 'ai-hotspot-multimodal-interface',
    track: 'multimodal',
    hotspotType: 'multimodal',
    title: '多模态能力更新开始重写应用层交互设计',
    summary: '新一轮变化不只在生成质量，更多落在输入输出格式统一、响应时延和多终端交互方式的变化上。',
    publishedAt: '2026-04-26 10:10',
    tags: ['图像输入', '语音链路', '前端交互'],
    viewCount: 3924,
    commentCount: 49,
    likeCount: 648,
    heat: 88,
    isToday: true
  },
  {
    id: 'ai-hotspot-model-gateway',
    track: 'infra',
    hotspotType: 'infra',
    title: '模型网关与统一鉴权成为团队接入默认层',
    summary: '越来越多团队先做模型网关，再做业务接入。核心不是抽象得多漂亮，而是把配额、日志和回退策略收成统一入口。',
    publishedAt: '2026-04-25 18:40',
    tags: ['网关', '鉴权', '日志追踪'],
    viewCount: 3016,
    commentCount: 31,
    likeCount: 512,
    heat: 81,
    isToday: false
  },
  {
    id: 'ai-hotspot-agent-eval',
    track: 'agent',
    hotspotType: 'agent',
    title: 'Agent 评测从问答正确率转向任务完成率',
    summary: '单轮回答质量不再足够，真正影响采用的是任务是否能在限制条件内跑完，以及异常是否可恢复。',
    publishedAt: '2026-04-25 14:15',
    tags: ['评测体系', '完成率', '异常恢复'],
    viewCount: 2874,
    commentCount: 28,
    likeCount: 476,
    heat: 79,
    isToday: false
  },
  {
    id: 'ai-hotspot-multimodal-production',
    track: 'multimodal',
    hotspotType: 'multimodal',
    title: '多模态生产链路开始替代部分传统内容流水线',
    summary: '在营销素材、帮助中心和商品内容等场景，多模态链路已经开始取代过去分散的人工作业和脚本拼接。',
    publishedAt: '2026-04-24 20:30',
    tags: ['内容生产', '工作流替换', '成本优化'],
    viewCount: 2548,
    commentCount: 26,
    likeCount: 428,
    heat: 74,
    isToday: false
  },
  {
    id: 'ai-hotspot-infra-cost',
    track: 'infra',
    hotspotType: 'infra',
    title: '推理成本下降正在重新打开边缘场景预算',
    summary: '一旦单次调用成本和延迟继续下降，很多之前不成立的高频调用场景会重新进入产品设计范围。',
    publishedAt: '2026-04-24 16:00',
    tags: ['推理成本', '高频调用', '场景边界'],
    viewCount: 2320,
    commentCount: 19,
    likeCount: 395,
    heat: 71,
    isToday: false
  }
]
