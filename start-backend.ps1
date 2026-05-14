Param(
    [switch]$SkipInfra
)

$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $backendRoot

function Start-ServiceTerminal {
    Param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    Write-Host "[STARTING] $Name"
    Start-Process powershell -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$backendRoot'; $Command"
    ) | Out-Null
}

$serviceCommands = @{
    'auth-service'         = "cd auth-service; `$env:AUTH_DB_PASSWORD='Shreya@2002'; mvn spring-boot:run"
    'project-service'      = "cd project-service; `$env:PROJECT_DB_PASSWORD='Shreya@2002'; mvn spring-boot:run"
    'file-service'         = "cd file-service; `$env:FILE_DB_PASSWORD='Shreya@2002'; mvn spring-boot:run"
    'collab-service'       = "cd collab-service; `$env:COLLAB_DB_PASSWORD='Shreya@2002'; mvn spring-boot:run"
    'execution-service'    = "cd execution-service; `$env:EXECUTION_DB_PASSWORD='Shreya@2002'; `$env:RABBITMQ_USER='guest'; `$env:RABBITMQ_PASS='guest'; mvn spring-boot:run"
    'version-service'      = "cd version-service; `$env:VERSION_DB_PASSWORD='Shreya@2002'; mvn spring-boot:run"
    'comment-service'      = "cd comment-service; `$env:COMMENT_DB_PASSWORD='Shreya@2002'; mvn spring-boot:run"
    'notification-service' = "cd notification-service; `$env:NOTIFICATION_DB_PASSWORD='Shreya@2002'; `$env:RABBITMQ_USER='guest'; `$env:RABBITMQ_PASS='guest'; mvn spring-boot:run"
    'codesync-gateway'     = "cd codesync-gateway; mvn spring-boot:run"
}

if (-not $SkipInfra) {
    Write-Host '[INFRA] Starting Redis, RabbitMQ via docker compose...'
    docker compose up -d redis rabbitmq
}

$eurekaModuleCandidates = @('eureka-server', 'discovery-service', 'service-registry')
$eurekaModule = $null

foreach ($candidate in $eurekaModuleCandidates) {
    if (Test-Path (Join-Path $backendRoot $candidate)) {
        $eurekaModule = $candidate
        break
    }
}

if ($eurekaModule) {
    Start-ServiceTerminal -Name $eurekaModule -Command "cd $eurekaModule; mvn spring-boot:run"
    Write-Host '[WAIT] Waiting 20 seconds for Eureka to come up...'
    Start-Sleep -Seconds 20
} else {
    Write-Host '[WARN] No Eureka module found (expected one of: eureka-server, discovery-service, service-registry).'
    Write-Host '[WAIT] Waiting 20 seconds before starting remaining services...'
    Start-Sleep -Seconds 20
}

$startOrder = @(
    'auth-service',
    'project-service',
    'file-service',
    'collab-service',
    'execution-service',
    'version-service',
    'comment-service',
    'notification-service',
    'codesync-gateway'
)

foreach ($service in $startOrder) {
    if ($serviceCommands.ContainsKey($service)) {
        Start-ServiceTerminal -Name $service -Command $serviceCommands[$service]
        Start-Sleep -Seconds 2
    }
}

Write-Host ''
Write-Host '[DONE] Backend launch sequence triggered.'
Write-Host 'Use -SkipInfra if MySQL/Redis/RabbitMQ are already running.'
