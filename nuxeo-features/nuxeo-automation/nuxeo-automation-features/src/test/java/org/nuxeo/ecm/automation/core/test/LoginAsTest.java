/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import java.util.ArrayList;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core",
    "org.nuxeo.ecm.automation.features",
    "org.nuxeo.ecm.platform.web.common", "org.nuxeo.ecm.platform.login" })
public class LoginAsTest {

    protected DocumentModel src;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    UserManager mgr;

    @Before
    public void initRepo() throws Exception {
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
        ArrayList<String> groups = new ArrayList<String>();
        groups.add("administrators");
        userModel.setProperty("user", "groups", groups);
        userModel = mgr.createUser(userModel);
    }

    // ------ Tests comes here --------

    @Test
    public void testLoginAs() throws Exception {
        // change the user inside an operation chain.

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        String origPrincipal = ctx.getPrincipal().getName();
        System.out.println(origPrincipal);
        OperationChain chain = new OperationChain("testloginas");
        chain.add(FetchContextDocument.ID);
        chain.add(LoginAs.ID).set("name", "Foo");
        chain.add(CreateDocument.ID).set("type", "Folder").set("name", "myfolder");
        DocumentModel doc = (DocumentModel)service.run(ctx, chain);

        Assert.assertEquals(origPrincipal, ctx.getPrincipal().getName());
        Assert.assertEquals("Foo", doc.getPropertyValue("dc:creator"));
    }

}
