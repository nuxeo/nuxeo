/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.platform.shibboleth.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.shibboleth.web.tree.UserTreeNode;
import org.nuxeo.ecm.platform.shibboleth.web.tree.UserTreeNodeHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.shibboleth.groups.web")
@Deploy("org.nuxeo.ecm.platform.shibboleth.groups.web:OSGI-INF/test-shibboleth-groups-contrib.xml")
public class TestUserTreeNodeParsing {

    protected DocumentModel newDoc(String id, String type) {
        return new DocumentModelImpl(null, type, id, null, null, null, null, new String[0], null, null, null);
    }

    @Test
    public void testParsing() {
        DocumentModel doc1 = newDoc("hello:::world:::john", "type");
        DocumentModel doc2 = newDoc("hello:::world:::john2", "type");

        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        docs.add(doc1);
        docs.add(doc2);

        List<UserTreeNode> nodes = UserTreeNodeHelper.getHierarcicalNodes(docs);
        assertEquals(1, nodes.size());
        assertEquals("hello", nodes.get(0).getId());
        assertEquals(1, nodes.get(0).getChildrens().size());
        assertEquals("world", nodes.get(0).getChildrens().get(0).getId());
        assertEquals(2, nodes.get(0).getChildrens().get(0).getChildrens().size());
        assertEquals("hello:::world:::john2", nodes.get(0).getChildrens().get(0).getChildrens().get(1).getId());
    }

    @Test
    public void testMixedGroup() {
        DocumentModel doc1 = newDoc("Administrators", "type");
        DocumentModel doc2 = newDoc("Administrator:::Admini", "type");
        DocumentModel doc3 = newDoc("Administrator:::Admini:::hello", "type");

        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        docs.add(doc1);
        docs.add(doc2);
        docs.add(doc3);

        List<UserTreeNode> nodes = UserTreeNodeHelper.getHierarcicalNodes(docs);
        assertEquals(2, nodes.size());
        assertEquals("Administrators", nodes.get(0).getId());
        assertEquals("Administrator", nodes.get(1).getId());
        assertEquals(2, nodes.get(1).getChildrens().size());
        assertEquals("Administrator:::Admini", nodes.get(1).getChildrens().get(0).getId());
        assertEquals("Admini", nodes.get(1).getChildrens().get(1).getId());
        assertEquals(1, nodes.get(1).getChildrens().get(1).getChildrens().size());
    }

    @Test
    public void testBuildBranch() {
        DocumentModel doc1 = newDoc("test", "type");
        DocumentModel doc2 = newDoc("test:::test", "type");

        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        docs.add(doc1);
        docs.add(doc2);

        List<UserTreeNode> nodes = UserTreeNodeHelper.buildBranch("branch:::hello", docs);
        assertEquals(1, nodes.size());
        assertEquals(1, nodes.get(0).getChildrens().size());
        assertEquals(2, nodes.get(0).getChildrens().get(0).getChildrens().size());

        nodes = UserTreeNodeHelper.buildBranch("external:::group", new ArrayList<DocumentModel>());
        assertEquals(0, nodes.size());
    }

    @Test
    public void testParsingHandling() {
        DocumentModel doc1 = newDoc("empty", "type");
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        docs.add(doc1);

        List<UserTreeNode> nodes = UserTreeNodeHelper.getHierarcicalNodes(docs);
        assertEquals(1, nodes.size());
        assertEquals("empty", nodes.get(0).getId());

        doc1 = newDoc(":::hello:::", "type");
        docs = new ArrayList<DocumentModel>();
        docs.add(doc1);

        nodes = UserTreeNodeHelper.getHierarcicalNodes(docs);
        assertEquals(1, nodes.size());
        assertEquals("", nodes.get(0).getId());
        assertEquals(1, nodes.get(0).getChildrens().size());
        assertEquals("hello", nodes.get(0).getChildrens().get(0).getId());
        assertEquals(1, nodes.get(0).getChildrens().get(0).getChildrens().size());
    }

    @Test
    public void testDisplayedName() {
        assertEquals("node", new UserTreeNode("node").getDisplayedName());
        assertNotSame("node", new UserTreeNode("node2").getDisplayedName());

        DocumentModel doc1 = newDoc("empty", "type");
        assertEquals("empty", new UserTreeNode(doc1).getDisplayedName());

        doc1 = newDoc("empty:::hello", "type");
        assertEquals("hello", new UserTreeNode(doc1).getDisplayedName());

        doc1 = newDoc("empty:::hello:::", "type");
        assertEquals("", new UserTreeNode(doc1).getDisplayedName());
    }
}
