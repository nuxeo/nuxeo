/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.DepthValues;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.usermanager.api", "org.nuxeo.ecm.platform.usermanager",
        "org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml", "org.nuxeo.ecm.permissions" })
@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class ACLJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public ACLJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    private ACE ace1;

    private ACE ace2;

    @Before
    public void before() {
        DocumentModel root = session.getDocument(new PathRef("/"));
        ACP acp = root.getACP();
        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(Constants.NOTIFY_KEY, false);
        contextData.put(Constants.COMMENT_KEY, "sample comment");
        ace1 = ACE.builder("Administrator", "Read").creator("Administrator").contextData(contextData).build();
        ace2 = new ACE("joe", "Read");
        acp.addACE(ACL.LOCAL_ACL, ace1);
        acp.addACE(ACL.LOCAL_ACL, ace2);
        root.setACP(acp, true);
    }

    @After
    public void tearDown() {
        DocumentModel root = session.getDocument(new PathRef("/"));
        ACP acp = root.getACP();
        acp.removeACE(ACL.LOCAL_ACL, ace1);
        acp.removeACE(ACL.LOCAL_ACL, ace2);
        root.setACP(acp, true);
    }

    @Test
    public void test() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root, CtxBuilder.enrichDoc("acls").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.hasNot("ace");
        json.has("aces").isArray();
        json = json.has("aces").get(0);
        json.has("username").isText();
        json.has("creator").isNull();
    }

    @Test
    public void testUsersFetching() throws IOException {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root,
                CtxBuilder.enrichDoc("acls")
                          .fetch("acls", "username")
                          .fetch("acls", "creator")
                          .depth(DepthValues.children)
                          .get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.has("aces").isArray();
        json = json.has("aces").get(3);
        json.has("username").isObject();
        json.has("creator").isObject();
    }

    @Test
    public void testExtendedFetching() throws IOException {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root,
                CtxBuilder.enrichDoc("acls").fetch("acls", "extended").depth(DepthValues.children).get());
        json = json.has("contextParameters").isObject();
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.has("aces").isArray();
        json = json.has("aces").get(3);
        json.has("notify").isEquals(false);
        json.has("comment").isEquals("sample comment");
    }

    @Test
    public void testExtendedFetchingAsRegularUser() throws Exception {
        CoreSession systemSession = session;
        try (CoreSession joeSession = CoreInstance.openCoreSession(session.getRepositoryName(), "joe")) {
            session = joeSession;
            LoginContext loginContext = Framework.login("joe", "joe");
            try {
                testExtendedFetching();
            } finally {
                loginContext.logout();
            }
        } finally {
            session = systemSession;
        }
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.permissions:test-acl-enricher-compat-config.xml")
    public void testCompatibility() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root, CtxBuilder.enrichDoc("acls").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.has("aces").isArray();
        JsonAssert aces = json.has("ace").get(0);
        aces.has("username").isText();
        aces.has("creator").isNull();
        json.has("ace").isArray();
        JsonAssert ace = json.has("ace").get(0);
        ace.has("username").isText();
        ace.has("creator").isNull();
    }
}
