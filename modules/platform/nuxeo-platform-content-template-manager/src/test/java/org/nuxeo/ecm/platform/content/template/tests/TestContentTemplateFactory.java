/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.content.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.content.template.service.ContentFactoryDescriptor;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateServiceImpl;
import org.nuxeo.ecm.platform.content.template.service.FactoryBindingDescriptor;
import org.nuxeo.ecm.platform.content.template.service.NotificationDescriptor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template.tests:test-content-template-framework.xml")
@Deploy("org.nuxeo.ecm.platform.content.template.tests:test-content-template-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.content.template.tests:test-content-template-listener.xml")
public class TestContentTemplateFactory {

    @Inject
    protected ContentTemplateService service;

    @Inject
    protected CoreSession session;

    @Before
    public void setUp() throws Exception {
        service.executeFactoryForType(session.getRootDocument());
    }

    @Test
    public void testServiceFactoryContribs() {
        ContentTemplateServiceImpl serviceImpl = (ContentTemplateServiceImpl) service;
        assertNotNull(serviceImpl);
        Map<String, ContentFactoryDescriptor> factories = serviceImpl.getFactories();
        assertTrue(factories.containsKey("SimpleTemplateFactory"));
        assertTrue(factories.containsKey("ImportFactory"));
        assertEquals(2, factories.size());
    }

    @Test
    public void testServiceFactoryBindingContribs() {
        ContentTemplateServiceImpl serviceImpl = (ContentTemplateServiceImpl) service;
        assertNotNull(serviceImpl);
        Map<String, FactoryBindingDescriptor> factoryBindings = serviceImpl.getFactoryBindings();
        assertEquals(4, factoryBindings.size());
        assertTrue(factoryBindings.containsKey("Root"));
        assertTrue(factoryBindings.containsKey("Domain"));

        assertEquals(4, factoryBindings.get("Domain").getTemplate().size());
        assertEquals("Workspaces", factoryBindings.get("Domain").getTemplate().get(0).getId());
    }

    @Test
    public void testServiceFactoryForSecurity() {
        ContentTemplateServiceImpl serviceImpl = (ContentTemplateServiceImpl) service;
        assertNotNull(serviceImpl);
        Map<String, FactoryBindingDescriptor> factoryBindings = serviceImpl.getFactoryBindings();

        FactoryBindingDescriptor factory = factoryBindings.get("Workspace");
        assertNotNull(factory);

        // check that ACL is not null
        assertNotNull(factory.getTemplate().get(1).getAcl());
        assertEquals(2, factory.getTemplate().get(1).getAcl().size());

        // check root ACL
        factory = factoryBindings.get("Root");
        assertNotNull(factory);
        assertNotNull(factory.getRootAcl());
        assertEquals(2, factory.getRootAcl().size());
    }

    @Test
    public void testServiceFactoryForNotifications() {
        ContentTemplateServiceImpl serviceImpl = (ContentTemplateServiceImpl) service;
        assertNotNull(serviceImpl);
        Map<String, FactoryBindingDescriptor> factoryBindings = serviceImpl.getFactoryBindings();

        FactoryBindingDescriptor factory = factoryBindings.get("Workspace");
        assertNotNull(factory);

        List<NotificationDescriptor> notif = factory.getTemplate().get(1).getNotifications();
        assertNotNull(notif);
        assertTrue(!notif.isEmpty());
        assertEquals(2, notif.size());

        NotificationDescriptor notif1 = notif.get(0);
        assertEquals("Modification", notif1.getEvent());

        List<String> users = notif1.getUsers();
        assertNotNull(users);
        assertTrue(!users.isEmpty());
        assertEquals(2, users.size());
        assertEquals("jdoe", users.get(0));
        assertEquals("bree", users.get(1));

        List<String> groups = notif1.getGroups();
        assertNotNull(groups);
        assertTrue(!groups.isEmpty());
        assertEquals(1, groups.size());
        assertEquals("members", groups.get(0));

        NotificationDescriptor notif2 = notif.get(1);
        assertEquals("Creation", notif2.getEvent());

        users = notif2.getUsers();
        assertNotNull(users);
        assertTrue(users.isEmpty());

        groups = notif2.getGroups();
        assertNotNull(groups);
        assertTrue(!groups.isEmpty());
        assertEquals(1, groups.size());
        assertEquals("members", groups.get(0));
    }

