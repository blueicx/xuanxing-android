param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [int]$AdbPort = 5038,
    [ValidateRange(1, 10)][int]$Iterations = 3,
    [string]$AdbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
# This diagnostic is deliberately restricted to emulators. It never wipes app data.
if ($Serial -notmatch '^emulator-[0-9]+$') { throw 'Only an isolated emulator serial is accepted.' }
function Invoke-IsolatedAdb([string[]]$Arguments) {
    $result = & $AdbPath -P $AdbPort -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB diagnostic failed: $($Arguments -join ' ')" }
    return ($result -join "`n")
}
if ((Invoke-IsolatedAdb -Arguments @('shell', 'getprop', 'ro.kernel.qemu')).Trim() -ne '1') {
    throw 'Target does not identify as an emulator.'
}
$samples = for ($iteration = 0; $iteration -lt $Iterations; $iteration++) {
    Invoke-IsolatedAdb -Arguments @('shell', 'am', 'force-stop', 'com.xuanji.app') | Out-Null
    $launch = Invoke-IsolatedAdb -Arguments @('shell', 'am', 'start', '-W', '-n', 'com.xuanji.app/.MainActivity')
    if ($launch -notmatch 'Status:\s*ok') { throw "Launch failed: $launch" }
    $total = [regex]::Match($launch, 'TotalTime:\s*(\d+)')
    if (-not $total.Success -or [int]$total.Groups[1].Value -le 0 -or $launch -notmatch 'Activity:\s*com\.xuanji\.app/') {
        throw 'No valid app launch measurement: an existing task or permission dialog may be intercepting launch. Do not report zero as a cold start.'
    }
    [pscustomobject]@{ iteration = $iteration + 1; totalTimeMs = [int]$total.Groups[1].Value }
}
$memory = Invoke-IsolatedAdb -Arguments @('shell', 'dumpsys', 'meminfo', 'com.xuanji.app')
$pss = [regex]::Match($memory, 'TOTAL PSS:\s*(\d+)')
$version = Invoke-IsolatedAdb -Arguments @('shell', 'dumpsys', 'package', 'com.xuanji.app')
[pscustomobject]@{
    measuredAtUtc = [DateTime]::UtcNow.ToString('o')
    serial = $Serial
    scope = 'Debug APK, initial app screen, process-cold launches; not a physical-device or stage/game benchmark'
    samples = @($samples)
    totalPssKb = $(if ($pss.Success) { [int]$pss.Groups[1].Value } else { $null })
    version = @($version -split "`n" | Where-Object { $_ -match 'versionCode=|versionName=' } | ForEach-Object { $_.Trim() })
} | ConvertTo-Json -Depth 5
