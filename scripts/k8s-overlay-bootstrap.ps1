$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$targets = @(
    (Join-Path $root 'k8s\overlays\staging')
    (Join-Path $root 'k8s\overlays\production')
)

foreach ($dir in $targets) {
    $example = Join-Path $dir 'secrets.env.example'
    $target = Join-Path $dir 'secrets.env'

    if (-not (Test-Path $example)) {
        throw "Missing template: $example"
    }

    if (-not (Test-Path $target)) {
        Copy-Item $example $target
        Write-Host "Created $target from template"
    } else {
        Write-Host "Exists: $target"
    }

    $required = @(
        'DATABASE_USERNAME',
        'DATABASE_PASSWORD',
        'JWT_SECRET',
        'RABBITMQ_USERNAME',
        'RABBITMQ_PASSWORD'
    )

    $content = Get-Content $target
    foreach ($key in $required) {
        if (-not ($content | Where-Object { $_ -match "^$key=" })) {
            throw "Missing key '$key' in $target"
        }
    }
}

Write-Host 'Overlay bootstrap complete. Update secrets.env values before non-local deployment.'
