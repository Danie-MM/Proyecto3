
$ErrorActionPreference = 'Stop'

# ============================================================
# PETMATCHCR V2 - LOCAL STARTUP
# Project: J:\Cenfotec\Proyecto3
# PostgreSQL: 17
# ============================================================

# 1. PROJECT PATHS
$root = $PSScriptRoot
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'
$localConfig = Join-Path $root '.petmatch.local.json'
$jwtFile = Join-Path $root '.jwt-secret.local'

# 2. DATABASE CONFIGURATION
$database = 'petmatchcr'
$port = 5432
$postgresDefaultBin = Join-Path $env:ProgramFiles 'PostgreSQL\17\bin'

# ============================================================
# HELPER FUNCTIONS
# ============================================================

function Test-Port([int]$Number) {
    $client = $null

    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $result = $client.BeginConnect('127.0.0.1', $Number, $null, $null)
        $ok = $result.AsyncWaitHandle.WaitOne(1500)

        if ($ok) {
            $client.EndConnect($result)
        }

        return $ok
    }
    catch {
        return $false
    }
    finally {
        if ($null -ne $client) {
            $client.Dispose()
        }
    }
}

function Find-Postgres {

    $services = @(
        Get-CimInstance Win32_Service -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -like 'postgresql*' -or
            $_.DisplayName -like 'PostgreSQL*'
        } |
        Sort-Object Name -Descending
    )

    $service = $null
    $paths = @()

    if ($services.Count -gt 0) {
        $service = $services[0]

        if ($service.Name -match '(\d+)$') {
            $version = $Matches[1]
            $paths += Join-Path $env:ProgramFiles "PostgreSQL\$version\bin"
        }
    }

    # Explicit PostgreSQL 17 path
    $paths += $postgresDefaultBin

    # Search other PostgreSQL installations
    foreach ($base in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {

        if (-not $base) {
            continue
        }

        $parent = Join-Path $base 'PostgreSQL'

        if (Test-Path $parent) {
            $paths += @(
                Get-ChildItem $parent -Directory -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending |
                ForEach-Object {
                    Join-Path $_.FullName 'bin'
                }
            )
        }
    }

    # Search psql.exe in PATH
    $command = Get-Command 'psql.exe' -ErrorAction SilentlyContinue

    if ($command) {
        $paths += Split-Path $command.Source
    }

    foreach ($path in ($paths | Select-Object -Unique)) {

        $psqlPath = Join-Path $path 'psql.exe'

        if (Test-Path $psqlPath) {
            return @{
                Service = $service
                PgBin = $path
                Psql = $psqlPath
            }
        }
    }

    return $null
}

function Get-Secret {

    if (Test-Path $jwtFile) {
        $value = (Get-Content $jwtFile -Raw).Trim()

        if ($value) {
            return $value
        }
    }

    $bytes = [byte[]]::new(64)

    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    $value = [Convert]::ToBase64String($bytes)

    [System.IO.File]::WriteAllText($jwtFile, $value)

    return $value
}

function Quote-PS([string]$value) {
    return "'" + $value.Replace("'", "''") + "'"
}

# ============================================================
# 3. VALIDATE PROJECT STRUCTURE
# ============================================================

Write-Host ''
Write-Host '========================================' -ForegroundColor Cyan
Write-Host '       PETMATCHCR V2 - STARTUP' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host ''

Write-Host "Project root: $root"
Write-Host "Backend:      $backend"
Write-Host "Frontend:     $frontend"

if (-not (Test-Path $backend)) {
    throw "Backend folder not found: $backend"
}

if (-not (Test-Path $frontend)) {
    throw "Frontend folder not found: $frontend"
}

$mvn = Join-Path $backend 'mvnw.cmd'

if (-not (Test-Path $mvn) -and
    -not (Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue) -and
    -not (Get-Command 'mvn.exe' -ErrorAction SilentlyContinue)) {

    throw 'Maven wrapper or Maven is required.'
}

if (-not (Get-Command 'java.exe' -ErrorAction SilentlyContinue)) {
    throw 'Java is not installed or not in PATH.'
}

if (-not (Get-Command 'npm.cmd' -ErrorAction SilentlyContinue)) {
    throw 'Node.js/npm is not installed or not in PATH.'
}

if (-not (Test-Path (Join-Path $frontend 'package.json'))) {
    throw 'frontend/package.json was not found.'
}

Write-Host 'Project paths validated.' -ForegroundColor Green

# ============================================================
# 4. FIND POSTGRESQL
# ============================================================

Write-Host ''
Write-Host 'Checking PostgreSQL...' -ForegroundColor Yellow

