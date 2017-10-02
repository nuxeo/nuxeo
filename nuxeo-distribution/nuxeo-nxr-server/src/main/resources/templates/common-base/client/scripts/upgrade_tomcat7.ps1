#Requires -Version 2.0
##
## (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##
## Contributors:
##     Frantz Fischer
##
## PowerShell script that upgrades Tomcat 7 in Nuxeo (for 6.0, LTS 2015, LTS 2016, LTS 2017)
##

Set-Variable TomcatArchiveUrl -option Constant -value  "http://archive.apache.org/dist/tomcat/tomcat-7"
Set-Variable TomcatLatestUrl -option Constant -value "http://www.apache.org/dist/tomcat/tomcat-7"
Set-Variable TomcatFallbackVersion -option Constant -value "7.0.75"

$NuxeoHome=""
$TomcatTarget=""

function Display-Usage() {
  Write-Output "Usage:"
  Write-Output "`tupgrade_tomcat7.ps1 NUXEO_HOME [TOMCAT_TARGET_VERSION]"
  Write-Output ""
  Write-Output "Note: If no target version is specified the latest one will be retrieved from the Tomcat site"
  Write-Output ""
  Write-Output "Examples:"
  Write-Output "`tupgrade_tomcat7.ps1 C:\path\to\nuxeo-cap-7.10-tomcat"
  Write-Output "`tupgrade_tomcat7.ps1 C:\path\to\nuxeo-cap-7.10-tomcat $TomcatFallbackVersion"
  Write-Output ""
}

function Download-File($prefix, $filename) {
  Write-Output "`tDownloading $prefix/$filename..."
  Invoke-WebRequest -Uri "$prefix/$filename" -OutFile "$filename"
  if (-not $?) {
    Write-Output "ERROR: could not download the file!"
    exit 1
  }
}

function Verify-Checksum($File, $Algorithm) {
  foreach ($line in (Get-Content $File)) {
    $fields = $line -split '\s+'
    $hash = $fields[0].Trim().ToUpper()
    $filename = $fields[1].Trim()
    if($filename.StartsWith("*")){
	  $filename = $filename.Substring(1).Trim()
    }

    $computedHash = (Get-FileHash -Algorithm $Algorithm $filename).Hash.ToUpper()
    if($hash.Equals($computedHash)){
      Write-Host "`tChecking [$Algorithm] for $filename => OK"
    } else {
      Write-Host "`tChecking [$Algorithm] for $filename => Failed"
    }
  }
}

function Process-Checksums($File) {
  Verify-Checksum -Algorithm MD5 -File $File".md5"
  Verify-Checksum -Algorithm SHA1 -File $File".sha1"
}

if ($args.Count -lt 1 -or $args.Count -gt 2) {
  Display-Usage
  exit 1
}

# does the Nuxeo location seem valid?
if (-Not (Test-Path($($args[0]) + "\templates\nuxeo.defaults"))) {
  Write-Output "ERROR: Cannot find nuxeo.defaults file. Please check the Nuxeo location."
  Write-Output ""
  Display-Usage
  exit 1
}
$NuxeoHome=$args[0]

if ($args.Count -eq 1) {
  Write-Output "Trying to detect the latest available Tomcat version...`n"
  # autodetects latest version
  $TomcatTarget=(Invoke-WebRequest -UseBasicParsing -Uri "$TomcatLatestUrl").Links.href | Where-Object {$_ -like "v7.0.[0-9][0-9]/"}
  if ($TomcatTarget -eq $null -or ($TomcatTarget -is [String] -and $TomcatTarget -eq [String]::Empty)) {
    Write-Output "Failed to detect the Tomcat version. Falling back to $TomcatFallbackVersion...`n"
    $TomcatTarget=$TomcatFallbackVersion # fallback to 7.0.75 if autodetection failed
  } else {
    $TomcatTarget=$TomcatTarget.trim('/').trim('v')
  }
  # check the TOMCAT version exists
  $VersionFound=(Invoke-WebRequest -UseBasicParsing -Uri "$TomcatArchiveUrl").Links.href | Where-Object {$_ -like "v$TomcatTarget/"}
  if ($TomcatTarget -eq $null -or ($TomcatTarget -is [String] -and $TomcatTarget -eq [String]::Empty)) {
    Write-Output "ERROR: Cannot find Tomcat version $TomcatTarget`n"
    exit 1
  }
} else {
  $TomcatTarget=$args[1]
}

# setting temporary working directory
$CurrentLocation = $(Get-Location)
Set-Location $env:temp
$TempWorkingDir = [System.Guid]::NewGuid().ToString()
New-Item -Type Directory -Name $TempWorkingDir | Out-Null
Set-Location $TempWorkingDir
if (-not $?) {
  Write-Output "ERROR: could not set the temporary directory!"
  exit 1
}

