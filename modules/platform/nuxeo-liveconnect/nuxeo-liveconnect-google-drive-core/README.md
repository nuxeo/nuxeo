# About Nuxeo Google Client

This module enables the use of remote Google Drive files in Nuxeo.

When creating or editing a file, a new picker allows you to select a file in your Google Drive.

This is a work in progress, therefore it is not yet supported by Nuxeo.

# Configuration

You must define in your `nuxeo.conf` a client id key:

    nuxeo.google.clientid = <YOUR-CLIENT-ID>

MORE INFO TBD

# Requirements

This module requires Java 8 and Maven 3.

# Building
 
Get the source code:

    git clone git@github.com:nuxeo/nuxeo-google-client.git
    cd nuxeo-google-client

Build using Maven:

    mvn clean install

See our [Core Developer Guide](http://doc.nuxeo.com/x/B4BH) for instructions and guidelines.

TODO Marketplace package

# Licensing
 
This module is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (http://www.gnu.org/licenses/lgpl-2.1.html).
 
# About Nuxeo
 
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).
