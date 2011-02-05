To (re-)generate PDE projects in 'projects' directory:
./gen.sh

To develop:
Open Eclipse
Define the target platform to be used as follows:


Install the nuxeo OSGi launcher plugin for Eclipse from the following update site:
http://osgi.nuxeo.org/repository/sites/

Import the fragments and projects in eclipse (for example under a working set Nuxeo OSGi).
You can have both regular Nuxeo and PDE projects in the same Eclipse workspace. 

To launch Nuxeo as an OSGi application from Eclipse:


Note that the manifests are duplicated between PDE projects and regular Nuxeo projects. 
If you modify them you must synchronize the manifest using:
./sync.sh

