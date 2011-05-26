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

    public static final String DIRECTORY_NAME = "userDirectory";

    public static final String DEFAULT_DIRECTORY_NAME = DIRECTORY_NAME;

    public static final String DIRECTORY_SUFFIX = "_domain_a";

    public static final String SILLY_DIRECTORY_SUFFIX = "alaclairefontaine";

    public static final String SPECIFIC_DIRECTORY_NAME = DIRECTORY_NAME
            + DIRECTORY_SUFFIX;

    DocumentModel superSpaceWithoutLocalConfiguration;

    DocumentModel superSpaceWithLocalConfiguration;

    DocumentModel superSpaceWithSillyLocalConfiguration;

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
    }

    public void testshouldReturnUserDirectoryWhenNoContextIsGiven()
            throws Exception {

        DirectoryService dirService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

        Directory dir = dirService.getDirectory(DIRECTORY_NAME);
        assertEquals(DEFAULT_DIRECTORY_NAME, dir.getName());

        Session dirSession = dirService.open(DIRECTORY_NAME);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testshouldReturnUserDirectoryWhenContextIsNull()
            throws Exception {

        DirectoryService dirService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

        Directory dir = dirService.getDirectory(DIRECTORY_NAME, null);
        assertEquals(DEFAULT_DIRECTORY_NAME, dir.getName());

        Session dirSession = dirService.open(DIRECTORY_NAME, null);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testshouldReturnUserDirectoryWhenNoLocalConfigurationSet()
            throws Exception {

        DirectoryService dirService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

        Directory dir = dirService.getDirectory(DIRECTORY_NAME,
                superSpaceWithoutLocalConfiguration);
        assertEquals(DEFAULT_DIRECTORY_NAME, dir.getName());

        Session dirSession = dirService.open(DIRECTORY_NAME,
                superSpaceWithoutLocalConfiguration);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testshouldReturnUserDirectoryWithoutSuffixWhenDirectoryContextIsGivenAndDirectoryNotExists()
            throws Exception {

        DirectoryService dirService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

        Directory dir = dirService.getDirectory(DIRECTORY_NAME,
                superSpaceWithSillyLocalConfiguration);
        assertEquals(DEFAULT_DIRECTORY_NAME, dir.getName());

        Session dirSession = dirService.open(DIRECTORY_NAME,
                superSpaceWithSillyLocalConfiguration);
        assertEquals(3, dirSession.getEntries().size());

    }

    public void testshouldReturnUserDirectoryWithSuffixWhenDirectoryContextIsGiven()
            throws Exception {

        DirectoryService dirService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

        Directory dir = dirService.getDirectory(DIRECTORY_NAME,
                superSpaceWithLocalConfiguration);
        assertEquals(SPECIFIC_DIRECTORY_NAME, dir.getName());

        Session dirSession = dirService.open(DIRECTORY_NAME,
                superSpaceWithLocalConfiguration);
        assertEquals(1, dirSession.getEntries().size());

    }

    protected void initRepository() throws ClientException {

        openSession();

        superSpaceWithoutLocalConfiguration = session.createDocumentModel("Workspace");
        superSpaceWithoutLocalConfiguration.setPathInfo("/", "default-domain");
        session.createDocument(superSpaceWithoutLocalConfiguration);

        superSpaceWithLocalConfiguration = session.createDocumentModel("Workspace");
        superSpaceWithLocalConfiguration.setPathInfo("/", "domain-with-conf");
        session.createDocument(superSpaceWithLocalConfiguration);

        setDirectorySuffix(superSpaceWithLocalConfiguration, DIRECTORY_SUFFIX);

        superSpaceWithSillyLocalConfiguration = session.createDocumentModel("Workspace");
        superSpaceWithSillyLocalConfiguration.setPathInfo("/", "domain-with-conf2");
        session.createDocument(superSpaceWithSillyLocalConfiguration);

        setDirectorySuffix(superSpaceWithSillyLocalConfiguration, SILLY_DIRECTORY_SUFFIX);

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
