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

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.security.auth.login.LoginContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature.Identity;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ ClientLoginFeature.class, SQLDirectoryFeature.class })
@LocalDeploy({
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-alteration-config.xml" })
@ClientLoginFeature.Opener(TestColCreationSQLDirectory.Opener.class)
@Identity(administrator = true)
public class TestColCreationSQLDirectory {

    protected Session tmpDir1session;

    protected Session tmpDir2session;

    protected void open() {
        tmpDir1session = directoryService.getDirectory("tmpdirectory1").getSession();
        tmpDir2session = directoryService.getDirectory("tmpdirectory2").getSession();
    }

    protected void close() {
        tmpDir1session.close();
        tmpDir2session.close();
    }

    public class Opener implements ClientLoginFeature.Listener {

        @Override
        public void onLogin(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context) {
            open();
        }

        @Override
        public void onLogout(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context) {
            close();
        }

    }

    @Inject
    RuntimeHarness harness;

    @Inject
    DirectoryService directoryService;

    @Test
    public void testColumnCreation() throws Exception {

        String schema1 = "tmpschema1";
        DocumentModel entry = BaseSession.createEntryModel(null, schema1, null,
                null);
        entry.setProperty(schema1, "id", "john");
        entry.setProperty(schema1, "label", "monLabel");

        assertNull(tmpDir1session.getEntry("john"));
        entry = tmpDir1session.createEntry(entry);
        assertEquals("john", entry.getId());
        assertNotNull(tmpDir1session.getEntry("john"));

        // Open a new directory that uses the same table with a different
        // schema.
        // And test if the table has not been re-created, and data are there
        assertNotNull(tmpDir2session.getEntry("john"));

    }

}
