$ErrorActionPreference = 'Stop'
$runtime = Join-Path $PSScriptRoot '.runtime'
if (Test-Path -LiteralPath $runtime) {
    Set-Content -LiteralPath (Join-Path $runtime 'stop') -Value 'stop'
    Write-Output 'Stop requested. Supervisor will terminate its own model process within a few seconds.'
} else {
    Write-Output 'No Laya runtime exists.'
}
