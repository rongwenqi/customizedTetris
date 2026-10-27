param(
    [string]$JavaHome=$env:JAVA_HOME,
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$BuildToolsDir,
    [string]$AndroidJar,
    [string]$OutputApk=(Join-Path $PSScriptRoot 'build\文奇俄罗斯方块.apk')
)
$ErrorActionPreference='Stop'
if (!$BuildToolsDir) { $BuildToolsDir=Join-Path $SdkRoot 'build-tools\35.0.0' }
if (!$AndroidJar) { $AndroidJar=Join-Path $SdkRoot 'platforms\android-35\android.jar' }
$java=Join-Path $JavaHome 'bin\java.exe'
$javac=Join-Path $JavaHome 'bin\javac.exe'
$keytool=Join-Path $JavaHome 'bin\keytool.exe'
$build=Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force $build,(Join-Path $build 'classes'),(Join-Path $build 'testclasses'),(Split-Path $OutputApk) | Out-Null
function Run([string]$exe,[string[]]$Arguments) { & $exe @Arguments; if($LASTEXITCODE -ne 0){throw "Command failed: $exe"} }
$game=Join-Path $PSScriptRoot 'app\src\main\java\cn\fixedblocks\Game.java'
Run $javac @('-encoding','UTF-8','-d',"$build\testclasses",$game,"$PSScriptRoot\tests\GameTest.java")
Run $java @('-cp',"$build\testclasses",'cn.fixedblocks.GameTest')
Run "$BuildToolsDir\aapt2.exe" @('compile','--dir',"$PSScriptRoot\app\src\main\res",'-o',"$build\resources.zip")
Run "$BuildToolsDir\aapt2.exe" @('link','-o',"$build\unsigned.apk",'--manifest',"$PSScriptRoot\app\src\main\AndroidManifest.xml",'-I',$AndroidJar,'--min-sdk-version','26','--target-sdk-version','35',"$build\resources.zip")
Run $javac @('-encoding','UTF-8','-source','8','-target','8','-classpath',$AndroidJar,'-d',"$build\classes",$game,"$PSScriptRoot\app\src\main\java\cn\fixedblocks\MainActivity.java")
Add-Type -AssemblyName System.IO.Compression.FileSystem
if(Test-Path "$build\classes.jar"){Remove-Item -LiteralPath "$build\classes.jar"}
[System.IO.Compression.ZipFile]::CreateFromDirectory("$build\classes","$build\classes.jar")
Run $java @('-cp',"$BuildToolsDir\lib\d8.jar",'com.android.tools.r8.D8','--lib',$AndroidJar,'--min-api','26','--output',$build,"$build\classes.jar")
$apkZip=[System.IO.Compression.ZipFile]::Open("$build\unsigned.apk",[System.IO.Compression.ZipArchiveMode]::Update)
try {[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apkZip,"$build\classes.dex",'classes.dex') | Out-Null} finally {$apkZip.Dispose()}
Run "$BuildToolsDir\zipalign.exe" @('-f','4',"$build\unsigned.apk","$build\aligned.apk")
# A local test key is sufficient for sideloading; use your own release key for distribution.
$key=Join-Path $build 'local-test.p12'
if(!(Test-Path $key)){Run $keytool @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','localtest','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Fixed Blocks Local Test,O=Local,C=CN')}
Run $java @('-jar',"$BuildToolsDir\lib\apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--ks-key-alias','localtest','--out',$OutputApk,"$build\aligned.apk")
Run $java @('-jar',"$BuildToolsDir\lib\apksigner.jar",'verify','--verbose',$OutputApk)
Write-Output "APK ready: $OutputApk"
