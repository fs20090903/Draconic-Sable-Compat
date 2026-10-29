param([string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$testRoot = Join-Path $ProjectRoot 'test-libs'
$source = Join-Path $testRoot 'sodium-neoforge-0.8.13+mc1.21.1.jar'
$extracted = Join-Path $testRoot 'sodium-extracted'
[IO.Directory]::CreateDirectory($extracted) | Out-Null
$outer = [IO.Compression.ZipFile]::OpenRead($source)
try {
    foreach ($entry in $outer.Entries) {
        if ($entry.FullName.EndsWith('.jar')) {
            # Only basenames are used; archive paths cannot escape this directory.
            $destination = Join-Path $extracted ([IO.Path]::GetFileName($entry.FullName))
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$destination,$true)
        }
    }
    $inner = Join-Path $extracted 'net.caffeinemc.sodium-neoforge-0.8.13+mc1.21.1-mod.jar'
    $combinedPath = Join-Path $extracted 'sodium-dev-combined.jar'
    Copy-Item -LiteralPath $inner -Destination $combinedPath -Force
    $combined = [IO.Compression.ZipFile]::Open($combinedPath,[IO.Compression.ZipArchiveMode]::Update)
    try {
        foreach ($entry in $outer.Entries) {
            if (!$entry.FullName.EndsWith('.class') -or $combined.GetEntry($entry.FullName)) { continue }
            $added = $combined.CreateEntry($entry.FullName)
            $inputStream = $entry.Open(); $outputStream = $added.Open()
            try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose(); $inputStream.Dispose() }
        }
    } finally { $combined.Dispose() }
} finally { $outer.Dispose() }
Write-Output "Created development-only Sodium jar: $combinedPath"
