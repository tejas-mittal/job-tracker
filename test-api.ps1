$ErrorActionPreference = "Stop"
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Testing Gmail Job Tracker API (localhost:8080)" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Get a developer token from auth-service
Write-Host "`n1. Generating JWT Token from Auth Service..." -ForegroundColor Yellow
$authPayload = @{
    googleId = "user123"
    email = "test@example.com"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/dev-token" -Body $authPayload -ContentType "application/json"
$token = $authResponse.access_token
Write-Host "SUCCESS! Received Token: $($token.Substring(0, 30))..." -ForegroundColor Green

# 2. Create a Job Application in tracker-service
Write-Host "`n2. Creating a new Job Application..." -ForegroundColor Yellow
$appPayload = @{
    company = "Google"
    role = "Software Engineer"
    notes = "Found on LinkedIn"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

$appResponse = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/applications" -Headers $headers -Body $appPayload
Write-Host "SUCCESS! Created Application with ID: $($appResponse.id)" -ForegroundColor Green
Write-Host "Company: $($appResponse.company) | Status: $($appResponse.status)"

# 3. Get all Job Applications
Write-Host "`n3. Fetching your saved Job Applications..." -ForegroundColor Yellow
$getListResponse = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/applications" -Headers $headers
Write-Host "SUCCESS! Found $($getListResponse.Count) applications." -ForegroundColor Green
$getListResponse | Format-Table id, company, role, status, appliedDate

# 4. Check Analytics Service
Write-Host "`n4. Fetching Job Analytics..." -ForegroundColor Yellow
$analyticsResponse = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/analytics" -Headers $headers
Write-Host "SUCCESS! Analytics data:" -ForegroundColor Green
$analyticsResponse | Format-List

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "All backend microservices are working perfectly!" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