$TomcatSource=(java -cp "$NuxeoHome\lib\catalina.jar" org.apache.catalina.util.ServerInfo) | Where-Object {$_ -like "Server number*"}
$TomcatSource=($TomcatSource -split '\s+')[2].trim(".0")
$TomcatNuxeoDefault=(sls tomcat.version "$NuxeoHome\templates\nuxeo.defaults" -ca | select -exp line).split("=", [System.StringSplitOptions]::RemoveEmptyEntries)[1]

Write-Output "NUXEO_HOME is $NuxeoHome"
Write-Output "TEMPORARY WORK FOLDER is $env:temp\$TempWorkingDir"
Write-Output "TOMCAT source version (from libs) is $TomcatSource"
Write-Output "TOMCAT source version (from nuxeo.defaults) is $TomcatNuxeoDefault"

if ($TomcatSource.equals($TomcatNuxeoDefault)) {
  Write-Output "TOMCAT source versions match!"
} else {
  Write-Output "ERROR: TOMCAT source versions don't match!"
  exit 1
}
Write-Output "TOMCAT target version is $TomcatTarget"

Write-Output "`nRetrieving files..."
$DownloadPrefix="$TomcatArchiveUrl/v$TomcatTarget/bin"
Download-File "$DownloadPrefix" "apache-tomcat-$TomcatTarget.zip"
Download-File "$DownloadPrefix" "apache-tomcat-$TomcatTarget.zip.md5"
Download-File "$DownloadPrefix" "apache-tomcat-$TomcatTarget.zip.sha1"
$DownloadPrefix+="/extras"
Download-File "$DownloadPrefix" "tomcat-juli-adapters.jar"
Download-File "$DownloadPrefix" "tomcat-juli-adapters.jar.md5"
Download-File "$DownloadPrefix" "tomcat-juli-adapters.jar.sha1"
Download-File "$DownloadPrefix" "tomcat-juli.jar"
Download-File "$DownloadPrefix" "tomcat-juli.jar.md5"
Download-File "$DownloadPrefix" "tomcat-juli.jar.sha1"

Write-Output "`nChecking archives..."
Process-Checksums "apache-tomcat-$TomcatTarget.zip"
Process-Checksums "tomcat-juli-adapters.jar"
Process-Checksums "tomcat-juli.jar"

Write-Output "`nPatching Nuxeo..."
# upgrading files from core distribution
Write-Output "`tUncompressing apache archive..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$(Get-Location)\apache-tomcat-$TomcatTarget.zip", "$(Get-Location)")

Write-Output "`tCopying apache files..."
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\lib" -Destination "$NuxeoHome" -Recurse -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\bin\*.jar" -Destination "$NuxeoHome\bin" -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\bin\catalina-tasks.xml" -Destination "$NuxeoHome\bin" -Force

Remove-Item -Path "$NuxeoHome\nxserver\lib\tomcat-jdbc-$TomcatSource.jar" -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\lib\tomcat-jdbc.jar" -Destination "$NuxeoHome\nxserver\lib\tomcat-jdbc-$TomcatTarget.jar" -Force

Remove-Item -Path "$NuxeoHome\nxserver\lib\tomcat-juli-$TomcatSource.jar" -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\bin\tomcat-juli.jar" -Destination "$NuxeoHome\nxserver\lib\tomcat-juli-$TomcatTarget.jar" -Force

# upgrading files from extras
Write-Output "`tCopying apache extras files..."
Copy-Item "$(Get-Location)\tomcat-juli.jar" -Destination "$NuxeoHome\bin" -Force
Copy-Item "$(Get-Location)\tomcat-juli-adapters.jar" -Destination "$NuxeoHome\lib" -Force

# release files
Write-Output "`tCopying apache release files..."
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\RELEASE-NOTES" -Destination "$NuxeoHome\doc-tomcat" -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\LICENSE" -Destination "$NuxeoHome\doc-tomcat" -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\NOTICE" -Destination "$NuxeoHome\doc-tomcat" -Force
Copy-Item "$(Get-Location)\apache-tomcat-$TomcatTarget\RUNNING.txt" -Destination "$NuxeoHome\doc-tomcat" -Force

# nuxeo version bump
Write-Output "`tUpdating Tomcat version in nuxeo.defaults..."
cat "$NuxeoHome\templates\nuxeo.defaults" | %{$_ -replace "^tomcat.version=$TomcatNuxeoDefault$","tomcat.version=$TomcatTarget"} | Out-File -encoding ascii -filepath "$env:temp\$TempWorkingDir\nuxeo.defaults"
Copy-Item "$env:temp\$TempWorkingDir\nuxeo.defaults" -Destination "$NuxeoHome\templates" -Force

# temporary working directory cleanup
Set-Location $CurrentLocation
Remove-Item -Path "$env:temp\$TempWorkingDir" -Recurse -Force

exit 0
