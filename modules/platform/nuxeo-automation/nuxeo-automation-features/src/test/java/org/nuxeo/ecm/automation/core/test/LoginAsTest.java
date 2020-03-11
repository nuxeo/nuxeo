/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import java.util.ArrayList;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.login.LoginAs;
import org.nuxeo.ecm.automation.core.operations.login.Logout;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.test.NuxeoLoginFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, NuxeoLoginFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public class LoginAsTest {

    @Inject
    protected AutomationService service;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager mgr;

    protected DocumentModel src;

    @Before
    public void initRepo() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        DocumentModel userModel = mgr.getBareUserModel();
        String schemaName = mgr.getUserSchemaName();
        userModel.setProperty(schemaName, "username", "Foo");
        ArrayList<String> groups = new ArrayList<>();
        groups.add("administrators");
        userModel.setProperty("user", "groups", groups);
        mgr.createUser(userModel);
    }

    // ------ Tests comes here --------

    @Test
    public void testLoginAs() throws Exception {
        // change the user inside an operation chain.

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(src);
            String origPrincipal = ctx.getPrincipal().getName();
            OperationChain chain = new OperationChain("testloginas");
            chain.add(FetchContextDocument.ID);
            chain.add(LoginAs.ID).set("name", "Foo");
            chain.add(CreateDocument.ID).set("type", "Folder").set("name", "myfolder");
            chain.add(Logout.ID);

            DocumentModel doc = (DocumentModel) service.run(ctx, chain);

            Assert.assertEquals(origPrincipal, ctx.getPrincipal().getName());
            Assert.assertEquals("Foo", doc.getPropertyValue("dc:creator"));
        }
    }

}
