To generate PDE projects in 'projects' directory:

cd tools/nuxeo-project-generator/
mvn install
./run.sh

To develop:
Open Eclipse
Define the target platform to be used as follows:


Install the nuxeo OSGi launcher plugin for eclipse:


Import the fragments and projects in eclipse (for example under a working set Nuxeo OSGi).
You can habe both regular Nuxeo project and the PDE ones in the same Eclipse workspace. 

To launch Nuxeo as an OSGi application from Eclipse:


Note that the manifests are duplicated between PDE projects and regular Nuxeo projects. 
If you modify them you must synchronize the manifest using:

