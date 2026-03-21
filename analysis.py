import subprocess
import re
from pathlib import Path

# 1. 从数据库导出所有权限
db_permissions = set()
result = subprocess.run(
    'docker exec mysql8 mysql -uroot -p6678722mm lead_declaration -e "SELECT permission FROM sys_menu WHERE deleted = 0 AND permission IS NOT NULL AND permission != \\'\\' ORDER BY permission;"',
    shell=True,
    capture_output=True,
    text=True
)
for line in result.stdout.split('\\n'):
    line = line.strip()
    if line and line != 'permission':
        db_permissions.add(line)

# 2. 从后端代码中提取权限
backend_permissions = set()
pattern = r'@RequiresPermissions\s*\(\s*"^([^"]+)"'
backend_files = Path('backend/src/main/java/com/declaration/controller').glob('*.java')
for file in backend_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
        matches = re.findall(pattern, content)
        backend_permissions.update(matches)

# 3. 从前端代码中提取权限
frontend_permissions = set()
pattern_vue = r"v-permission\s*=\s*\[\s*'(?P^<perm^>[^']+^)'"
frontend_files = Path('frontend/src/views').glob('**/*.vue')
for file in frontend_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
        matches = re.findall(pattern_vue, content)
        frontend_permissions.update(matches)

print('=== 数据库中的权限 ==='')
print('总数:', len(db_permissions))
for p in sorted(db_permissions): print('  ', p)

print(
print('=== 后端使用的权限 ==='')
print('总数:', len(backend_permissions))
for p in sorted(backend_permissions): print('  ', p)
