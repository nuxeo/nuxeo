/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.jms.SerializableEventBundle;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.event")
public class TestJMSEventBundle {

    protected static class CoreSessionInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String name = method.getName();
            if (name.equals("getRepositoryName")) {
                return "default";
            } else if (name.equals("exists")) {
                return Boolean.FALSE;
            }
            return null;
        }
    }

    protected final CoreSession fakeCoreSession = (CoreSession) Proxy.newProxyInstance(
            CoreSession.class.getClassLoader(), new Class<?>[] { CoreSession.class },
            new CoreSessionInvocationHandler());

    protected EventBundle createTestEventBundle() {
        EventBundle bundle = new EventBundleImpl();

        EventContext ctx1 = new EventContextImpl(fakeCoreSession, new UserPrincipal("toto", null, false, false));
        EventContext ctx2 = new EventContextImpl(fakeCoreSession, new UserPrincipal("titi", null, false, false));

        DocumentRef parentRef = new IdRef("01");
        DocumentRef docRef = new IdRef("02");
        String[] schemas = { "file", "dublincore" };
        DocumentModel srcDoc = new DocumentModelImpl("sid0", "File", "02", new Path("/"), null, docRef, parentRef,
                schemas, null, null, null);
        DocumentRef destinationRef = new IdRef("03");

        EventContext ctx3 = new DocumentEventContext(null, new UserPrincipal("tata", null, false, false), srcDoc,
                destinationRef);

        bundle.push(ctx1.newEvent("EVT1"));
        bundle.push(ctx2.newEvent("EVT2"));
        bundle.push(ctx3.newEvent("EVT3"));

        return bundle;
    }

    public static Object serialize(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(obj);
        out.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        return in.readObject();
    }

    @Test
    public void testBundleSerialization() throws Exception {
        EventBundle srcEventBundle = createTestEventBundle();
        SerializableEventBundle srcJmsEventBundle = new SerializableEventBundle(srcEventBundle);
        SerializableEventBundle dstJmsEventBundle2 = (SerializableEventBundle) serialize(srcJmsEventBundle);
        assertNotNull(dstJmsEventBundle2);

        EventBundle dstEventBundle2 = dstJmsEventBundle2.reconstructEventBundle(fakeCoreSession);
        assertNotNull(dstEventBundle2);
        List<Event> events = new ArrayList<Event>();
        for (Event event : dstEventBundle2) {
            events.add(event);
        }
        assertEquals(3, events.size());

        if (!(events.get(0).getContext() instanceof EventContextImpl)) {
            fail();
        }
        if (!(events.get(1).getContext() instanceof EventContextImpl)) {
            fail();
        }
        if (!(events.get(2).getContext() instanceof DocumentEventContext)) {
            fail();
        }

        DocumentEventContext docCtx = (DocumentEventContext) events.get(2).getContext();
        assertNotNull(docCtx.getSourceDocument());
        assertNotNull(docCtx.getDestination());

        assertEquals("02", docCtx.getSourceDocument().getRef().toString());
        assertEquals("03", docCtx.getDestination().toString());
        assertEquals("tata", docCtx.getPrincipal().getName());
    }

}
