
For some unknown reason, this project can't be build using the sar dependency.
=> compilation failed

For now, the only solution to build is to depend on nuxeo-ecm-login jar
as the dependency on sar does not work.

So, you will need to add the missing jar into you local repository.

Go into .m2/repository/org/nuxeo/ecm/platform/nuxeo-platform-login/5.1-SNAPSHOT
and copy sar file to a jar file with the same name.
