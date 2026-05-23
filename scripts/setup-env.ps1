# ============================================================
# PeakStars Blog - 本地环境变量一键配置脚本
# 将代码中暴露的密钥写入 Windows 用户级环境变量
# 运行方式: 右键此文件 -> "使用 PowerShell 运行"
#          或在 PowerShell 中执行: .\scripts\setup-env.ps1
# ============================================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  PeakStars Blog - 环境变量配置工具" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$vars = @{
    # ---- 数据库 ----
    "DB_USERNAME"       = "root"
    "DB_PASSWORD"       = "lixuF123456"

    # ---- JWT 认证 ----
    "AUTH_TOKEN_SECRET" = "interview-demo-auth-secret"

    # ---- 邮箱 (QQ SMTP) ----
    "MAIL_USERNAME"     = "1843599766@qq.com"
    "MAIL_PASSWORD"     = ""    # <-- QQ 邮箱授权码，需要你自己填写

    # ---- MinIO (本地对象存储) ----
    "MINIO_ACCESS_KEY"  = "minioadmin"
    "MINIO_SECRET_KEY"  = "minioadmin"
}

Write-Host "以下环境变量将被写入当前用户:"
Write-Host ""
foreach ($key in $vars.Keys) {
    $display = if ($vars[$key] -eq "") { "(空，请手动填写)" } else { $vars[$key] }
    Write-Host "  $key = $display"
}
Write-Host ""

$confirm = Read-Host "确认写入? (y/n)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "已取消。" -ForegroundColor Yellow
    Read-Host "按任意键退出"
    exit
}

foreach ($key in $vars.Keys) {
    [Environment]::SetEnvironmentVariable($key, $vars[$key], "User")
}

Write-Host ""
Write-Host "已写入用户环境变量。" -ForegroundColor Green
Write-Host ""
Write-Host "注意:" -ForegroundColor Yellow
Write-Host "  1. 变量已写入 Windows 用户级环境变量，重启终端 / IDE 后生效。" -ForegroundColor Yellow
Write-Host "  2. MAIL_PASSWORD 为空，请手动设置或编辑此脚本重新运行。" -ForegroundColor Yellow
Write-Host "  3. 生产环境请生成全新的 AUTH_TOKEN_SECRET。" -ForegroundColor Yellow
Write-Host "     (可用 openssl rand -hex 32 生成)" -ForegroundColor Yellow

Read-Host "按任意键退出"
