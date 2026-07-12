$services = @("auth", "tracker", "email", "notification", "analytics")

$targetBase = "monolith-service\src\main\java\com\jobtracker\monolith"
New-Item -ItemType Directory -Force -Path $targetBase | Out-Null

foreach ($svc in $services) {
    $srcPath = "$svc-service\src\main\java\com\jobtracker\$svc"
    $destPath = "$targetBase\$svc"
    
    if (Test-Path $srcPath) {
        Write-Host "Copying $svc..."
        Copy-Item -Path "$srcPath\*" -Destination $destPath -Recurse -Force
        
        # Rewrite package and imports
        $files = Get-ChildItem -Path $destPath -Recurse -Include *.java
        foreach ($file in $files) {
            $content = Get-Content $file.FullName
            $content = $content -replace "package com.jobtracker.$svc", "package com.jobtracker.monolith.$svc"
            $content = $content -replace "import com.jobtracker.$svc", "import com.jobtracker.monolith.$svc"
            
            # Note: Cross-service imports (e.g. tracker importing auth) are minimal because they were separate microservices.
            # But if there were any, we could replace them globally:
            foreach ($other in $services) {
                $content = $content -replace "com.jobtracker.$other.", "com.jobtracker.monolith.$other."
            }
            
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8
        }
    }
}

Write-Host "Migration script completed."
