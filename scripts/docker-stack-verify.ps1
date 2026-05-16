# Quick smoke test after: docker compose up --build -d
param(
    [int]$AppPort = 8080,
    [int]$MaxWaitSeconds = 180
)

$deadline = (Get-Date).AddSeconds($MaxWaitSeconds)
Write-Host "Waiting for app readiness (max ${MaxWaitSeconds}s)..."

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$AppPort/actuator/health/readiness" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "OK: readiness probe passed"
            $r = Invoke-WebRequest -Uri "http://127.0.0.1:$AppPort/actuator/health" -UseBasicParsing
            Write-Host $r.Content
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 5
    }
}

Write-Host "FAIL: app not ready within ${MaxWaitSeconds}s"
docker compose ps
docker compose logs app --tail 80
exit 1
