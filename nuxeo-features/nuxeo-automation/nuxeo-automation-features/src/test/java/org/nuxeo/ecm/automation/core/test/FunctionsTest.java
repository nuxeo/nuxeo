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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Functions;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.features.PlatformFunctions;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class})
@Deploy({"org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features"})
public class FunctionsTest {

    protected DocumentModel src;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    OperationContext ctx;


    @Before
    public void initRepo() throws Exception {
        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());
        ctx = new OperationContext(session);
        ctx.setInput(src);
    }

    @After
    public void clearRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
    }

    @Test
    public void testPrincipalWrapper() throws Exception {
        assertEquals(Functions.getInstance(), Scripting.newExpression("Fn").eval(ctx));
        assertEquals(Functions.getInstance().getClass(), PlatformFunctions.class);
        NuxeoPrincipal np = (NuxeoPrincipal)((PlatformFunctions)Functions.getInstance()).getPrincipal("Administrator");
        assertEquals("Administrator", np.getName());
        assertEquals("Administrator",
                ((NuxeoPrincipalImpl)Scripting.newExpression("Fn.getPrincipal(\"Administrator\")").eval(ctx)).getName());

    }

    public void testPrincipalProperties() throws Exception {
        NuxeoPrincipalImpl np = new NuxeoPrincipalImpl("test");
        np.setFirstName("Bob");
        assertEquals("test",
                (String) Scripting.newExpression("CurrentUser.name").eval(ctx));
        assertEquals(
                "Bob",
                (String) Scripting.newExpression("CurrentUser.firstName").eval(
                        ctx));
    }



}
