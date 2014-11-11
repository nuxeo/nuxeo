#!/bin/sh -x
# this script remove from the server the blob from deleted document.
# the script will create:
#    - a existing-blob.txt file that list the md5 of blob that exists in Nuxeo
#    - a stored-blob.txt file that list the md5 of blob that are stored inside JBoss
#    - a deleted-blob.txt file that list the md5 of blob that were removed from Nuxeo but still are in JBoss
# This script can take several hours to run depending of the size of your data directory.
# It will remove the deleted-blob from JBoss and copy them to $BACKUP_DIR.
# It assumes that you are using PostgreSQL
# you need to set the following property first

# the path of the JBoss server
JBOSS_HOME=/opt/nuxeo-dm-5.4.2-HF10
# the path where the deleted blob will be moved
BACKUP_DIR=/tmp/nuxeo-delete
# other property you might change
PGPASSWORD=${PGPASSWORD:-myPr3c1ous} # the password for the user of the DB
PGUSERNAME=${PGUSERNAME:-nuxeo}      # the user of the DB
DBNAME=${DBNAME:-nuxeo}              # the name of the DB
PGHOSTNAME=${PGHOSTNAME:-localhost}  # the name of the host
PGPORT=${PGPORT:-5432}               # the port of the DB

# check binaries and directories
mkdir -p $BACKUP_DIR || exit 1
[ -w $BACKUP_DIR ] || exit 1
NUXEO_DATA_DIR=$JBOSS_HOME/server/default/data/NXRuntime/binaries/data/
[ ! -d $NUXEO_DATA_DIR ] &&  echo "Nuxeo data directory not found: $NUXEO_DATA_DIR" && exit 1
psql -V || exit 1

# give the possibility to back out
echo "This script will remove deleted blobs, make sure you have done a FULL BACKUP, press a key to continue, Hit Ctrl-C to abort"
read dummy

# start the processing
HERE=`pwd`
# the list of blob inside the server, computed first. If blob are removed or added, it is still safe
find  $NUXEO_DATA_DIR -type f | tr "/" " " | awk '{print $NF}' | sort > stored-blob.txt
# the list of existing blob in nuxeo
export PGPASSWORD
psql -U $PGUSERNAME -d $DBNAME -h $PGHOSTNAME -p $PGPORT -t -c "select distinct data from content order by data" | cut -d' ' -f2 > existing-blob.txt || exit 1
# find the file to delete
comm -23 stored-blob.txt existing-blob.txt > deleted-blob.txt
# copy deleted file
cd $NUXEO_DATA_DIR
while read line
  do find . -name $line -exec cp --parents -R -t $BACKUP_DIR/ {} \;
done < $HERE/deleted-blob.txt
# remove deleted file and empty directory
while read line
  do find . -name $line -delete
done < $HERE/deleted-blob.txt
for i in `seq 1 3`; # at worst there are 3 level of directories without file in it
   do find . -type d -empty -exec rmdir {} \;
done
