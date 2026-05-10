export const interviewFirstLevelCategories = [
  { key: 'all', label: '全部', subCategories: [] },
  {
    key: 'backend',
    label: 'Java 后端',
    subCategories: [
      { key: 'all', label: '全部', category: 'all' },
      { key: 'java', label: 'Java 基础', category: 'java' },
      { key: 'collection', label: '集合框架', category: 'java' },
      { key: 'jvm', label: 'JVM', category: 'java' },
      { key: 'concurrent', label: '并发编程', category: 'java' },
      { key: 'springboot', label: 'Spring Boot', category: 'java' },
      { key: 'mybatis', label: 'MyBatis', category: 'java' },
      { key: 'mysql', label: 'MySQL', category: 'java' },
      { key: 'redis', label: 'Redis', category: 'java' },
      { key: 'system-design', label: 'Elasticsearch', category: 'java' }
    ]
  },
  {
    key: 'frontend',
    label: '前端',
    subCategories: [
      { key: 'all', label: '全部', category: 'frontend' },
      { key: 'html', label: 'HTML/CSS', category: 'frontend' },
      { key: 'javascript', label: 'JavaScript', category: 'frontend' },
      { key: 'typescript', label: 'TypeScript', category: 'frontend' },
      { key: 'vue', label: 'Vue', category: 'frontend' },
      { key: 'react', label: 'React', category: 'frontend' }
    ]
  },
  { key: 'agent', label: 'Agent 开发', subCategories: [] },
  { key: 'llm', label: '大模型原理', subCategories: [] },
  { key: 'algorithm', label: '算法', subCategories: [] }
]

export const interviewFirstLevelCategoryApiMap = {
  backend: 'java',
  frontend: 'frontend'
}

const adminInterviewCategoryKeyMap = {
  frontend: 'frontend',
  java: 'backend',
  agent: 'agent',
  llm: 'llm'
}

/**
 * 目的：统一返回后台面经表单可选标签，确保与用户端面经页分类配置保持一致。
 * 逻辑：先把后台分类编码映射到前台一级分类，再过滤“全部”等占位项，仅保留真实子标签名称。
 */
export function getInterviewTagOptions(categoryCode) {
  const firstLevelKey = adminInterviewCategoryKeyMap[categoryCode] || categoryCode
  const matchedCategory = interviewFirstLevelCategories.find((item) => item.key === firstLevelKey)
  if (!matchedCategory || !Array.isArray(matchedCategory.subCategories)) {
    return []
  }
  return matchedCategory.subCategories
    .filter((item) => item.key !== 'all')
    .map((item) => item.label)
}
