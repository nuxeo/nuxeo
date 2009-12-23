In order to run the tests, you may choose different approach.
1. Assemble the nuxeo-webengine-blogs plugin, download a Jboss server containing a nuxeo-distribution, copy the nuxeo-webengine-blogs plugin in the nuxeo-distribution, start the Jboss server, run selenium tests and finally stop the Jboss server.
For all these steps to be executed, you need to run 'ant test-all' from the parent project location.

2. Copy the nuxeo-webengine-blogs plugin in the nuxeo-distribution, start the Jboss server, run selenium tests and finally stop the Jboss server.
For all these steps to be executed, you need to run 'ant test-copy' from the parent project location.

3. Start the Jboss server, run selenium tests and finally stop the Jboss server.
For all these steps to be executed, you need to run 'ant test-jboss' from the parent project location.

4. In case problems appear regarding the nuxeo-platform-userworkspace-core component, then run 'test-all-with-userworkspace'
