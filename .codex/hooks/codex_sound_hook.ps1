param(
    [ValidateSet("ready", "thinking", "working", "processing", "warning", "finished", "complete", "error", "offline")]
    [string]$Kind = "complete"
)

$ErrorActionPreference = "SilentlyContinue"

$HookInput = ""
try {
    if (-not [Console]::IsInputRedirected) {
        $HookInput = ""
    } else {
        $HookInput = [Console]::In.ReadToEnd()
    }
} catch {
    $HookInput = ""
}

function Play-WarningSound {
    [Console]::Beep(880, 140)
    [Console]::Beep(660, 180)
}

function Play-CompletionSound {
    [Console]::Beep(660, 120)
    [Console]::Beep(880, 180)
}

function Convert-ToAgentStatus {
    param(
        [string]$Kind
    )

    switch ($Kind.ToLowerInvariant()) {
        "ready" { return "READY" }
        "thinking" { return "THINKING" }
        "working" { return "WORKING" }
        "processing" { return "PROCESSING" }
        "warning" { return "WARNING" }
        "finished" { return "FINISHED" }
        "complete" { return "FINISHED" }
        "error" { return "ERROR" }
        "offline" { return "OFFLINE" }
        default { return "FINISHED" }
    }
}

function Read-CodingAgentSessionConfig {
    $configPath = Join-Path (Get-Location) ".codex\coding_agent_session.yaml"
    if (-not (Test-Path $configPath)) {
        return @{}
    }

    $config = @{}
    foreach ($line in Get-Content -Path $configPath -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }
        $match = [regex]::Match($trimmed, "^([A-Za-z0-9_-]+)\s*:\s*(.*)$")
        if (-not $match.Success) {
            continue
        }
        $key = $match.Groups[1].Value
        $value = $match.Groups[2].Value.Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $config[$key] = $value
    }
    return $config
}

function Get-HookJsonValue {
    param(
        [object]$Payload,
        [string[]]$Names
    )

    if ($null -eq $Payload) {
        return $null
    }

    foreach ($name in $Names) {
        if ($Payload.PSObject.Properties.Name -contains $name) {
            $value = $Payload.$name
            if ($null -ne $value -and "$value".Trim().Length -gt 0) {
                return "$value"
            }
        }
    }

    foreach ($property in $Payload.PSObject.Properties) {
        if ($property.Value -is [pscustomobject]) {
            $value = Get-HookJsonValue -Payload $property.Value -Names $Names
            if ($null -ne $value) {
                return $value
            }
        }
    }

    return $null
}

function Send-CodingAgentSessionStatus {
    param(
        [string]$Status,
        [string]$InputJson
    )

    $config = Read-CodingAgentSessionConfig
    if ($config.Count -eq 0) {
        return
    }
    if ($config.ContainsKey("enabled") -and "$($config["enabled"])".ToLowerInvariant() -eq "false") {
        return
    }

    $backendUrl = "$($config["backendUrl"])".Trim()
    $agentId = "$($config["agentId"])".Trim()
    $pairCode = "$($config["pairCode"])".Trim()
    if ($backendUrl.Length -eq 0 -or $agentId.Length -eq 0 -or $pairCode.Length -eq 0) {
        return
    }
    if (-not ($agentId -match "^\d+$")) {
        return
    }

    $payload = $null
    if ($InputJson.Trim().Length -gt 0) {
        try {
            $payload = $InputJson | ConvertFrom-Json
        } catch {
            $payload = $null
        }
    }

    $sessionId = "$($config["sessionId"])".Trim()
    if ($sessionId.Length -eq 0) {
        $sessionId = Get-HookJsonValue -Payload $payload -Names @("session_id", "sessionId", "conversation_id", "conversationId", "thread_id", "threadId")
    }
    if ($null -eq $sessionId -or "$sessionId".Trim().Length -eq 0) {
        $sessionId = $env:CODEX_SESSION_ID
    }
    if ($null -eq $sessionId -or "$sessionId".Trim().Length -eq 0) {
        $sessionId = $env:CODEX_THREAD_ID
    }
    if ($null -eq $sessionId -or "$sessionId".Trim().Length -eq 0) {
        $sessionId = Split-Path -Leaf (Get-Location)
    }

    $cwd = "$($config["cwd"])".Trim()
    if ($cwd.Length -eq 0) {
        $cwd = Get-HookJsonValue -Payload $payload -Names @("cwd", "workspace", "workspace_dir", "workspaceDir")
    }
    if ($null -eq $cwd -or "$cwd".Trim().Length -eq 0) {
        $cwd = (Get-Location).Path
    }

    $uploadUrl = "$($config["uploadUrl"])".Trim()
    if ($uploadUrl.Length -eq 0) {
        $uploadUrl = $backendUrl.TrimEnd("/") + "/api/v2/codingAgentSession/upload"
    }

    $body = @{
        agentId = [int]$agentId
        pairCode = $pairCode
        sessionId = "$sessionId".Trim()
        cwd = "$cwd".Trim()
        finalStatus = $Status
    } | ConvertTo-Json -Compress

    try {
        $utf8NoBom = New-Object System.Text.UTF8Encoding -ArgumentList $false
        $bodyBytes = $utf8NoBom.GetBytes($body)
        Invoke-RestMethod -Method Post -Uri $uploadUrl -ContentType "application/json; charset=utf-8" -Body $bodyBytes -TimeoutSec 2 | Out-Null
    } catch {
        return
    }
}

$Status = Convert-ToAgentStatus -Kind $Kind

if ($Kind -eq "warning") {
    Play-WarningSound
    Send-CodingAgentSessionStatus -Status $Status -InputJson $HookInput
    exit 0
}

if ($Status -eq "FINISHED") {
    Play-CompletionSound
}
Send-CodingAgentSessionStatus -Status $Status -InputJson $HookInput
if ($Kind -eq "complete" -or $Kind -eq "finished") {
    Write-Output '{"continue":true}'
}
exit 0
