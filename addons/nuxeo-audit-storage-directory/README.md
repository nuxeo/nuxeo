# About Nuxeo Audit Storage

This module enables to specify one or several storages for the Nuxeo Audit logs. 

For instance, the Audit logs can be stored in both Elastic Search and a SQL storage.

# Configuration

You must define in your `nuxeo.conf`:

    nuxeo.stream.audit.enabled = true

# Requirements

This module requires Java 8 and Maven 3.

# Building
 
Get the source code:

    git clone git@github.com:nuxeo/nuxeo-audit-storage-directory.git
    cd nuxeo-audit-storage-directory

Build using Maven:

    mvn clean install

See our [Core Developer Guide](http://doc.nuxeo.com/x/B4BH) for instructions and guidelines.

[TODO] Marketplace package

# Licensing
 
This module is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (http://www.gnu.org/licenses/lgpl-2.1.html).
 
# About Nuxeo
 
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).