param(
    [int]$Port = 8001,
    [ValidateSet('cpu', 'cuda', IgnoreCase = $true)]
    [string]$Device = 'cpu',
    [int]$StartupTimeoutSeconds = 300
)
$ErrorActionPreference = 'Stop'
$layaRoot = $PSScriptRoot
$runtime = Join-Path $layaRoot '.runtime'
$python = Join-Path $layaRoot '.venv\Scripts\python.exe'
if (!(Test-Path -LiteralPath $python)) { throw 'Create .venv and install requirements first.' }
if ($StartupTimeoutSeconds -le 0) { throw 'StartupTimeoutSeconds must be positive.' }
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
$pidFile = Join-Path $runtime 'supervisor.pid'
if (Test-Path -LiteralPath $pidFile) {
    $supervisorId = [int](Get-Content -LiteralPath $pidFile)
    try {
        $existing = Get-CimInstance Win32_Process -Filter "ProcessId = $supervisorId" -ErrorAction Stop
    } catch {
        # WMI process command-line inspection can be denied for another-user processes.
        # The listener check below still prevents starting a second service on the port.
        $existing = $null
    }
    if ($existing -and $existing.CommandLine -like "*$layaRoot*supervise.py*") {
        Write-Output "Laya supervisor already running (PID $supervisorId)."
        exit 0
    }
}
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
$listener.ExclusiveAddressUse = $true
try {
    $listener.Start()
} catch {
    throw "Port $Port is already in use or unavailable."
} finally {
    $listener.Stop()
}
$env:LAYA_PORT = "$Port"
$env:LAYA_DEVICE = $Device
$env:HF_HOME = Join-Path $runtime 'huggingface'
$localModel = Join-Path $runtime 'models\multilingual'
if ((Test-Path -LiteralPath (Join-Path $localModel 'verified.json')) -and !$env:LAYA_MODEL_PATH) {
    $env:LAYA_MODEL_PATH = $localModel
}
$env:PYTHONUTF8 = '1'
$env:PYTHONUNBUFFERED = '1'
$supervisorScript = Join-Path $layaRoot 'supervise.py'
$process = Start-Process -FilePath $python -ArgumentList @('-u', ('"' + $supervisorScript + '"')) `
    -WorkingDirectory $layaRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $runtime 'supervisor.log') `
    -RedirectStandardError (Join-Path $runtime 'supervisor.err.log')
$healthUrl = "http://127.0.0.1:$Port/health"
$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
do {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 5 -ErrorAction Stop
        if ($health.status -eq 'ok' -and !$health.restart_required) {
            Write-Output "Laya supervisor ready (PID $($process.Id)), $healthUrl"
            Write-Output ("workers={0}, threads_per_worker={1}, physical_cores={2}, logical_cpus={3}" -f `
                    $health.workers, $health.threads_per_worker, $health.physical_cores, $health.logical_cpus)
            exit 0
        }
    } catch {
        # The child is still loading the model, restarting, or has not bound the port yet.
    }
} while ((Get-Date) -lt $deadline)
Write-Error "Laya did not become healthy within $StartupTimeoutSeconds seconds. Check $runtime\service.log and $runtime\supervisor.log."
exit 1
