<#
.SYNOPSIS
    EatWhat 外卖平台 一键启停脚本

.DESCRIPTION
    统一管理三个服务：
        MySQL    127.0.0.1:3306
        后端 API  http://127.0.0.1:8080
        前端 Vite http://127.0.0.1:5173

    所有服务均以普通后台进程运行，脚本本身不会注册任何开机自启动
    （不写注册表 Run 项、不写启动文件夹、不创建 Windows 服务或计划任务）。
    执行 stop 或重启电脑后即全部停止。

.PARAMETER Action
    start   启动全部服务（含健康检查）
    stop    关闭全部服务
    restart 先关后开
    status  查看当前状态（默认）

.PARAMETER Rebuild
    启动后端前强制重新构建 jar

.EXAMPLE
    .\manage.ps1 start
    .\manage.ps1 stop
    .\manage.ps1 status
    .\manage.ps1 restart -Rebuild
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('start', 'stop', 'restart', 'status')]
    [string]$Action = 'status',

    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

# ============================== 路径配置 ==============================
$Root      = 'D:\IT\Java\EatWhat'
$JavaHome  = 'D:\Tools\jdk\jdk-17.0.20.1+1'
$MvnCmd    = 'D:\Tools\apache-maven-3.9.16\bin\mvn.cmd'
$MysqlHome = 'D:\Tools\mysql-8.0.28-winx64'
$MysqlIni  = Join-Path $MysqlHome 'my.ini'
$NodeExe   = 'D:\Program Files\nodejs\node.exe'

$JarPath = Join-Path $Root 'backend\target\eatwhat-backend.jar'
$ViteJs  = Join-Path $Root 'frontend\node_modules\vite\bin\vite.js'
$LogDir  = Join-Path $Root 'logs'

$PortMysql   = 3306
$PortBackend = 8080
$PortFront   = 5173
$FrontUrl    = "http://127.0.0.1:$PortFront"
$ApiHealth   = "http://127.0.0.1:$PortBackend/api/v1/shops"
$DocUrl      = "http://127.0.0.1:$PortBackend/doc.html"

# ============================== 输出辅助 ==============================
function Write-Step([string]$Msg) { Write-Host "  $Msg" -ForegroundColor Cyan }
function Write-Ok([string]$Msg)   { Write-Host "  [OK] $Msg" -ForegroundColor Green }
function Write-Note([string]$Msg) { Write-Host "  [--] $Msg" -ForegroundColor Yellow }
function Write-Err([string]$Msg)  { Write-Host "  [!!] $Msg" -ForegroundColor Red }

# ============================== 端口工具 ==============================
function Get-PortPid([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($conn) { return [int]$conn.OwningProcess }
    return $null
}

function Wait-Port([int]$Port, [int]$TimeoutSec, [string]$Name) {
    for ($i = 0; $i -lt $TimeoutSec; $i++) {
        if (Get-PortPid $Port) {
            Write-Ok "$Name 已就绪（端口 $Port）"
            return $true
        }
        Start-Sleep -Seconds 1
    }
    Write-Err "$Name 启动超时（${TimeoutSec}s 内端口 $Port 未监听）"
    return $false
}

function Wait-Http([string]$Url, [int]$TimeoutSec, [string]$Name) {
    for ($i = 0; $i -lt $TimeoutSec; $i++) {
        $code = & curl.exe -s -o NUL -w "%{http_code}" --max-time 3 $Url 2>$null
        if ("$code" -match '^2\d\d$') {
            Write-Ok "$Name 健康检查通过（HTTP $code）"
            return $true
        }
        Start-Sleep -Seconds 1
    }
    Write-Err "$Name 健康检查失败（$Url）"
    return $false
}

function Stop-PortProcess([int]$Port, [string]$Name) {
    $procId = Get-PortPid $Port
    if (-not $procId) {
        Write-Note "$Name 未运行（端口 $Port 空闲）"
        return
    }
    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        for ($i = 0; $i -lt 20; $i++) {
            Start-Sleep -Milliseconds 300
            if (-not (Get-PortPid $Port)) { break }
        }
        Write-Ok "$Name 已停止（pid $procId）"
    } catch {
        Write-Err "$Name 停止失败：$($_.Exception.Message)"
    }
}

