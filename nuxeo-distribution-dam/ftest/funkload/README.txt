=============================
DAM funkload scripts
=============================

DAM-XXX description


Requires
------------

Install funkload 1.10.x, visit http://funkload.nuxeo.org/INSTALL.html


Configuration
----------------

Edit the ``Dam.conf`` configuration file and set the [test_dam/import_files]
value to point to a file that contain a listing of the data to import.

You can create such file using a command like::

  find ~/  -size +100k -a \( -name '*.jpg' -o -name '*.gif' -o -name '*.mp3' \
    -o -name '*.ogg'  \) > input-files.txt

Note that you must use full path.


Import scenario
-----------------

The import script upload input_count random file from input_files using
random metadata.

There is a uniq tag starting by FLNX in the description, the location is a
random african country.


Limitation
------------

* There is no way to check if the upload went fine

* During import the title is overriden with the filename


Usage
-------

* Test the import::

 make test-import URL=http://dam.demo.nuxeo.com/nuxeo

* Test the import and check the output in firefox::

 make test-import URL=http://dam.demo.nuxeo.com/nuxeo EXT="-V"

* Run a load bench from 1 to 30 concurrent users during::

   make bench-import-stress URL=http://dam.demo.nuxeo.com/nuxeo

 The report is generated in the report directory.

* Run an longevity import bench to load the database, using 5 concurrent
  thread during 6 cycle of 10 minutes::

   make bench-import-long URL=http://dam.demo.nuxeo.com/nuxeo






