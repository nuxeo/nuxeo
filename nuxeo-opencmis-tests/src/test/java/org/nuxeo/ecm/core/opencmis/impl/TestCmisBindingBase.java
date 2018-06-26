/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assume.assumeTrue;

import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.TempStoreOutputStreamFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactoryManager;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoBinding;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepositories;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Common methods for the binding tests.
 */
public abstract class TestCmisBindingBase {

    protected static final String USERNAME = "Administrator";

    protected static final String PASSWORD = "test";

    protected static final int THRESHOLD = 4 * 1024 * 1024;

    protected static final int MAX_SIZE = -1;

    protected String repositoryId;

    protected String rootFolderId;

    protected BindingsObjectFactory factory;

    protected RepositoryService repoService;

    protected ObjectService objService;

    protected NavigationService navService;

    protected MultiFilingService filingService;

    protected DiscoveryService discService;

    protected VersioningService verService;

    protected AclService aclService;

    protected CmisBinding binding;

    protected String file5id;

    protected String file6verid;

    protected String proxyid;

    @Inject
    protected CoreFeature coreFeature;

    protected void assumeSupportsJoins() {
        assumeTrue("joins not supported", supportsJoins());
    }

    protected void assumeSupportsProxies() {
        assumeTrue("proxies not supported", supportsProxies());
    }

    protected boolean supportsJoins() {
        return false;
    }

    protected boolean supportsProxies() {
        return true;
    }

    protected boolean useElasticsearch() {
        return false;
    }

    protected boolean returnsRootInFolderQueries() {
        return supportsJoins();
    }

    /**
     * Whether negative matches for list values also match empty lists.
     * <p>
     * Will return true if "ANY listprop NOT IN ('foo')" returns documents where listprop is empty.
     */
    protected boolean emptyListNegativeMatch() {
        return coreFeature.getStorageConfiguration().isDBSMongoDB();
    }

    protected boolean supportsNXQLQueryTransformers() {
        return !supportsJoins();
    }

    public void setUpBinding(CoreSession coreSession) {
        setUpBinding(coreSession, USERNAME);
    }

    public void setUpBinding(CoreSession coreSession, String username) {
        repositoryId = coreSession.getRepositoryName();
        rootFolderId = coreSession.getRootDocument().getId();
        initBinding(repositoryId, username);
        factory = binding.getObjectFactory();
        repoService = binding.getRepositoryService();
        objService = binding.getObjectService();
        navService = binding.getNavigationService();
        filingService = binding.getMultiFilingService();
        discService = binding.getDiscoveryService();
        verService = binding.getVersioningService();
        aclService = binding.getAclService();
    }

    public void tearDownBinding() {
        closeBinding();
    }

    protected void initBinding(String repositoryId, String username) {
        NuxeoRepository repository = Framework.getService(NuxeoRepositories.class).getRepository(repositoryId);
        repository.setSupportsJoins(supportsJoins());
        repository.setSupportsProxies(supportsProxies());
        repository.setUseElasticsearch(useElasticsearch());

        NuxeoCmisServiceFactoryManager manager = Framework.getService(NuxeoCmisServiceFactoryManager.class);
        NuxeoCmisServiceFactory serviceFactory = manager.getNuxeoCmisServiceFactory();
        TempStoreOutputStreamFactory streamFactory = TempStoreOutputStreamFactory.newInstance( //
                Environment.getDefault().getTemp(), THRESHOLD, MAX_SIZE, false);
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        CallContextImpl context = new CallContextImpl(CallContext.BINDING_LOCAL, CmisVersion.CMIS_1_1, repositoryId,
                FakeServletContext.getServletContext(), request, response, serviceFactory, streamFactory);
        context.put(CallContext.USERNAME, username);
        context.put(CallContext.PASSWORD, PASSWORD);
        CmisService service = serviceFactory.getService(context);
        binding = new NuxeoBinding(service);
    }

    protected void closeBinding() {
        binding.close();
    }

    @Inject
    TransactionalFeature txFeature;

    protected void setUpData(CoreSession coreSession) throws Exception {
        Map<String, String> info = Helper.makeNuxeoRepository(coreSession, true); // add a proxy
        txFeature.nextTransaction(); // flush
        sleepForFulltext();
        file5id = info.get("file5id");
        file6verid = info.get("file6verid");
        proxyid = info.get("proxyid");
    }

    protected void waitForAsyncCompletion() {
        nextTransaction();
    }

    protected void sleepForFulltext() {
        waitForAsyncCompletion();
        coreFeature.getStorageConfiguration().sleepForFulltext();
    }

    protected boolean supportsMultipleFulltextIndexes() {
        return coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes();
    }

    protected void nextTransaction() {
        txFeature.nextTransaction();
    }

}
