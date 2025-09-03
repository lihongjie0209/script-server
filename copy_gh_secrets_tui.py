#!/usr/bin/env python3
"""
GitHub CLI 密钥和环境变量复制工具 (TUI版本)

功能：
- 从其他GitHub项目复制secrets和variables
- 支持项目搜索和选择
- 交互式TUI界面，支持上下键选择
- 复制前确认和修改
- 批量操作支持

使用前提：
- 安装GitHub CLI (gh)
- 已登录GitHub账户：gh auth login
"""

import subprocess
import json
import sys
import re
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass
import inquirer
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.text import Text
from rich import print as rprint

console = Console()


@dataclass
class Repository:
    """仓库信息"""
    name: str
    full_name: str
    description: str
    private: bool


@dataclass
class Secret:
    """密钥信息"""
    name: str
    updated_at: str


@dataclass
class Variable:
    """环境变量信息"""
    name: str
    value: str
    updated_at: str


class GitHubSecretsManager:
    """GitHub密钥和环境变量管理器"""
    
    def __init__(self):
        self.check_gh_cli()
    
    def check_gh_cli(self):
        """检查GitHub CLI是否安装和登录"""
        try:
            # 检查gh cli是否安装
            result = subprocess.run(['gh', '--version'], capture_output=True, text=True, check=True,
                                  encoding='utf-8', errors='ignore')
            version = result.stdout.split('\n')[0]
            console.print(f"✓ GitHub CLI 已安装: {version.split()[-1]}", style="green")
            
            # 检查是否已登录
            result = subprocess.run(['gh', 'auth', 'status'], capture_output=True, text=True, check=True,
                                  encoding='utf-8', errors='ignore')
            console.print("✓ GitHub CLI 已登录", style="green")
            
        except subprocess.CalledProcessError as e:
            if 'gh auth status' in str(e.cmd):
                console.print("❌ 请先登录GitHub CLI: gh auth login", style="red")
                sys.exit(1)
            else:
                console.print("❌ 请先安装GitHub CLI: https://cli.github.com", style="red")
                sys.exit(1)
        except FileNotFoundError:
            console.print("❌ 请先安装GitHub CLI: https://cli.github.com", style="red")
            sys.exit(1)

    def search_repositories(self, query: str = "", limit: int = 20) -> List[Repository]:
        """搜索用户自己的仓库"""
        try:
            # 首先获取当前用户信息
            user_result = subprocess.run(['gh', 'api', 'user'], capture_output=True, text=True, check=True,
                                       encoding='utf-8', errors='ignore')
            user_data = json.loads(user_result.stdout)
            username = user_data['login']
            
            # 列出用户自己的仓库
            cmd = ['gh', 'repo', 'list', username, '--limit', str(limit), '--json', 
                   'name,nameWithOwner,description,isPrivate']
            
            result = subprocess.run(cmd, capture_output=True, text=True, check=True,
                                  encoding='utf-8', errors='ignore')
            repos_data = json.loads(result.stdout)
            
            repositories = []
            for repo in repos_data:
                # 如果有查询条件，进行过滤
                if query:
                    if (query.lower() not in repo['name'].lower() and 
                        query.lower() not in repo.get('description', '').lower()):
                        continue
                
                repositories.append(Repository(
                    name=repo['name'],
                    full_name=repo['nameWithOwner'],
                    description=repo.get('description', ''),
                    private=repo['isPrivate']
                ))
            
            return repositories
        except subprocess.CalledProcessError as e:
            console.print(f"❌ 搜索仓库失败: {e.stderr}", style="red")
            return []
        except json.JSONDecodeError:
            console.print("❌ 解析仓库数据失败", style="red")
            return []
    
    def get_secrets(self, repo_full_name: str) -> List[Secret]:
        """获取仓库的secrets"""
        try:
            cmd = ['gh', 'secret', 'list', '--repo', repo_full_name, '--json', 
                   'name,updatedAt']
            result = subprocess.run(cmd, capture_output=True, text=True, check=True,
                                  encoding='utf-8', errors='ignore')
            secrets_data = json.loads(result.stdout)
            
            secrets = []
            for secret in secrets_data:
                secrets.append(Secret(
                    name=secret['name'],
                    updated_at=secret['updatedAt']
                ))
            
            return secrets
        except subprocess.CalledProcessError as e:
            console.print(f"❌ 获取secrets失败: {e.stderr}", style="red")
            return []
        except json.JSONDecodeError:
            console.print("❌ 解析secrets数据失败", style="red")
            return []
    
    def get_variables(self, repo_full_name: str) -> List[Variable]:
        """获取仓库的variables"""
        try:
            cmd = ['gh', 'variable', 'list', '--repo', repo_full_name, '--json', 
                   'name,value,updatedAt']
            result = subprocess.run(cmd, capture_output=True, text=True, check=True,
                                  encoding='utf-8', errors='ignore')
            variables_data = json.loads(result.stdout)
            
            variables = []
            for var in variables_data:
                variables.append(Variable(
                    name=var['name'],
                    value=var['value'],
                    updated_at=var['updatedAt']
                ))
            
            return variables
        except subprocess.CalledProcessError as e:
            console.print(f"❌ 获取variables失败: {e.stderr}", style="red")
            return []
        except json.JSONDecodeError:
            console.print("❌ 解析variables数据失败", style="red")
            return []