    @Test
    public void testRootFactory() {
        // Fake repo init for now
        DocumentModel root = session.getRootDocument();
        service.executeFactoryForType(root);

        // check root ACL
        assertTrue(session.getACP(root.getRef()).getAccess("Administrator", "Everything").toBoolean());
        assertTrue(session.getACP(root.getRef()).getAccess("Danny", "Dream").toBoolean());

        // check that default domain has been created
        DocumentModelList children = session.getChildren(root.getRef());
        assertEquals(1, children.size());

        children = session.getChildren(root.getRef(), "Domain");
        DocumentModel domain = children.get(0);
        assertEquals(1, children.size());
        assertEquals("defaut domain", domain.getTitle());

        // check that the default domain has the template layout
        children = session.getChildren(domain.getRef());
        assertEquals(3, children.size());
        children = session.getChildren(domain.getRef(), "WorkspaceRoot");
        assertEquals(1, children.size());
        assertEquals("Workspaces", children.get(0).getTitle());

        // check that Section is created under sectionRoot
        children = session.getChildren(domain.getRef(), "SectionRoot");
        assertEquals(1, children.size());
        DocumentModel sectionRoot = children.get(0);
        assertEquals("Sections", sectionRoot.getTitle());
        children = session.getChildren(sectionRoot.getRef(), "Section");
        assertEquals(1, children.size());
        assertEquals("Section", children.get(0).getTitle());
    }

    @Test
    public void testDomainFactory() {
        DocumentModel testDom = session.createDocumentModel("/", "TestDomain", "Domain");
        testDom.setProperty("dublincore", "title", "MyTestDomain");
        testDom = session.createDocument(testDom);
        session.save();

        // check that the created domain has the template layout
        DocumentModelList children = session.getChildren(testDom.getRef());
        assertEquals(3, children.size());

        children = session.getChildren(testDom.getRef(), "WorkspaceRoot");
        assertEquals(1, children.size());
        assertEquals("Workspaces", children.get(0).getTitle());
    }

    @Test
    public void testWSFactory() {
        // reach first available WSRoot
        DocumentModel root = session.getRootDocument();
        service.executeFactoryForType(root);

        DocumentModel firstDomain = session.getChildren(root.getRef()).get(0);
        DocumentModel wsRoot = session.getChildren(firstDomain.getRef(), "WorkspaceRoot").get(0);

        // create new WS
        DocumentModel testWS = session.createDocumentModel(wsRoot.getPathAsString(), "TestWS", "Workspace");
        testWS.setProperty("dublincore", "title", "MyTestWorkspace");
        testWS = session.createDocument(testWS);
        session.save();

        // Check children, rights and properties
        DocumentModelList children = session.getChildren(testWS.getRef());
        assertEquals(3, children.size());

        for (DocumentModel child : children) {
            if (child.getTitle().equals("Folder1")) {
                ACP acp = session.getACP(child.getRef());
                ACL existingACL = acp.getACL(ACL.LOCAL_ACL);
                if (existingACL != null) {
                    assertEquals(0, existingACL.size());
                }
                // check properties
                assertEquals("Administrator", child.getPropertyValue("dublincore:creator"));
                assertEquals("coverage", child.getPropertyValue("dublincore:coverage"));
            } else if (child.getTitle().equals("Secret Folder")) {
                ACP acp = session.getACP(child.getRef());
                ACL existingACL = acp.getACL(ACL.LOCAL_ACL);
                assertNotNull(existingACL);
                assertEquals(2, existingACL.size());
            } else if (child.getTitle().equals("Folder2")) {
                ACP acp = session.getACP(child.getRef());
                ACL existingACL = acp.getACL(ACL.LOCAL_ACL);
                if (existingACL != null) {
                    assertEquals(0, existingACL.size());
                }
            } else {
                // we should not go here !!!
                fail();
            }
        }
    }

    @Test
    public void testFacetFactories() {
        // reach first available WSRoot
        DocumentModel root = session.getRootDocument();
        service.executeFactoryForType(root);
        DocumentModel firstDomain = session.getChildren(root.getRef()).get(0);

        // Check if every superspaces have FacetFolder Document
        DocumentModel wsRoot = session.getChildren(firstDomain.getRef(), "WorkspaceRoot").get(0);
        DocumentModel facetFolder = session.getChild(wsRoot.getRef(), "FacetFolder");
        assertNotNull(facetFolder);

        DocumentModel templateRoot = session.getChildren(firstDomain.getRef(), "TemplateRoot").get(0);
        facetFolder = session.getChild(templateRoot.getRef(), "FacetFolder");
        assertNotNull(facetFolder);

        DocumentModel sectionRoot = session.getChildren(firstDomain.getRef(), "SectionRoot").get(0);
        facetFolder = session.getChild(sectionRoot.getRef(), "FacetFolder");
        assertNotNull(facetFolder);
    }

    /*
     * NXP-24757
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.content.template.tests:OSGI-INF/test-content-template-mandatory-metadata-contrib.xml")
    public void testFolderFactoryWithMandatoryMetadata() {
        DocumentModel specialFolder = session.createDocumentModel("/", "SpecialFolder", "SpecialFolder");
        specialFolder = session.createDocument(specialFolder);
        session.save();

        // check that the created special folder has a special file with the mandatory metadata holding default value
        DocumentModelList children = session.getChildren(specialFolder.getRef());
        assertEquals(1, children.size());
        assertEquals("france", children.get(0).getPropertyValue("sf:country"));
    }

}
