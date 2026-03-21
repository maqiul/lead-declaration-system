# Wait for devtools to finish restarting the backend
Write-Host "Waiting 5 seconds for backend to be ready..."
Start-Sleep -Seconds 5

$createBody = @{
    shipperCompany = "Test Corp"
    consigneeCompany = "Target Corp"
    totalAmount = 5000
    currency = "USD"
} | ConvertTo-Json

Write-Host "Creating Declaration Form..."
$createResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/declarations" -Method Post -Body $createBody -ContentType "application/json"

Write-Host "Create Response: "
$createResponse | ConvertTo-Json

if ($createResponse.code -eq 200) {
    $formId = $createResponse.data
    Write-Host "Created Form ID: $formId"
    
    Write-Host "Submitting Form..."
    try {
        $submitResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/declarations/$formId/submit" -Method Post
        Write-Host "Submit Response: "
        $submitResponse | ConvertTo-Json
    } catch {
        Write-Host "Submit crashed: $_"
        $errResponse = $_.Exception.Response
        if ($errResponse) {
            $errStream = $errResponse.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errStream)
            $errBody = $reader.ReadToEnd()
            Write-Host "Error Body: $errBody"
        }
    }

    Write-Host "Done Testing."
} else {
    Write-Host "Failed to create."
}
