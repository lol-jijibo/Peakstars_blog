/**
 * 测试用 Token 生成器
 * 用于模拟用户登录进行功能和安全测试
 *
 * 用法:
 *   node scripts/generate-test-token.js <userId> <email> <username> [expireHours] [secret]
 *   node scripts/generate-test-token.js 1 admin@test.com admin 720
 *   node scripts/generate-test-token.js 2 user@test.com testuser 168
 */

const crypto = require('crypto');

function base64UrlEncode(str) {
  return Buffer.from(str).toString('base64url');
}

function sign(content, secret) {
  const hmac = crypto.createHmac('sha256', secret);
  hmac.update(content);
  return hmac.digest('base64url');
}

function generateToken(userId, email, username, expireHours, secret) {
  const now = Date.now();
  const exp = now + expireHours * 3600 * 1000;

  const payload = JSON.stringify({
    userId: Number(userId),
    email,
    username,
    exp,
  });

  const encodedPayload = base64UrlEncode(payload);
  const signature = sign(encodedPayload, secret);

  return `${encodedPayload}.${signature}`;
}

// --- main ---
const args = process.argv.slice(2);

if (args.length < 3) {
  console.log('用法: node scripts/generate-test-token.js <userId> <email> <username> [expireHours] [secret]');
  console.log('');
  console.log('示例:');
  console.log('  node scripts/generate-test-token.js 1 admin@test.com admin');
  console.log('  node scripts/generate-test-token.js 2 user@test.com testuser 168');
  console.log('');
  console.log('默认 secret 使用开发环境密钥 (dev-secret-key-for-local-development-only)');
  process.exit(1);
}

const [userId, email, username, expireHours = '720', secret = 'dev-secret-key-for-local-development-only'] = args;

const token = generateToken(userId, email, username, parseInt(expireHours), secret);

console.log('');
console.log('=== 生成的测试 Token ===');
console.log('');
console.log(token);
console.log('');
console.log('=== 使用方法 ===');
console.log('');
console.log('方法 1 — 浏览器 DevTools Console:');
console.log(`  localStorage.setItem('interview_demo_access_token', '${token}');`);
console.log(`  localStorage.setItem('interview_demo_current_user', JSON.stringify({ id: ${userId}, username: '${username}', email: '${email}', role: '${username === 'admin' ? 'admin' : 'user'}', joinedAt: '2025-01-01T00:00:00' }));`);
console.log('  location.reload();');
console.log('');
console.log('方法 2 — 作为 Authorization header (curl/Postman):');
console.log(`  Authorization: Bearer ${token}`);
console.log('');
