$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
if(!(Test-Path 'data/processes.json')){Write-Output '没有记录到本项目启动的进程。';exit}
$records=@(Get-Content 'data/processes.json' -Raw|ConvertFrom-Json)
[array]::Reverse($records)
foreach($r in $records){
 if($null -eq $r.pid){continue}
 $p=Get-Process -Id $r.pid -ErrorAction SilentlyContinue
 if($p -and ([DateTime]$r.started).ToUniversalTime().Subtract($p.StartTime.ToUniversalTime()).Duration().TotalSeconds -lt 2){
  if($r.name -eq 'mysql'){
   $config=Get-Content 'config.local.json' -Raw|ConvertFrom-Json
   if(-not [IO.Path]::IsPathRooted($config.RUNTIME)){$config.RUNTIME=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot $config.RUNTIME))}
   $env:MYSQL_PWD=$config.ROOT_PASSWORD
   & "$($config.RUNTIME)/mysql/mysql-8.4.0-winx64/bin/mysqladmin.exe" --host=127.0.0.1 --port=13306 -u root shutdown
   Remove-Item Env:MYSQL_PWD
  }else{& taskkill.exe /PID $r.pid /F | Out-Null}
 }
}
Remove-Item -LiteralPath 'data/processes.json'
Write-Output '本项目服务已停止，资料与计划已保留。'