$pg = Find-Postgres

if (-not $pg) {
    throw "PostgreSQL psql.exe was not found. Expected: $postgresDefaultBin"
}

$psql = $pg.Psql

Write-Host "PostgreSQL executable: $psql" -ForegroundColor Green

# ============================================================
# 5. START POSTGRESQL SERVICE
# ============================================================

if (-not (Test-Port $port)) {

    if ($pg.Service) {

        Write-Host "Starting service: $($pg.Service.Name)" -ForegroundColor Yellow

        try {
            Start-Service -Name $pg.Service.Name -ErrorAction Stop
        }
        catch {
            throw "Cannot start PostgreSQL. Run PowerShell as Administrator. Details: $($_.Exception.Message)"
        }

    }
    else {
        throw 'PostgreSQL service was not found and port 5432 is closed.'
    }

    $ready = $false

    for ($i = 0; $i -lt 30; $i++) {

        if (Test-Port $port) {
            $ready = $true
            break
        }

        Start-Sleep -Seconds 1
    }

    if (-not $ready) {
        throw 'PostgreSQL did not become available on port 5432.'
    }
}

Write-Host 'PostgreSQL is running.' -ForegroundColor Green

# ============================================================
# 6. LOAD LOCAL CONFIGURATION
# ============================================================

$config = @{}

if (Test-Path $localConfig) {

    try {
        $saved = Get-Content $localConfig -Raw | ConvertFrom-Json

        if ($saved.DB_USERNAME) {
            $config.DB_USERNAME = [string]$saved.DB_USERNAME
        }

        if ($saved.GOOGLE_CLIENT_ID) {
            $config.GOOGLE_CLIENT_ID = [string]$saved.GOOGLE_CLIENT_ID
        }
    }
    catch {
        Write-Warning 'Local configuration could not be read.'
    }
}

# ============================================================
# 7. DATABASE CREDENTIALS
# ============================================================

Write-Host ''
Write-Host 'PostgreSQL configuration' -ForegroundColor Cyan

$defaultUser = if ($config.DB_USERNAME) {
    $config.DB_USERNAME
}
else {
    'postgres'
}

$userInput = Read-Host "PostgreSQL username [$defaultUser]"

$dbUser = if ([string]::IsNullOrWhiteSpace($userInput)) {
    $defaultUser
}
else {
    $userInput.Trim()
}

$securePassword = Read-Host 'PostgreSQL password' -AsSecureString

$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $dbPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

if ([string]::IsNullOrWhiteSpace($dbPassword)) {
    throw 'PostgreSQL password cannot be empty.'
}

# ============================================================
# 8. GOOGLE OAUTH CONFIGURATION
# ============================================================

Write-Host ''
Write-Host 'Google OAuth configuration' -ForegroundColor Cyan

$defaultClientId = if ($config.GOOGLE_CLIENT_ID) {
    $config.GOOGLE_CLIENT_ID
}
else {
    ''
}

$clientPrompt = if ($defaultClientId) {
    "Your Google OAuth Client ID [$defaultClientId]"
}
else {
    'Your own Google OAuth Client ID (required)'
}

$clientInput = Read-Host $clientPrompt

$clientId = if ([string]::IsNullOrWhiteSpace($clientInput)) {
    $defaultClientId
}
else {
    $clientInput.Trim()
}

if ($clientId -notmatch '^[a-zA-Z0-9-]+\.apps\.googleusercontent\.com$') {
    throw 'Invalid Google Client ID.'
}

# ============================================================
# 9. ENVIRONMENT VARIABLES
# ============================================================

$env:DB_URL = "jdbc:postgresql://localhost:${port}/${database}"
$env:DB_USERNAME = $dbUser
$env:DB_PASSWORD = $dbPassword
$env:GOOGLE_CLIENT_ID = $clientId
$env:JWT_SECRET = Get-Secret

# ============================================================
# 10. VERIFY POSTGRESQL CREDENTIALS
# ============================================================

Write-Host ''
Write-Host 'Verifying PostgreSQL credentials...' -ForegroundColor Yellow

$env:PGPASSWORD = $dbPassword

