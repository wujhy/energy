# Encoding Rules

This repository uses UTF-8 without BOM and LF for text files.

Before editing in Windows PowerShell, enable the project shell settings:

```powershell
. .\Use-ProjectUtf8.ps1
```

Repository rules:

- `.editorconfig` sets `charset = utf-8` and `end_of_line = lf`.
- `.gitattributes` normalizes text files to LF and UTF-8.
- Local Git config for this checkout should be:
  - `core.autocrlf=false`
  - `i18n.commitEncoding=utf-8`
  - `i18n.logOutputEncoding=utf-8`
  - `core.quotepath=false`

Do not use these commands to rewrite repository text files in Windows PowerShell:

```powershell
Set-Content
Out-File
>
>>
```

Use `apply_patch` for manual edits. If a scripted write is unavoidable, use .NET explicitly:

```powershell
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText($path, ($text -replace "`r`n", "`n"), $utf8NoBom)
```

After editing Chinese text, verify with `git diff`, not only `Get-Content`; the terminal display path can still be wrong even when file bytes are correct.
PowerShell quoting rule:

- Do not build complex `powershell` commands that embed Java/JSON/regex/text snippets inside double-quoted PowerShell string literals.
- Prefer `apply_patch` for edits. If PowerShell scripting is unavoidable, use single-quoted literals, here-strings (`@' ... '@` / `@" ... "@`), or pass values through variables before calling `.Replace(...)` / regex APIs.
- Avoid backslash-style escaping such as `\"` in Windows PowerShell 5; it is not a string escape there and has repeatedly caused parse errors.
- After any fallback scripted write, run `git diff --check` and inspect the relevant diff before continuing.
