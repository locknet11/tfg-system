#!/bin/sh
#
# Agent Installation Script
# Generated for target: ${targetUniqueId}
#
# This script will download and configure the security agent
# for the specified target system.
#

set -e

API_URL="${apiUrl}"
ORGANIZATION_ID="${organizationIdentifier}"
PROJECT_ID="${projectIdentifier}"
TARGET_UNIQUE_ID="${targetUniqueId}"
PREAUTH_CODE="${preauthCode}"

echo "Installing security agent for target: $TARGET_UNIQUE_ID"
echo "API URL: $API_URL"
echo "Organization: $ORGANIZATION_ID"
echo "Project: $PROJECT_ID"
echo "Target: $TARGET_UNIQUE_ID"

exit 0