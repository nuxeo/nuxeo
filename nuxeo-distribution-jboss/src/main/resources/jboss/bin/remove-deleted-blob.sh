#!/bin/sh -x
# this script remove from the server the blob from deleted document. You need to run this script as root.
# the script will create:
#    - a existing-blob.txt file that list the md5 of blob that exists in Nuxeo
#    - a stored-blob.txt file that list the md5 of blob that are stored inside JBoss
#    - a deleted-blob.txt file that list the md5 of blob that were removed from Nuxeo but still are in JBoss
# It will remove the deleted-blob from JBoss and copy them to $BACKUP_DIR.     
# It assumes that you are using Postgres
# you need to set the following property first
# the path of the jboss server
JBOSS=/opt/nuxeo-dm-5.2.0
# the path where the deleted blob will be moved
BACKUP_DIR=/tmp/nuxeo-delete
[ ! -d $BACKUP_DIR ] && mkdir -p $BACKUP_DIR

NUXEO_DATA_DIR=$JBOSS/server/default/data/NXRuntime/binaries/data/
[ ! -d $NUXEO_DATA_DIR ] &&  echo "Nuxeo data directory not found: $NUXEO_DATA_DIR" && exit 1
HERE=`pwd`
if [ `id -u` != '0' ]; then
  echo "You must acquire root privileges to run this"
  exit 1
fi
# the list of existing blob in nuxeo
su postgres -c "psql -t -c \"select distinct data from content order by data\" -d nuxeo " | cut -d' ' -f2 > existing-blob.txt
# the list of blob inside the server 
find  $NUXEO_DATA_DIR -type f | tr "/" " " | awk '{print $NF}' | sort > stored-blob.txt
# find the file to delete
comm -23 stored-blob.txt existing-blob.txt > deleted-blob.txt
# copy deleted file
cd $NUXEO_DATA_DIR
while read line
  do find . -name $line -exec cp --parents -R -t $BACKUP_DIR/ {} \;
done < $HERE/deleted-blob.txt
# remove deleted file and empty directory
while read line
  do find . -name $line -exec rm {} \;
done < $HERE/deleted-blob.txt
for i in `seq 1 3`; # at worst there are 3 level of directories without file in it
   do find . -type d -empty -exec rmdir {} \;
done
cd $HERE
