$file = "D:/repos/local-dream/app/src/main/java/io/github/xororz/localdream/ui/screens/ModelRunScreen.kt"
$body = Get-Content $file -Raw

# Split imports and body at the first blank line after last import
$lines = Get-Content $file
$bodyStart = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^import ') { $bodyStart = $i + 1 }
}
$codeBlock = $lines[$bodyStart..$($lines.Count-1)] -join "`n"

# Get all imports
$imports = @()
foreach ($line in $lines) {
    if ($line -match '^import (.+)$') {
        $imports += $matches[1]
    }
}

$unused = @()
foreach ($imp in $imports) {
    $simpleName = ""
    $type = ""
    if ($imp -match ' as (.+)$') {
        $simpleName = $matches[1]
    } else {
        $parts = $imp.Split('.')
        $simpleName = $parts[-1]
    }

    # Determine what kind of import
    if ($imp -match '^[a-z]+\.') { $type = "package" } else { $type = "type" }

    # For simple types, check if they appear in code (word boundary)
    $escaped = [regex]::Escape($simpleName)
    # Check word-boundary, handle generics and nullable
    if ($codeBlock -match "(?<![\w\d]|\.)$escaped(?![\w\d])") {
        # Used
    } else {
        $unused += @{Import=$imp; Name=$simpleName; Type=$type}
    }
}

Write-Host "Unused imports:"
$unused | ForEach-Object { Write-Host "$($_.Import)" }
