This is the integration of Shindig (Opensocial) and Nuxeo

to try it :

# hg clone http://hg.nuxeo.org/opensocial
# cd opensocial
# mvn clean install
# cd nuxeo-distribution-opensocial
# mvn clean install
# cp -r target/nuxeo.ear $JBOSS/server/default/deploy/

Yould should now be able to add Spaces documents