try {

    & $psql -h localhost -p $port -U $dbUser -d postgres -tAc 'SELECT 1' | Out-Null

    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0) {
        throw 'PostgreSQL authentication failed. Check username and password.'
    }

    Write-Host 'PostgreSQL authentication successful.' -ForegroundColor Green

    # Check whether the database exists

    $output = & $psql -h localhost -p $port -U $dbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'"

    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0) {
        throw 'Could not query PostgreSQL databases.'
    }

    $exists = if ($null -ne $output) {
        ([string]($output -join '')).Trim()
    }
    else {
        ''
    }

    # Create database if necessary

    if ($exists -ne '1') {

        Write-Host "Database '$database' does not exist." -ForegroundColor Yellow

        $answer = Read-Host "Create database '$database'? (Y/N)"

        if ($answer -match '^[YySs]') {

            & $psql -h localhost -p $port -U $dbUser -d postgres -c "CREATE DATABASE $database"

            $exitCode = $LASTEXITCODE

            if ($exitCode -ne 0) {
                throw 'Database creation failed. Check CREATE DATABASE permissions.'
            }

            Write-Host "Database '$database' created successfully." -ForegroundColor Green

        }
        else {
            throw "Database '$database' is required."
        }
    }
    else {
        Write-Host "Database '$database' already exists." -ForegroundColor Green
    }

}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

# ============================================================
# 11. SAVE LOCAL CONFIGURATION
# ============================================================

@{
    DB_USERNAME = $dbUser
    GOOGLE_CLIENT_ID = $clientId
} |
ConvertTo-Json |
Set-Content -Path $localConfig -Encoding UTF8

Write-Host 'Local configuration saved.' -ForegroundColor Green

# ============================================================
# 12. INSTALL FRONTEND DEPENDENCIES
# ============================================================

if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {

    Write-Host ''
    Write-Host 'Installing frontend dependencies...' -ForegroundColor Yellow

    Push-Location $frontend

    try {
        & npm.cmd install

        if ($LASTEXITCODE -ne 0) {
            throw 'npm install failed.'
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host 'Frontend dependencies ready.' -ForegroundColor Green

# ============================================================
# 13. CHECK GOOGLE CLIENT ID IN ANGULAR
# ============================================================

$constantsFiles = @(
    Get-ChildItem (Join-Path $frontend 'src') `
        -Filter 'constants.ts' `
        -Recurse `
        -File `
        -ErrorAction SilentlyContinue
)

foreach ($file in $constantsFiles) {

    $content = Get-Content $file.FullName -Raw

    if ($content -match 'GOOGLE_CLIENT_ID' -and
        $content -notmatch [regex]::Escape($clientId)) {

        Write-Warning "Angular Google Client ID may differ: $($file.FullName)"

        Write-Host 'Update GOOGLE_CLIENT_ID in that file with your own Client ID.' -ForegroundColor Yellow
    }
}

# ============================================================
# 14. START BACKEND AND FRONTEND
# ============================================================

Write-Host ''
Write-Host 'Preparing application startup...' -ForegroundColor Cyan

if (Test-Path $mvn) {
    $backendCommand = "& '.\mvnw.cmd' spring-boot:run"
}
else {
    $mvnCommand = Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue

    if (-not $mvnCommand) {
        $mvnCommand = Get-Command 'mvn.exe' -ErrorAction SilentlyContinue
    }

    $backendCommand = "& $(Quote-PS $mvnCommand.Source) spring-boot:run"
}

$backendStart = "Set-Location -LiteralPath $(Quote-PS $backend); $backendCommand"

$frontendStart = "Set-Location -LiteralPath $(Quote-PS $frontend); & 'npx.cmd' ng serve --port 4200 --proxy-config proxy.local.conf.json"

# Use encoded commands to preserve paths containing spaces.
$backendEncoded = [Convert]::ToBase64String(
    [Text.Encoding]::Unicode.GetBytes($backendStart)
)

$frontendEncoded = [Convert]::ToBase64String(
    [Text.Encoding]::Unicode.GetBytes($frontendStart)
)

# Child processes inherit the configured environment variables.

Start-Process powershell.exe -ArgumentList @(
    '-NoExit',
    '-EncodedCommand',
    $backendEncoded
)

Start-Process powershell.exe -ArgumentList @(
    '-NoExit',
    '-EncodedCommand',
    $frontendEncoded
)

# ============================================================
# 15. FINAL STATUS
# ============================================================

Write-Host ''
Write-Host '========================================' -ForegroundColor Green
Write-Host '       PETMATCHCR LAUNCHED' -ForegroundColor Green
Write-Host '========================================' -ForegroundColor Green

Write-Host ''
Write-Host 'Database: petmatchcr'
Write-Host 'PostgreSQL: localhost:5432'
Write-Host 'Frontend: http://localhost:4200'
Write-Host 'Backend:  http://localhost:8080'

Write-Host ''
Write-Host 'Check both terminals for startup errors.' -ForegroundColor Cyan
