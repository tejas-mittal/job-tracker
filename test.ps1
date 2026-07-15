$body = '{"contents":[{"parts":[{"text":"Hello"}]}]}'
$response = Invoke-WebRequest -Uri "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=FAKE_KEY" -Method Post -ContentType "application/json" -Body $body -ErrorAction SilentlyContinue
if ($response -eq $null) {
    Write-Output $error[0].ErrorDetails.Message
} else {
    Write-Output $response.Content
}
