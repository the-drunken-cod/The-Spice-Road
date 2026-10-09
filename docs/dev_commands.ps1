# To register these as global commands:
# 1. Open Powershell, run: echo $PROFILE
# 2. Open or create that file, then paste any of the below functions in it and save it.
# 3. Relaunch any open Powershell terminals to be able to use the commands.


#region pure DataGen & client launch
# Renames the current run/mods/ folder when datagenning, so that no mod installed in the devenv makes
# datagen fail. Then renames the folder back and uses runClient to start the devenv.

# Run NeoForge DataGen without it failing via installed mods, then launch the regular devenv client:
function neo {
    $ModsFolder = 'neoforge/run/mods'
    $ModsFolderRenamed = 'neoforge/run/mods_temp_disabled_by_neo_command__'
    $FolderRenamed = 0

    if (Test-Path -Path $ModsFolderRenamed) {
        if (Test-Path -Path $ModsFolder) {
            Write-Output "Found old mods folder and temp renamed folder. Aborting for manual fix."
            return
        }

        Move-Item -Path $ModsFolderRenamed -Destination $ModsFolder
    }

    if (Test-Path -Path $ModsFolder) {
        Move-Item -Path $ModsFolder -Destination $ModsFolderRenamed
        $FolderRenamed = 1
    }

    ./gradlew :neoforge:runData $args

    $runDataExitCode = $LASTEXITCODE

    if ($FolderRenamed -eq 1) {
        if (Test-Path -Path $ModsFolder) {
            Remove-Item -Path $ModsFolder -Recurse -Force
        }
        Move-Item -Path $ModsFolderRenamed -Destination $ModsFolder -Force
    }

    if ($runDataExitCode -eq 0) {
        ./gradlew :neoforge:runClient $args
    }
}

# Run Fabric DataGen without it failing via installed mods, then launch the regular devenv client:
function fab {
    $ModsFolder = 'fabric/runs/client/mods'
    $ModsFolderRenamed = 'fabric/runs/client/mods_temp_disabled_by_fab_command__'
    $FolderRenamed = 0

    if (Test-Path -Path $ModsFolderRenamed) {
        if (Test-Path -Path $ModsFolder) {
            Write-Output "Found old mods folder and temp renamed folder. Aborting for manual fix."
            return
        }

        Move-Item -Path $ModsFolderRenamed -Destination $ModsFolder
    }

    if (Test-Path -Path $ModsFolder) {
        Move-Item -Path $ModsFolder -Destination $ModsFolderRenamed
        $FolderRenamed = 1
    }

    ./gradlew :fabric:runData $args

    $runDataExitCode = $LASTEXITCODE

    if ($FolderRenamed -eq 1) {
        if (Test-Path -Path $ModsFolder) {
            Remove-Item -Path $ModsFolder -Recurse -Force
        }
        Move-Item -Path $ModsFolderRenamed -Destination $ModsFolder -Force
    }

    if ($runDataExitCode -eq 0) {
        ./gradlew :fabric:runClient $args
    }
}