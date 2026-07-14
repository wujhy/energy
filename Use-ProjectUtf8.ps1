# Project-local UTF-8 shell bootstrap.
# Run from the repository root with:
#   . .\Use-ProjectUtf8.ps1
# The leading dot is intentional: it keeps these settings in the current session.

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$env:LANG = 'C.UTF-8'
$env:LC_ALL = 'C.UTF-8'
$env:PYTHONUTF8 = '1'
$env:PYTHONIOENCODING = 'utf-8'
$env:JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS + ' -Dfile.encoding=UTF-8') -replace '^\s+', '').Trim()

# Make the active console use UTF-8 when chcp is available.
if (Get-Command chcp.com -ErrorAction SilentlyContinue) {
    chcp.com 65001 | Out-Null
}

# Keep Git display and line-ending behavior aligned with repository rules.
git config core.autocrlf false | Out-Null
git config i18n.commitEncoding utf-8 | Out-Null
git config i18n.logOutputEncoding utf-8 | Out-Null
git config core.quotepath false | Out-Null

Write-Host 'Project UTF-8 session enabled. Avoid Set-Content/Out-File/redirection for repository text writes; use apply_patch or .NET UTF8Encoding(false).'
