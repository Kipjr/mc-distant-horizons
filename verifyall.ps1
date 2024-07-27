# Usage: .\verifyall.ps1 [forge|fabric|whatever to put before ":classes"]

param (
    [string]$prefix
)

# Clear the screen
Clear-Host

# Define an array to hold completed builds
$completedBuilds = @()

# Get all version properties files
$versionFiles = Get-ChildItem -Path "./versionProperties/" -Filter "*.properties"

foreach ($versionFile in $versionFiles) {
    $version = [System.IO.Path]::GetFileNameWithoutExtension($versionFile.Name)

    # Initialize result variable
    $result = ""

    # Run the gradle command
    $gradleCommand = ".\gradlew $($prefix)classes -PmcVer=$version"
    $process = Start-Process -FilePath "cmd.exe" -ArgumentList "/c $gradleCommand" -NoNewWindow -PassThru -Wait

    if ($process.ExitCode -eq 0) {
        $result += "`e[1;32m"
    } else {
        $result += "`e[1;31m"
    }
    
    $result += $version
    $result += "`e[0m"

    # Print the result with formatting
    $versionLength = $version.Length
    $topChars = ("^" * $versionLength)
    $bottomChars = ("=" * $versionLength)

    Write-Host "# $topChars"
    Write-Host "# $version"
    Write-Host "# $bottomChars"
    Write-Host "`e[0m"

    # Add result to completed builds
    $completedBuilds += $result
}

# Run clean and classes gradle tasks
Start-Process -FilePath "cmd.exe" -ArgumentList "/c .\gradlew clean" -NoNewWindow -Wait
Start-Process -FilePath "cmd.exe" -ArgumentList "/c .\gradlew classes" -NoNewWindow -Wait

# Print build results
Write-Host
Write-Host "`e[1mBuild results:`e[0m"
Write-Host ($completedBuilds -join " ")