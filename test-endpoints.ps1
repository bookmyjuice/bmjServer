# API Testing Script
$baseUrl = "http://127.0.0.1:8080"

Write-Host "`n=== BookMyJuice API Testing ===`n" -ForegroundColor Cyan

# Test 1: Login
Write-Host "[1] Testing Login..." -ForegroundColor Yellow
$loginBody = @{
    username = "support@bookmyjuice.co.in"
    password = "rADHASOAMI@0"
} | ConvertTo-Json

try {
    $login = Invoke-RestMethod -Uri "$baseUrl/api/auth/signin" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $login.accessToken
    Write-Host "SUCCESS: Logged in" -ForegroundColor Green
    
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Test 2: Get Subscription Plans
Write-Host "`n[2] Testing Get Subscription Plans..." -ForegroundColor Yellow
try {
    $plans = Invoke-RestMethod -Uri "$baseUrl/api/subscriptions/pricing/plans" -Method Get
    Write-Host "SUCCESS: Found $($plans.data.Count) plans" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Get My Subscriptions
Write-Host "`n[3] Testing Get My Subscriptions..." -ForegroundColor Yellow
try {
    $subs = Invoke-RestMethod -Uri "$baseUrl/api/subscriptions" -Method Get -Headers $headers
    Write-Host "SUCCESS: Found $($subs.data.Count) subscriptions" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Get Pricing Page
Write-Host "`n[4] Testing Get Pricing Page..." -ForegroundColor Yellow
try {
    $pricing = Invoke-RestMethod -Uri "$baseUrl/api/subscriptions/pricing-page" -Method Get -Headers $headers
    Write-Host "SUCCESS: $($pricing.url)" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Get Orders
Write-Host "`n[5] Testing Get Orders..." -ForegroundColor Yellow
try {
    $orders = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Get -Headers $headers
    Write-Host "SUCCESS: Found $($orders.data.Count) orders" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Get Invoices
Write-Host "`n[6] Testing Get Invoices..." -ForegroundColor Yellow
try {
    $invoices = Invoke-RestMethod -Uri "$baseUrl/api/invoices" -Method Get -Headers $headers
    Write-Host "SUCCESS: Found $($invoices.data.Count) invoices" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 7: Get Items
Write-Host "`n[7] Testing Get Items..." -ForegroundColor Yellow
try {
    $items = Invoke-RestMethod -Uri "$baseUrl/api/items/all" -Method Get -Headers $headers
    Write-Host "SUCCESS: Found $($items.Count) items" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Testing Complete ===`n" -ForegroundColor Cyan
