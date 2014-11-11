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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.api.PropertiesDef;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * Base class for versioning tests.
 *
 * @author DM
 */
public abstract class VersioningBaseTestCase extends RepositoryTestCase {

    protected static final String VERSIONING_SCHEMA_NAME = DocumentModelUtils.getSchemaName(PropertiesDef.DOC_PROP_MAJOR_VERSION);

    private static final Log log = LogFactory.getLog(VersioningBaseTestCase.class);

    private Repository repository;

    protected Session session;

    protected Document root;

    protected CoreSession coreSession;

    @Override
    public Repository getRepository() throws Exception {
        if (repository == null) {
            // the repository should be deployed the last
            // after any other bundle that is deploying doctypes
            deployBundle("org.nuxeo.ecm.core.event");
            deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                    "DemoRepository.xml");
            repository = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                    "demo");
        }
        return repository;
    }

    @Override
    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }

    protected static Map<String, Serializable> getContext() {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        return context;
    }

    @Override
    protected Session getSession() throws Exception {
        return getRepository().getSession(getContext());
    }

    protected void openCoreSession() throws ClientException {
        coreSession = CoreInstance.getInstance().open("demo", getContext());
        assertNotNull(coreSession);
    }

    @Override
    public void setUp() throws Exception {
        log.info("Initializing NX Core for local tests");
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "DocumentAdapterService.xml");
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "LifeCycleService.xml");
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "LifeCycleCoreExtensions-versioningtest.xml");

        // Versioning specific extensions
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "nxversioning-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "test-nxversioning-contrib-bundle.xml");

        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "VersionDocTypes.xml");

        // open session
        session = getSession();
        root = session.getRootDocument();
        // open core session
        openCoreSession();
    }

    @Override
    protected void tearDown() throws Exception {
        log.info("Shutting down NX Core for local tests");
        root.remove();
        session.save();
        session.close();
        session = null;
        root = null;
        releaseRepository();
        super.tearDown();
    }

    protected static VersioningService getVersioningService() {
        final VersioningService service = ServiceHelper.getVersioningService();
        assertNotNull("VersioningService not available", service);
        return service;
    }

    /**
     * Utility method to check versions on a DocumentModel.
     *
     * @param doc
     * @param expectedMajor
     * @param expectedMinor
     */
    protected static void checkVersion(DocumentModel doc, Long expectedMajor,
            Long expectedMinor) {

        Long currentMajor;
        try {
            currentMajor = (Long) doc.getProperty(
                    VERSIONING_SCHEMA_NAME,
                    DocumentModelUtils.getFieldName(PropertiesDef.DOC_PROP_MAJOR_VERSION));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        Long currentMinor;
        try {
            currentMinor = (Long) doc.getProperty(
                    VERSIONING_SCHEMA_NAME,
                    DocumentModelUtils.getFieldName(PropertiesDef.DOC_PROP_MINOR_VERSION));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

        log.info("Current major version: " + currentMajor);
        log.info("Current minor version: " + currentMinor);

        assertEquals("Major", expectedMajor, currentMajor);
        assertEquals("Minor", expectedMinor, currentMinor);
    }

    /**
     * Utility method to check versions on a Document.
     *
     * @param doc
     * @param expectedMajor
     * @param expectedMinor
     * @throws DocumentException
     */
    protected static void checkVersion(Document doc, Long expectedMajor,
            Long expectedMinor) throws DocumentException {

        final Long currentMajor = (Long) doc.getPropertyValue(PropertiesDef.DOC_PROP_MAJOR_VERSION);
        final Long currentMinor = (Long) doc.getPropertyValue(PropertiesDef.DOC_PROP_MINOR_VERSION);

        log.info("Current major version: " + currentMajor);
        log.info("Current minor version: " + currentMinor);

        assertEquals(expectedMajor, currentMajor);
        assertEquals(expectedMinor, currentMinor);
    }

}
