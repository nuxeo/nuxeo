#!/bin/sh

runningStatus=$(curl $1/runningstatus)
echo Running status: $runningStatus

runtimeStatus=$(echo $runningStatus | jq -r '.runtimeStatus')
repositoryStatus=$(echo $runningStatus | jq -r '.repositoryStatus')
streamStatus=$(echo $runningStatus | jq -r '.streamStatus')

if [ "$runtimeStatus" = 'ok' -a "$repositoryStatus" = 'ok' -a "$streamStatus" = 'ok' ]; then
  exit 0
else
  exit 1
fi
