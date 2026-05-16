$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot

function Require-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $name"
    }
    Write-Host "OK command: $name"
}

function Require-Path($path) {
    if (-not (Test-Path $path)) {
        throw "Missing required path: $path"
    }
    Write-Host "OK path: $path"
}

Require-Command docker
Require-Command kubectl
Require-Command mvn

$terraformCmd = Get-Command terraform -ErrorAction SilentlyContinue
if ($terraformCmd) {
    Write-Host 'OK command: terraform'
} else {
    Write-Warning 'terraform not found; Terraform execution checks skipped.'
}

$requiredPaths = @(
    (Join-Path $root 'Dockerfile')
    (Join-Path $root 'docker-compose.yml')
    (Join-Path $root 'DEPLOYMENT.md')
    (Join-Path $root '.github\workflows\ci.yml')
    (Join-Path $root '.github\workflows\terraform-validate.yml')
    (Join-Path $root 'k8s\base\deployment.yaml')
    (Join-Path $root 'k8s\base\service.yaml')
    (Join-Path $root 'k8s\base\configmap.yaml')
    (Join-Path $root 'k8s\base\poddisruptionbudget.yaml')
    (Join-Path $root 'k8s\base\secret.example.yaml')
    (Join-Path $root 'terraform\main.tf')
    (Join-Path $root 'terraform\variables.tf')
    (Join-Path $root 'terraform\outputs.tf')
    (Join-Path $root 'terraform\terraform.tfvars.example')
    (Join-Path $root 'terraform\README.md')
    (Join-Path $root 'PHASE3-EVIDENCE-INDEX.md')
)

foreach ($p in $requiredPaths) {
    Require-Path $p
}

Push-Location $root
try {
    docker compose -f docker-compose.yml config | Out-Null
    Write-Host 'COMPOSE_CONFIG_OK'

    & "$root\scripts\k8s-overlay-bootstrap.ps1" | Out-Null
    & "$root\scripts\k8s-overlay-validate.ps1" | Out-Null

    if ($terraformCmd) {
        Push-Location (Join-Path $root 'terraform')
        try {
            terraform fmt -check -recursive
            Write-Host 'TERRAFORM_FMT_OK'
        }
        finally {
            Pop-Location
        }
    }
}
finally {
    Pop-Location
}

Write-Host 'PHASE3_PREFLIGHT_OK'
