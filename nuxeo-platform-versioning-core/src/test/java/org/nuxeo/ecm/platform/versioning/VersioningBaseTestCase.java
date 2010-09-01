/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.api.PropertiesDef;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * Base class for versioning tests.
 */
public abstract class VersioningBaseTestCase extends SQLRepositoryTestCase {

    protected static final String VERSIONING_SCHEMA_NAME
            = DocumentModelUtils.getSchemaName(PropertiesDef.DOC_PROP_MAJOR_VERSION);

    protected VersioningService versioningService;

    protected VersioningBaseTestCase() {
        super(null);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "OSGI-INF/types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "OSGI-INF/versioning-rules-contrib.xml");

        versioningService = ServiceHelper.getVersioningService();
        assertNotNull(versioningService);
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    /**
     * Utility method to check versions on a DocumentModel.
     */
    @SuppressWarnings("boxing")
    protected static void checkVersion(DocumentModel doc, long expectedMajor,
            long expectedMinor) {
        long currentMajor;
        long currentMinor;
        try {
            currentMajor = (Long) doc.getProperty(
                    VERSIONING_SCHEMA_NAME,
                    DocumentModelUtils.getFieldName(PropertiesDef.DOC_PROP_MAJOR_VERSION));
            currentMinor = (Long) doc.getProperty(
                    VERSIONING_SCHEMA_NAME,
                    DocumentModelUtils.getFieldName(PropertiesDef.DOC_PROP_MINOR_VERSION));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        assertEquals(expectedMajor + "." + expectedMinor, //
                currentMajor + "." + currentMinor);
    }

}
