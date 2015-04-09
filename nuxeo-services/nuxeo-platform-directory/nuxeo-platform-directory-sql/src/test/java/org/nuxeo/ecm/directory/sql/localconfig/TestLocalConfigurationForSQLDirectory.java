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

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.directory.types.contrib", //
})
// override user schema with intField & dateField
@LocalDeploy({ "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-bundle.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-for-local-configuration-bundle.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-types-with-directory-local-configuration.xml" })
public class TestLocalConfigurationForSQLDirectory {

    @Inject
    protected DirectoryService dirService;

    @Inject
    protected CoreSession session;

    DocumentModel workspace;

    @Before
    public void setUp() throws Exception {
        initRepository();
    }

    @Test
    public void testShouldReturnUserDirectoryWhenNoContextIsGiven() throws Exception {

        Directory dir = dirService.getDirectory("userDirectory");
        assertEquals("userDirectory", dir.getName());

        Session dirSession = dirService.open("userDirectory");
        assertEquals(3, dirSession.getEntries().size());

    }

    @Test
    public void testShouldReturnUserDirectoryWhenContextIsNull() throws Exception {

        Directory dir = dirService.getDirectory("userDirectory", null);
        assertEquals("userDirectory", dir.getName());

        Session dirSession = dirService.open("userDirectory", null);
        assertEquals(3, dirSession.getEntries().size());

    }

    @Test
    public void testShouldReturnUserDirectoryWhenNoLocalConfigurationSet() throws Exception {

        Directory dir = dirService.getDirectory("userDirectory", workspace);
        assertEquals("userDirectory", dir.getName());

        Session dirSession = dirService.open("userDirectory", workspace);
        assertEquals(3, dirSession.getEntries().size());

    }

    @Test
    public void testShouldReturnUserDirectoryWhenLocalConfigurationSetIsAnEmptyString() throws Exception {

        setDirectorySuffix(workspace, "          ");

        Directory dir = dirService.getDirectory("userDirectory", workspace);
        assertEquals("userDirectory", dir.getName());
        // even id the userDirectory_ exists, we return the userDirectory
        // directory

        Session dirSession = dirService.open("userDirectory", workspace);
        assertEquals(3, dirSession.getEntries().size());

    }

    @Test
    public void testShouldReturnUserDirectoryWithSuffixWhenDirectoryContextIsGiven() throws Exception {

        setDirectorySuffix(workspace, "domain_a");

        Directory dir = dirService.getDirectory("userDirectory", workspace);
        assertEquals("userDirectory_domain_a", dir.getName());

        Session dirSession = dirService.open("userDirectory", workspace);
        assertEquals(1, dirSession.getEntries().size());

    }

    protected void initRepository() throws ClientException {
        workspace = session.createDocumentModel("Workspace");
        workspace.setPathInfo("/", "default-domain");
        session.createDocument(workspace);
        session.save();
    }

    protected void setDirectorySuffix(DocumentModel doc, String directorySuffix) throws ClientException {
        doc.setPropertyValue(DIRECTORY_CONFIGURATION_FIELD, directorySuffix);
        session.saveDocument(doc);
        session.save();
    }

}
