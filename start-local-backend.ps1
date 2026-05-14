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
    ) | Out-null
}

$serviceCommands = @{
    'auth-service'         = "cd auth-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'project-service'      = "cd project-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'file-service'         = "cd file-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'collab-service'       = "cd collab-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'execution-service'    = "cd execution-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'version-service'      = "cd version-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'comment-service'      = "cd comment-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'notification-service' = "cd notification-service; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
    'codesync-gateway'     = "cd codesync-gateway; mvn ""spring-boot:run"" ""-Dspring.profiles.active=local"""
}

Write-Host ''
Write-Host '========================================'
Write-Host 'CodeSync - Local Development Mode'
Write-Host '========================================'
Write-Host ''
Write-Host 'This script runs all services with local profile (--spring.profiles.active=local)'
Write-Host ''
Write-Host 'Prerequisites:'
Write-Host '  - MySQL on localhost:3307 (user: root, password: root)'
Write-Host '  - Redis on localhost:6379'
Write-Host '  - RabbitMQ on localhost:5672 (user: guest, password: guest)'
Write-Host ''

if (-not $SkipInfra) {
    Write-Host '[INFRA] Starting MySQL, Redis, RabbitMQ via docker compose...'
    docker compose up -d mysql redis rabbitmq
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
        Start-Sleep -Seconds 5
    }
}

Write-Host ''
Write-Host '[DONE] Backend launch sequence triggered with local profile!'
Write-Host ''
Write-Host 'Gateway: http://localhost:8080'
Write-Host 'MySQL:   localhost:3307 (user: root, password: root)'
Write-Host 'Redis:   localhost:6379'
Write-Host 'RabbitMQ: localhost:5672 (guest/guest)'
Write-Host ''
Write-Host 'Use -SkipInfra if MySQL/Redis/RabbitMQ are already running.'