#!/usr/bin/env bash
# 将 public/ 下的图片迁移到 MinIO，并更新数据库中对应文章的封面 URL。
# 前置条件：MinIO 已启动，mc 客户端已配置好 localminio alias。
set -euo pipefail

MC="d:/minio/mc.exe"
BUCKET="peakstars-content"
PUBLIC_DIR="d:/Frontend-stars/Peakstars_blog/public"
MYSQL_USER="root"
MYSQL_DB="interview_db"
TODAY=$(date +%Y/%m/%d)

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "============================================"
echo "  图片迁移: public/ → MinIO ($BUCKET)"
echo "============================================"

# 确保 mc alias 已配置
$MC alias set localminio http://localhost:9000 minioadmin minioadmin 2>/dev/null || true

# 文件名 → DB article ID 映射（基于当前数据库数据）
# 注意：多个 article 可能引用同一图片
declare -A FILE_MAP
FILE_MAP["ChatGPT Image 2026年4月23日 18_37_46.png"]="1"
FILE_MAP["【哲风壁纸】公路-后视镜-城镇.png"]="2,7"
FILE_MAP["【哲风壁纸】女孩-户外-旷野.png"]="3,8"
FILE_MAP["【哲风壁纸】夏日-晴天-氛围感.png"]="4"
FILE_MAP["【哲风壁纸】侧脸-树木-欧阳娜娜.png"]="5"
FILE_MAP["【哲风壁纸】xiaomiyu7-小米suv.png"]="6"
FILE_MAP["peakstars-blog-icon.jpg"]="26"

uploaded=0
skipped=0

for filepath in "$PUBLIC_DIR"/*.png "$PUBLIC_DIR"/*.jpg; do
  [ -f "$filepath" ] || continue

  filename=$(basename "$filepath")
  extension="${filename##*.}"
  uuid=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "$(date +%s)$((RANDOM % 10000))")
  uuid_clean=$(echo "$uuid" | tr -d '-' | head -c 32)

  # 为没有映射的文章也上传，但不更新数据库
  article_ids="${FILE_MAP[$filename]:-}"

  # MinIO 对象路径: cover/YYYY/MM/DD/{uuid}.{ext}
  object_path="cover/${TODAY}/${uuid_clean}.${extension}"
  # 代理 URL: /uploads/{bucket}/cover/YYYY/MM/DD/{uuid}.{ext}
  proxy_url="/uploads/${BUCKET}/cover/${TODAY}/${uuid_clean}.${extension}"

  echo ""
  echo -e "${YELLOW}上传:${NC} $filename → $object_path"

  if $MC cp "$filepath" "localminio/${BUCKET}/${object_path}" 2>&1; then
    echo -e "${GREEN}  上传成功${NC}"

    if [ -n "$article_ids" ]; then
      IFS=',' read -ra ids <<< "$article_ids"
      for article_id in "${ids[@]}"; do
        mysql -u "$MYSQL_USER" -p"${DB_PASSWORD:-}" "$MYSQL_DB" \
          -e "UPDATE tech_article SET cover_url = '$proxy_url' WHERE id = $article_id;" 2>&1
        echo "  数据库: article $article_id cover_url → $proxy_url"
      done
    else
      echo "  (无对应文章，仅上传到 MinIO)"
    fi
    uploaded=$((uploaded + 1))
  else
    echo "  上传失败！"
    skipped=$((skipped + 1))
  fi
done

echo ""
echo "============================================"
echo "  迁移完成: 上传 $uploaded 张, 跳过 $skipped 张"
echo "============================================"

# 同时修复已有的 MinIO 直连 URL，改为代理 URL
echo ""
echo -e "${YELLOW}修复 MinIO 直连 URL → 代理 URL...${NC}"

mysql -u "$MYSQL_USER" -p"${DB_PASSWORD:-}" "$MYSQL_DB" <<'SQL'
-- 封面 URL：将 http://localhost:9000/peakstars-content/... 改为 /uploads/peakstars-content/...
UPDATE tech_article
SET cover_url = REPLACE(cover_url, 'http://localhost:9000/peakstars-content/', '/uploads/peakstars-content/')
WHERE cover_url LIKE 'http://localhost:9000/peakstars-content/%';

-- 正文 HTML：将图片 URL 改为代理路径
UPDATE tech_article
SET content_html = REPLACE(content_html, 'http://localhost:9000/peakstars-content/', '/uploads/peakstars-content/')
WHERE content_html LIKE '%http://localhost:9000/peakstars-content/%';
SQL

echo "数据库 URL 修复完成"

# 显示迁移后的结果
echo ""
echo "============================================"
echo "  迁移后所有封面 URL 统计:"
echo "============================================"
mysql -u "$MYSQL_USER" -p"${DB_PASSWORD:-}" "$MYSQL_DB" \
  -e "SELECT
    COUNT(CASE WHEN cover_url LIKE '/uploads/%' THEN 1 END) AS proxy_urls,
    COUNT(CASE WHEN cover_url LIKE 'http://localhost:9000%' THEN 1 END) AS minio_direct,
    COUNT(CASE WHEN cover_url NOT LIKE '/uploads/%' AND cover_url NOT LIKE 'http%' THEN 1 END) AS local_paths
  FROM tech_article;" 2>&1
