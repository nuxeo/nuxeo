/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql.localconfig;

import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class TestLocalConfigurationForSQLDirectory extends
        SQLRepositoryTestCase {

    DocumentModel workspace;

    DirectoryService dirService = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");

        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        // override user schema with intField & dateField
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-schema-override.xml");

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-bundle.xml");

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-for-local-configuration-bundle.xml");

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-types-with-directory-local-configuration.xml");

        initRepository();
        dirService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

    }

    public void testShouldReturnUserDirectoryWhenNoContextIsGiven()
            throws Exception {

        Directory dir = dirService.getDirectory("userDirectory");
        assertEquals("userDirectory", dir.getName());

        Session dirSession = dirService.open("userDirectory");
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testShouldReturnUserDirectoryWhenContextIsNull()
            throws Exception {

        Directory dir = dirService.getDirectory("userDirectory", null);
        assertEquals("userDirectory", dir.getName());

        Session dirSession = dirService.open("userDirectory", null);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testShouldReturnUserDirectoryWhenNoLocalConfigurationSet()
            throws Exception {

        Directory dir = dirService.getDirectory("userDirectory", workspace);
        assertEquals("userDirectory", dir.getName());

        Session dirSession = dirService.open("userDirectory", workspace);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testShouldReturnUserDirectoryWhenLocalConfigurationSetIsAnEmptyString()
            throws Exception {

        setDirectorySuffix(workspace, "          ");

        Directory dir = dirService.getDirectory("userDirectory", workspace);
        assertEquals("userDirectory", dir.getName());
        // even id the userDirectory_ exists, we return the userDirectory
        // directory

        Session dirSession = dirService.open("userDirectory", workspace);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testShouldReturnUserDirectoryWithSuffixWhenDirectoryContextIsGiven()
            throws Exception {

        setDirectorySuffix(workspace, "domain_a");

        Directory dir = dirService.getDirectory("userDirectory", workspace);
        assertEquals("userDirectory_domain_a", dir.getName());

        Session dirSession = dirService.open("userDirectory", workspace);
        assertEquals(1, dirSession.getEntries().size());

    }

    protected void initRepository() throws ClientException {

        openSession();

        workspace = session.createDocumentModel("Workspace");
        workspace.setPathInfo("/", "default-domain");
        session.createDocument(workspace);
        session.save();

    }

    protected void setDirectorySuffix(DocumentModel doc, String directorySuffix)
            throws ClientException {
        doc.setPropertyValue(DIRECTORY_CONFIGURATION_FIELD, directorySuffix);
        session.saveDocument(doc);
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();

        super.tearDown();

    }

}
