$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot

$requiredFiles = @(
    (Join-Path $root 'k8s\overlays\staging\secrets.env')
    (Join-Path $root 'k8s\overlays\production\secrets.env')
)

foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing $file. Run ./scripts/k8s-overlay-bootstrap.ps1 first."
    }
}

Push-Location $root
try {
    kubectl kustomize k8s/base | Out-Null
    Write-Host 'KUSTOMIZE_BUILD_BASE_OK'

    kubectl kustomize k8s/overlays/local | Out-Null
    Write-Host 'KUSTOMIZE_BUILD_LOCAL_OK'

    kubectl kustomize k8s/overlays/staging | Out-Null
    Write-Host 'KUSTOMIZE_BUILD_STAGING_OK'

    kubectl kustomize k8s/overlays/production | Out-Null
    Write-Host 'KUSTOMIZE_BUILD_PRODUCTION_OK'
}
finally {
    Pop-Location
}
