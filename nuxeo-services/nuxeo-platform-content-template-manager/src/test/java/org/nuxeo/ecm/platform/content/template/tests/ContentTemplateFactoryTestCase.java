package org.nuxeo.ecm.platform.content.template.tests;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

public class ContentTemplateFactoryTestCase extends RepositoryTestCase {

    public ContentTemplateFactoryTestCase() {
        super();
    }

    public ContentTemplateFactoryTestCase(String name) {
        super(name);
    }

    protected CoreSession session;

    protected ContentTemplateService service;

    protected void initRepo() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "RepositoryManager.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "CoreTestExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "LifeCycleService.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "DefaultPlatform.xml");

        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "test-content-template-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "test-content-template-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests", "test-content-template-listener.xml");
        deployBundle("org.nuxeo.ecm.core.event");
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        initRepo();

        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        assertNotNull(mgr);
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "Administrator");
        session = mgr.getDefaultRepository().open(ctx);
        assertNotNull(session);
        service = Framework.getLocalService(ContentTemplateService.class);
        assertNotNull(service);
    }



}