def select_repository_tui(repositories: List[Repository]) -> Optional[Repository]:
    """使用TUI选择仓库"""
    if not repositories:
        return None
    
    choices = []
    for repo in repositories:
        visibility = "🔒 私有" if repo.private else "🌐 公开"
        desc = repo.description[:50] + "..." if len(repo.description) > 50 else repo.description
        choice_text = f"{repo.name} ({visibility}) - {desc}"
        choices.append((choice_text, repo))
    
    questions = [
        inquirer.List('repo',
                     message="请选择源仓库",
                     choices=choices,
                     carousel=True)
    ]
    
    try:
        answer = inquirer.prompt(questions)
        if answer:
            return answer['repo']
    except KeyboardInterrupt:
        return None
    
    return None


def copy_secrets_tui(secrets: List[Secret], target_repo: str, manager: GitHubSecretsManager):
    """使用TUI交互式复制secrets"""
    if not secrets:
        console.print("📝 没有secrets需要复制", style="yellow")
        return
    
    console.print(f"\n🔐 开始处理 {len(secrets)} 个Secrets", style="bold blue")
    
    for i, secret in enumerate(secrets, 1):
        console.print(f"\n[{i}/{len(secrets)}] 处理 Secret: [bold]{secret.name}[/bold]")
        console.print(f"   更新时间: {secret.updated_at}")
        
        # 使用inquirer选择操作
        questions = [
            inquirer.List('action',
                         message=f"选择对 {secret.name} 的操作",
                         choices=[
                             ('直接复制', 'copy'),
                             ('修改名称后复制', 'rename'),
                             ('跳过此项', 'skip')
                         ],
                         default='copy',  # 默认选择直接复制
                         carousel=True)
        ]
        
        try:
            answer = inquirer.prompt(questions)
            if not answer or answer['action'] == 'skip':
                console.print(f"⏭️  跳过 {secret.name}", style="yellow")
                continue
            
            secret_name = secret.name
            if answer['action'] == 'rename':
                new_name_question = [
                    inquirer.Text('new_name',
                                 message=f"输入新名称 (当前: {secret.name})",
                                 default=secret.name)
                ]
                name_answer = inquirer.prompt(new_name_question)
                if name_answer and name_answer['new_name'].strip():
                    secret_name = name_answer['new_name'].strip()
            
            # 获取secret值
            value_question = [
                inquirer.Password('value',
                                message=f"请输入 {secret_name} 的值")
            ]
            value_answer = inquirer.prompt(value_question)
            if not value_answer or not value_answer['value'].strip():
                console.print(f"❌ 跳过 {secret_name} (未提供值)", style="red")
                continue
            
            secret_value = value_answer['value'].strip()
            
            # 设置secret
            try:
                cmd = ['gh', 'secret', 'set', secret_name, '--repo', target_repo, '--body', secret_value]
                result = subprocess.run(cmd, capture_output=True, text=True, check=True,
                                      encoding='utf-8', errors='ignore')
                console.print(f"✅ 成功复制 secret: {secret_name}", style="green")
            except subprocess.CalledProcessError as e:
                console.print(f"❌ 复制 secret {secret_name} 失败: {e.stderr}", style="red")
                
        except KeyboardInterrupt:
            console.print("\n👋 用户中断操作", style="yellow")
            break


def copy_variables_tui(variables: List[Variable], target_repo: str, manager: GitHubSecretsManager):
    """使用TUI交互式复制variables"""
    if not variables:
        console.print("📝 没有variables需要复制", style="yellow")
        return
    
    console.print(f"\n🔧 开始处理 {len(variables)} 个Variables", style="bold blue")
    
    for i, variable in enumerate(variables, 1):
        console.print(f"\n[{i}/{len(variables)}] 处理 Variable: [bold]{variable.name}[/bold]")
        console.print(f"   当前值: {variable.value}")
        console.print(f"   更新时间: {variable.updated_at}")
        
        # 使用inquirer选择操作
        questions = [
            inquirer.List('action',
                         message=f"选择对 {variable.name} 的操作",
                         choices=[
                             ('直接复制', 'copy'),
                             ('修改名称/值后复制', 'modify'),
                             ('跳过此项', 'skip')
                         ],
                         default='copy',  # 默认选择直接复制
                         carousel=True)
        ]
        
        try:
            answer = inquirer.prompt(questions)
            if not answer or answer['action'] == 'skip':
                console.print(f"⏭️  跳过 {variable.name}", style="yellow")
                continue
            
            variable_name = variable.name
            variable_value = variable.value
            
            if answer['action'] == 'modify':
                # 修改名称
                name_question = [
                    inquirer.Text('new_name',
                                 message=f"输入新名称 (当前: {variable.name}, 回车保持不变)",
                                 default=variable.name)
                ]
                name_answer = inquirer.prompt(name_question)
                if name_answer and name_answer['new_name'].strip():
                    variable_name = name_answer['new_name'].strip()
                
                # 修改值
                value_question = [
                    inquirer.Text('new_value',
                                 message=f"输入新值 (当前: {variable.value}, 回车保持不变)",
                                 default=variable.value)
                ]
                value_answer = inquirer.prompt(value_question)
                if value_answer and value_answer['new_value'].strip():
                    variable_value = value_answer['new_value'].strip()
            
            # 设置variable
            try:
                cmd = ['gh', 'variable', 'set', variable_name, '--repo', target_repo, '--body', variable_value]
                result = subprocess.run(cmd, capture_output=True, text=True, check=True,
                                      encoding='utf-8', errors='ignore')
                console.print(f"✅ 成功复制 variable: {variable_name} = {variable_value}", style="green")
            except subprocess.CalledProcessError as e:
                console.print(f"❌ 复制 variable {variable_name} 失败: {e.stderr}", style="red")
                
        except KeyboardInterrupt:
            console.print("\n👋 用户中断操作", style="yellow")
            break