# ============================== 各服务 ==============================
function Start-MySql {
    if (Get-PortPid $PortMysql) {
        Write-Note "MySQL 已在运行（端口 $PortMysql）"
        return $true
    }
    $mysqld = Join-Path $MysqlHome 'bin\mysqld.exe'
    if (-not (Test-Path $mysqld)) { Write-Err "找不到 mysqld.exe：$mysqld"; return $false }
    if (-not (Test-Path $MysqlIni)) { Write-Err "找不到配置文件：$MysqlIni"; return $false }

    Write-Step '启动 MySQL ...'
    Start-Process -FilePath $mysqld `
        -ArgumentList "--defaults-file=$MysqlIni", '--console' `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'mysql.log') `
        -RedirectStandardError  (Join-Path $LogDir 'mysql.err.log') | Out-Null

    return (Wait-Port $PortMysql 45 'MySQL')
}

function Stop-MySql {
    $procId = Get-PortPid $PortMysql
    if (-not $procId) {
        Write-Note "MySQL 未运行（端口 $PortMysql 空闲）"
        return
    }
    $mysqladmin = Join-Path $MysqlHome 'bin\mysqladmin.exe'
    if (Test-Path $mysqladmin) {
        Write-Step '正在优雅关闭 MySQL ...'
        & $mysqladmin -u root -h 127.0.0.1 -P $PortMysql shutdown 2>$null | Out-Null
        for ($i = 0; $i -lt 20; $i++) {
            Start-Sleep -Milliseconds 500
            if (-not (Get-PortPid $PortMysql)) {
                Write-Ok "MySQL 已停止（pid $procId）"
                return
            }
        }
        Write-Note '优雅关闭未生效，改用强制结束'
    }
    Stop-PortProcess $PortMysql 'MySQL'
}

function Start-Backend {
    if (Get-PortPid $PortBackend) {
        Write-Note "后端 API 已在运行（端口 $PortBackend）"
        return $true
    }

    if ($Rebuild -or -not (Test-Path $JarPath)) {
        if ($Rebuild) {
            Write-Step '按要求重新构建后端 ...'
        } else {
            Write-Note '未找到后端 jar，正在构建（首次启动会慢一些）...'
        }
        $env:JAVA_HOME = $JavaHome
        Push-Location (Join-Path $Root 'backend')
        try {
            & $MvnCmd -o -B -q package -DskipTests
        } finally {
            Pop-Location
        }
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $JarPath)) {
            Write-Err '后端构建失败，请检查 Maven 输出'
            return $false
        }
        Write-Ok '后端构建完成'
    }

    $java = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path $java)) { Write-Err "找不到 java.exe：$java"; return $false }

    Write-Step '启动后端 API ...'
    Start-Process -FilePath $java `
        -ArgumentList '-Dfile.encoding=UTF-8', '-jar', $JarPath `
        -WorkingDirectory (Join-Path $Root 'backend') `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'backend.log') `
        -RedirectStandardError  (Join-Path $LogDir 'backend.err.log') | Out-Null

    if (-not (Wait-Port $PortBackend 90 '后端 API')) { return $false }
    return (Wait-Http $ApiHealth 40 '后端 API')
}

function Start-Frontend {
    if (Get-PortPid $PortFront) {
        Write-Note "前端 Vite 已在运行（端口 $PortFront）"
        return $true
    }
    if (-not (Test-Path $NodeExe)) { Write-Err "找不到 node.exe：$NodeExe"; return $false }
    if (-not (Test-Path $ViteJs)) {
        Write-Err "找不到 vite：$ViteJs，请先在 frontend 目录执行 pnpm install"
        return $false
    }

    Write-Step '启动前端 Vite ...'
    Start-Process -FilePath $NodeExe `
        -ArgumentList $ViteJs `
        -WorkingDirectory (Join-Path $Root 'frontend') `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'frontend.log') `
        -RedirectStandardError  (Join-Path $LogDir 'frontend.err.log') | Out-Null

    if (-not (Wait-Port $PortFront 60 '前端 Vite')) { return $false }
    return (Wait-Http $FrontUrl 30 '前端页面')
}

# ============================== 组合动作 ==============================
function Show-Status {
    Write-Host ''
    Write-Host '  ================ EatWhat 服务状态 ================' -ForegroundColor White

    $items = @(
        [pscustomobject]@{ Name = 'MySQL 数据库'; Port = $PortMysql },
        [pscustomobject]@{ Name = '后端 API    '; Port = $PortBackend },
        [pscustomobject]@{ Name = '前端 Vite   '; Port = $PortFront }
    )

    foreach ($item in $items) {
        $procId = Get-PortPid $item.Port
        if ($procId) {
            $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
            $pname = if ($p) { $p.ProcessName } else { '?' }
            $line = '  {0} : {1,-6}  运行中  pid={2} ({3})' -f $item.Name, $item.Port, $procId, $pname
            Write-Host $line -ForegroundColor Green
        } else {
            $line = '  {0} : {1,-6}  未运行' -f $item.Name, $item.Port
            Write-Host $line -ForegroundColor DarkGray
        }
    }

    Write-Host '  ==================================================' -ForegroundColor White
    Write-Host "  前端地址 : $FrontUrl"   -ForegroundColor Cyan
    Write-Host "  接口文档 : $DocUrl"       -ForegroundColor Cyan
    Write-Host '  日志目录 : ' -NoNewline -ForegroundColor Cyan
    Write-Host $LogDir
    Write-Host ''
}

function Invoke-Start {
    Write-Host ''
    Write-Host '  ================= 启动 EatWhat =================' -ForegroundColor White
    if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

    $okMysql = Start-MySql
    if (-not $okMysql) {
        Write-Err '数据库未能启动，后端与前端无法工作，已中止'
        Write-Host ''
        return $false
    }

    $okBackend = Start-Backend
    $okFront   = Start-Frontend

    Write-Host ''
    if ($okMysql -and $okBackend -and $okFront) {
        Write-Host '  全部服务启动成功' -ForegroundColor Green
        Write-Host ''
        Write-Host "    前端页面 : $FrontUrl" -ForegroundColor Cyan
        Write-Host "    接口文档 : $DocUrl" -ForegroundColor Cyan
        Write-Host ''
        Write-Host '    演示账号：' -ForegroundColor DarkCyan
        Write-Host '      管理员  13800000000 / Admin@123' -ForegroundColor DarkCyan
        Write-Host '      消费者  13800000001 / User@123' -ForegroundColor DarkCyan
        Write-Host '      商家    13800000010 / Merchant@123' -ForegroundColor DarkCyan
        Write-Host '      骑手    13800000020 / Rider@123' -ForegroundColor DarkCyan
        Write-Host ''
        return $true
    }

    Write-Err '部分服务启动失败，请查看 logs 目录下的日志文件'
    Write-Host ''
    return $false
}

function Invoke-Stop {
    Write-Host ''
    Write-Host '  ================= 关闭 EatWhat =================' -ForegroundColor White

    Stop-PortProcess $PortFront   '前端 Vite'
    Stop-PortProcess $PortBackend '后端 API'
    Stop-MySql

    Write-Host ''
    Write-Host '  已全部关闭' -ForegroundColor Green
    Write-Host ''
}

# ============================== 入口 ==============================
switch ($Action) {
    'start'   { [void](Invoke-Start) }
    'stop'    { Invoke-Stop }
    'restart' {
        Invoke-Stop
        Start-Sleep -Seconds 2
        [void](Invoke-Start)
    }
    'status'  { Show-Status }
}
