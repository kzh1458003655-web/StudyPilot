param([switch]$NoBrowser)
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
Set-Location -LiteralPath $projectRoot
$configPath=Join-Path $projectRoot 'config.local.json'
if(!(Test-Path -LiteralPath $configPath)){throw '缺少config.local.json，请先阅读README并配置运行目录。'}
$config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$runtime=$config.RUNTIME
if(-not [IO.Path]::IsPathRooted($runtime)){$runtime=[IO.Path]::GetFullPath((Join-Path $projectRoot $runtime))}
$env:STUDY_DB_PASSWORD=$config.STUDY_DB_PASSWORD
if(!$config.MODEL_KEY){$config|Add-Member MODEL_KEY ([guid]::NewGuid().ToString('N'));$config|ConvertTo-Json|Set-Content -LiteralPath $configPath -Encoding utf8}
$env:STUDY_MODEL_KEY=$config.MODEL_KEY
New-Item -ItemType Directory -Force -Path 'logs','app','data' | Out-Null
$started=@()
function Test-Port($Port){try{$c=[Net.Sockets.TcpClient]::new();$ok=$c.ConnectAsync('127.0.0.1',$Port).Wait(400);$c.Dispose();return $ok}catch{return $false}}
function Start-Owned($Name,$Exe,$Arguments){
 $p=Start-Process -FilePath $Exe -ArgumentList $Arguments -WorkingDirectory $projectRoot -RedirectStandardOutput "$projectRoot/logs/$Name.out.log" -RedirectStandardError "$projectRoot/logs/$Name.err.log" -WindowStyle Hidden -PassThru
 $script:started+=@{name=$Name;pid=$p.Id;path=$Exe;started=$p.StartTime.ToUniversalTime().ToString('o')}
}
function Wait-Url($Url,$Seconds){
 $until=[DateTime]::UtcNow.AddSeconds($Seconds)
 while([DateTime]::UtcNow -lt $until){try{Invoke-RestMethod $Url -TimeoutSec 3 | Out-Null;return}catch{Start-Sleep -Milliseconds 600}}
 throw "服务未能就绪：$Url。请查看logs目录。"
}
if(!(Test-Port 13306)){
 $mysql="$runtime/mysql/mysql-8.4.0-winx64"
 Start-Owned 'mysql' "$mysql/bin/mysqld.exe" @('--no-defaults',"--basedir=$mysql",('--datadir="'+$projectRoot+'\data\mysql"'),'--port=13306','--bind-address=127.0.0.1','--mysqlx=0','--console')
}
if(!(Test-Port 18082)){
 Start-Owned 'model' "$runtime/llama/llama-server.exe" @('-m',('"'+$runtime+'/Qwen3.5-4B-Q4_K_M.gguf"'),'--host','127.0.0.1','--port','18082','-ngl','99','-c','8192','-np','1','--jinja','--reasoning-budget','0','--api-key',$config.MODEL_KEY)
}
Wait-Url 'http://127.0.0.1:18082/health' 120
if(!(Test-Port 18081)){
 Copy-Item -LiteralPath 'ai/bin/study-ai.exe' -Destination 'app/study-ai.exe' -Force
 Start-Owned 'ai' "$projectRoot/app/study-ai.exe" @('data/index')
}
Wait-Url 'http://127.0.0.1:18081/health' 30
if(!(Test-Port 18080)){
 Copy-Item -LiteralPath 'backend/target/studypilot-1.0.0.jar' -Destination 'app/studypilot.jar' -Force
 $java=if($config.JAVA){if([IO.Path]::IsPathRooted($config.JAVA)){$config.JAVA}else{[IO.Path]::GetFullPath((Join-Path $projectRoot $config.JAVA))}}else{(Get-Command java -ErrorAction Stop).Source}
 Start-Owned 'backend' $java @('-jar',('"'+$projectRoot+'\app\studypilot.jar"'))
}
Wait-Url 'http://127.0.0.1:18080/api/health' 90
# 文件只记录本次实际启动的进程，避免旧 PID 累积后误停其他程序。
@($started)|ConvertTo-Json -Depth 3|Set-Content 'data/processes.json' -Encoding utf8
Write-Output '知序已启动：http://127.0.0.1:18080'
if(!$NoBrowser){Start-Process 'http://127.0.0.1:18080'}
