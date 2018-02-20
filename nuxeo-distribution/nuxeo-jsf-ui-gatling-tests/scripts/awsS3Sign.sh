#!/bin/bash -e
# Used by Gatling test to sign S3 request
configFile=$1
shift
bucket=$1
shift
filename=$1
shift
contentType=$1
shift

# Grab the config values
eval "$(grep -P "^\\w+=.*$" "$configFile")"

# Calculate the signature.
resource="/${bucket}/${filename}"
dateValue=$(date -R)
stringToSign="PUT\\n\\n${contentType}\\n${dateValue}\\n${resource}"
signature=$(echo -en "${stringToSign}" | openssl sha1 -hmac "${aws_secret_access_key}" -binary | base64)

# Output the Date and Authorization headers values
echo "${dateValue}|AWS ${aws_access_key_id}:${signature}"
