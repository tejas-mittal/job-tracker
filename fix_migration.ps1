$services = @("auth", "tracker", "email", "notification", "analytics")
$targetBase = "monolith-service\src\main\java\com\jobtracker\monolith"

# Clean up previously botched copy
if (Test-Path $targetBase) {
    Remove-Item -Path $targetBase\* -Recurse -Force -ErrorAction SilentlyContinue
}

New-Item -ItemType Directory -Force -Path $targetBase | Out-Null

foreach ($svc in $services) {
    $srcPath = "$svc-service\src\main\java\com\jobtracker\$svc"
    $destPath = "$targetBase\$svc"
    
    if (Test-Path $srcPath) {
        Write-Host "Copying $svc..."
        # Create destination directory first to avoid flattening
        New-Item -ItemType Directory -Force -Path $destPath | Out-Null
        Copy-Item -Path "$srcPath\*" -Destination $destPath -Recurse -Force
        
        # Rewrite package and imports
        $files = Get-ChildItem -Path $destPath -Recurse -Include *.java
        foreach ($file in $files) {
            $content = Get-Content $file.FullName -Raw
            $content = $content -replace "package com\.jobtracker\.$svc", "package com.jobtracker.monolith.$svc"
            $content = $content -replace "import com\.jobtracker\.$svc", "import com.jobtracker.monolith.$svc"
            
            foreach ($other in $services) {
                $content = $content -replace "com\.jobtracker\.$other\.", "com.jobtracker.monolith.$other."
            }
            
            Set-Content -Path $file.FullName -Value $content -NoNewline -Encoding UTF8
        }
    }
}

Write-Host "Migration script completed."
