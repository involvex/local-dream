$file = 'D:/repos/local-dream/app/src/main/java/io/github/xororz/localdream/ui/screens/ModelRunScreen.kt'
$content = Get-Content $file -Raw

# Get all import lines
$imports = [regex]::Matches($content, '^import ([^\s]+)', [System.Text.RegularExpressions.RegexOptions]::Multiline) | ForEach-Object { $_.Groups[1].Value }

# Find where the last import ends and body starts
$lastImportLine = [regex]::Match($content, '(?m)^import [^\n]+\n(?!import )').Index
if ($lastImportLine -eq -1) { $lastImportLine = [regex]::Match($content, '(?m)^import [^\n]+$').Index }
$bodyOnly = $content.Substring($lastImportLine)

$unused = @()
foreach ($imp in $imports) {
    # Extract the simple name (last segment or after ' as ')
    if ($imp -match ' as (.+)$') {
        $simpleName = $matches[1]
    } else {
        $parts = $imp.Split('.')
        $simpleName = $parts[$parts.Length-1]
    }

    # Skip if it's a lower-case package-like import (e.g., 'android.graphics.Rect as AndroidRect')
    if ($simpleName -match '^[a-z]' -and $imp -notmatch ' as ') { continue }

    # Check if the simple name appears in the body (excluding import lines)
    $escaped = [regex]::Escape($simpleName)
    if ($bodyOnly -match "(?<![\w\d\\.])$escaped(?![\w\d])") {
        # Used
    } else {
        $unused += $imp
    }
}

Write-Host "Unused imports:"
$unused | ForEach-Object { Write-Host $_ }
