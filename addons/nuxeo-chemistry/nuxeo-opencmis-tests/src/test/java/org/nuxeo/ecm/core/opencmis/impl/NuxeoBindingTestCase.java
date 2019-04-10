/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.ThresholdOutputStreamFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactoryManager;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoBinding;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepositories;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class NuxeoBindingTestCase {

    public static final String USERNAME = "Administrator";

    public static final String PASSWORD = "test";

    private static final int THRESHOLD = 4 * 1024 * 1024;

    private static final int MAX_SIZE = -1;

    public String repositoryId;

    public String rootFolderId;

    public CmisBinding binding;

    protected boolean supportsJoins() {
        return false;
    }

    protected boolean returnsRootInFolderQueries() {
        return supportsJoins();
    }

    protected boolean supportsNXQLQueryTransformers() {
        return !supportsJoins();
    }

    public static class NuxeoTestCase extends SQLRepositoryTestCase {
    }

    public NuxeoTestCase nuxeotc;

    public void setUp() throws Exception {
        nuxeotc = new NuxeoTestCase();
        nuxeotc.setUp();
        deployBundles();
        nuxeotc.fireFrameworkStarted();
        nuxeotc.openSession();

        Map<String, String> params = new HashMap<String, String>();
        params.put(SessionParameter.BINDING_SPI_CLASS,
                SessionParameter.LOCAL_FACTORY);
        params.put(SessionParameter.LOCAL_FACTORY,
                NuxeoCmisServiceFactory.class.getName());

        initBinding();
    }

    protected void deployBundles() throws Exception {
        // NuxeoCmisServiceFactoryManager registration
        nuxeotc.deployBundle("org.nuxeo.ecm.core.opencmis.bindings");
        // QueryMaker registration
        nuxeotc.deployBundle("org.nuxeo.ecm.core.opencmis.impl");
        // MyDocType
        nuxeotc.deployBundle("org.nuxeo.ecm.core.opencmis.tests");
        // MIME Type Icon Updater for renditions
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.types.api");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.types.core");
        // Audit Service
        nuxeotc.deployBundle("org.nuxeo.ecm.core.persistence");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.audit.api");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.audit");
        nuxeotc.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests",
                "OSGI-INF/audit-persistence-config.xml");
        // these deployments needed for NuxeoAuthenticationFilter.loginAs
        nuxeotc.deployBundle("org.nuxeo.ecm.directory.types.contrib");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.login");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.web.common");

        // types
        nuxeotc.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests",
                "OSGI-INF/types-contrib.xml");
    }

    public void initBinding() throws Exception {
        initBinding(USERNAME);
    }

    public void initBinding(String username) throws Exception {
        repositoryId = nuxeotc.database.repositoryName;
        NuxeoRepository repository = Framework.getService(
                NuxeoRepositories.class).getRepository(repositoryId);
        repository.setSupportsJoins(supportsJoins());
        rootFolderId = repository.getRootFolderId();

        NuxeoCmisServiceFactoryManager manager = Framework.getService(NuxeoCmisServiceFactoryManager.class);
        NuxeoCmisServiceFactory serviceFactory = manager.getNuxeoCmisServiceFactory();
        ThresholdOutputStreamFactory streamFactory = ThresholdOutputStreamFactory.newInstance(
                new File(System.getProperty("java.io.tmpdir")),
                THRESHOLD, MAX_SIZE, false);
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        CallContextImpl context = new CallContextImpl(
                CallContext.BINDING_LOCAL, CmisVersion.CMIS_1_1, repositoryId,
                FakeServletContext.getServletContext(), request, response,
                serviceFactory, streamFactory);
        context.put(CallContext.USERNAME, username);
        context.put(CallContext.PASSWORD, PASSWORD);
        CmisService service = serviceFactory.getService(context);
        binding = new NuxeoBinding(service);
    }

    public boolean supportsMultipleFulltextIndexes() {
        return nuxeotc.database.supportsMultipleFulltextIndexes();
    }

    public void sleepForFulltext() {
        nuxeotc.waitForFulltextIndexing();
    }

    public void waitForAsyncCompletion() {
        nuxeotc.waitForAsyncCompletion();
    }

    public void tearDown() throws Exception {
        closeBinding();
        if (nuxeotc != null) {
            nuxeotc.closeSession();
            nuxeotc.tearDown();
        }
    }

    public void closeBinding() {
        if (binding != null) {
            binding.close();
        }
    }

}
