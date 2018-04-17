/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.automation.context.ContextService;
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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public class FunctionsTest {

    protected DocumentModel src;

    @Inject
    CoreSession session;

    @Inject
    ContextService ctxService;

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
        Map<String, ContextHelper> contextHelperList = ctxService.getHelperFunctions();
        PlatformFunctions functions = (PlatformFunctions) contextHelperList.get("Fn");
        assertEquals(functions, Scripting.newExpression("Fn").eval(ctx));
        assertTrue(functions instanceof PlatformFunctions);

        NuxeoPrincipal np = (functions).getPrincipal("Administrator");
        assertEquals("Administrator", np.getName());
        assertEquals("Administrator",
                ((Principal) Scripting.newExpression("Fn.getPrincipal(\"Administrator\")").eval(ctx)).getName());
    }

    public void testPrincipalProperties() throws Exception {
        NuxeoPrincipalImpl np = new NuxeoPrincipalImpl("test");
        np.setFirstName("Bob");
        assertEquals("test", Scripting.newExpression("CurrentUser.name").eval(ctx));
        assertEquals("Bob", Scripting.newExpression("CurrentUser.firstName").eval(ctx));
    }

}
