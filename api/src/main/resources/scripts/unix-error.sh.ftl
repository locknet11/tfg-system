#!/bin/sh

# Error script when setting up the agent
echo "Error: Couldn't install security agent for target: ${targetUniqueId}"
echo "Error reason: ${errorMessage}"

exit 1