def display_secrets_and_variables_rich(secrets: List[Secret], variables: List[Variable]):
    """使用Rich显示secrets和variables"""
    
    # 显示Secrets
    if secrets:
        secrets_table = Table(title="🔐 Secrets", show_header=True, header_style="bold magenta")
        secrets_table.add_column("序号", style="dim", width=6)
        secrets_table.add_column("名称", style="cyan")
        secrets_table.add_column("更新时间", style="green")
        
        for i, secret in enumerate(secrets, 1):
            secrets_table.add_row(str(i), secret.name, secret.updated_at)
        
        console.print(secrets_table)
    else:
        console.print(Panel("无secrets", title="🔐 Secrets", border_style="dim"))
    
    # 显示Variables
    if variables:
        variables_table = Table(title="🔧 Variables", show_header=True, header_style="bold magenta")
        variables_table.add_column("序号", style="dim", width=6)
        variables_table.add_column("名称", style="cyan")
        variables_table.add_column("值", style="yellow")
        variables_table.add_column("更新时间", style="green")
        
        for i, var in enumerate(variables, 1):
            variables_table.add_row(str(i), var.name, var.value, var.updated_at)
        
        console.print(variables_table)
    else:
        console.print(Panel("无variables", title="🔧 Variables", border_style="dim"))


def main():
    """主函数"""
    console.print(Panel.fit("🚀 GitHub 密钥和环境变量复制工具 (TUI版)", style="bold blue"))
    
    manager = GitHubSecretsManager()
    
    # 搜索源仓库
    while True:
        search_query = console.input("\n🔍 搜索您的仓库 (回车显示所有仓库): ").strip()
        
        with console.status("[bold green]搜索仓库中..."):
            repositories = manager.search_repositories(search_query, limit=30)
        
        if not repositories:
            console.print("❌ 没有找到仓库，请重新搜索", style="red")
            continue
        
        source_repo = select_repository_tui(repositories)
        
        if source_repo is None:
            console.print("👋 已退出", style="yellow")
            return
        
        console.print(f"\n✓ 选择源仓库: [bold cyan]{source_repo.full_name}[/bold cyan]")
        break
    
    # 获取secrets和variables
    with console.status("[bold green]获取仓库配置..."):
        secrets = manager.get_secrets(source_repo.full_name)
        variables = manager.get_variables(source_repo.full_name)
    
    display_secrets_and_variables_rich(secrets, variables)
    
    if not secrets and not variables:
        console.print("❌ 该仓库没有secrets或variables可复制", style="red")
        return

    # 输入目标仓库
    target_repo_question = [
        inquirer.Text('target_repo',
                     message="🎯 输入目标仓库 (格式: owner/repo)",
                     validate=lambda _, x: re.match(r'^[\w\-\.]+/[\w\-\.]+$', x) is not None)
    ]
    
    try:
        target_answer = inquirer.prompt(target_repo_question)
        if not target_answer:
            console.print("👋 已退出", style="yellow")
            return
        
        target_repo = target_answer['target_repo']
        
        console.print(f"\n🔄 开始交互式复制到 [bold cyan]{target_repo}[/bold cyan]")
        console.print("=" * 60)

        # 交互式复制secrets
        if secrets:
            copy_secrets_tui(secrets, target_repo, manager)

        # 交互式复制variables  
        if variables:
            copy_variables_tui(variables, target_repo, manager)

        console.print(f"\n✅ 交互式复制完成!", style="bold green")
        
    except KeyboardInterrupt:
        console.print("\n👋 用户中断，已退出", style="yellow")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        console.print("\n\n👋 用户中断，已退出", style="yellow")
    except Exception as e:
        console.print(f"❌ 发生错误: {e}", style="red")
        raise
