#!/usr/bin/env python3
"""
快速构建脚本 - 使用BuildKit和缓存优化
"""

import os
import subprocess
import sys

def run_command(command, env=None):
    """执行命令"""
    print(f"执行: {command}")
    try:
        result = subprocess.run(command, shell=True, env=env)
        return result.returncode == 0
    except Exception as e:
        print(f"命令执行失败: {e}")
        return False

def main():
    print("🚀 快速构建 GraalVM 脚本执行服务")
    print("=" * 50)
    
    # 设置BuildKit环境变量
    env = os.environ.copy()
    env['DOCKER_BUILDKIT'] = '1'
    
    # 构建命令选项
    commands = {
        '1': {
            'name': '标准构建',
            'cmd': 'docker build --progress=plain -t script-server:latest .'
        },
        '2': {
            'name': '带缓存构建',
            'cmd': 'docker build --progress=plain --build-arg BUILDKIT_INLINE_CACHE=1 -t script-server:latest .'
        },
        '3': {
            'name': 'Buildx构建（推荐）',
            'cmd': 'docker buildx build --progress=plain --load -t script-server:latest .'
        },
        '4': {
            'name': '清除缓存重新构建',
            'cmd': 'docker build --progress=plain --no-cache -t script-server:latest .'
        }
    }
    
    print("选择构建方式:")
    for key, value in commands.items():
        print(f"  {key}. {value['name']}")
    
    choice = input("\n请选择 (1-4, 默认2): ").strip()
    if not choice:
        choice = '2'
    
    if choice not in commands:
        print("无效选择")
        sys.exit(1)
    
    selected = commands[choice]
    print(f"\n🔨 开始{selected['name']}...")
    
    # 执行构建
    if run_command(selected['cmd'], env=env):
        print("\n✅ 构建成功!")
        print("\n🏃 运行容器:")
        print("  docker run -p 8080:8080 script-server:latest")
        print("\n🧪 完整测试:")
        print("  python test.py")
    else:
        print("\n❌ 构建失败!")
        sys.exit(1)

if __name__ == "__main__":
    main()
