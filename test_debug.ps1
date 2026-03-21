$body = @{
    username = "admin"
    password = "admin"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/login" -Method Post -Body $body -ContentType "application/json"
if ($loginResponse.code -eq 200) {
    $tokenValue = $loginResponse.data.tokenValue
    $tokenName = $loginResponse.data.tokenName
    $headers = @{
        $tokenName = $tokenValue
    }
    
    Write-Host "Fetching debug info..."
    $debugInfo = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/declarations/debug-info" -Method Get -Headers $headers
    
    Write-Host "=== FLOWABLE TASKS ==="
    $debugInfo.data.flowable_tasks | ConvertTo-Json -Depth 5
    
    Write-Host "=== FLOWABLE HISTORY ==="
    $debugInfo.data.flowable_history | ConvertTo-Json -Depth 5
} else {
    Write-Host "Login Failed"
}
