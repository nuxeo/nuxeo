/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.jms.SerializableEventBundle;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestJMSEventBundle extends NXRuntimeTestCase {

    protected static class CoreSessionInvocationHandler implements
            InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
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
            CoreSession.class.getClassLoader(),
            new Class<?>[] { CoreSession.class },
            new CoreSessionInvocationHandler());

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    protected EventBundle createTestEventBundle() {
        EventBundle bundle = new EventBundleImpl();

        EventContext ctx1 = new EventContextImpl(fakeCoreSession, new SimplePrincipal("toto"));
        EventContext ctx2 = new EventContextImpl(fakeCoreSession, new SimplePrincipal("titi"));

        DocumentRef parentRef = new IdRef("01");
        DocumentRef docRef = new IdRef("02");
        String[] schemas = {"file","dublincore"};
        DocumentModel srcDoc = new DocumentModelImpl("sid0", "File", "02",
                new Path("/"), null, docRef, parentRef, schemas, null, null,
                null);
        DocumentRef destinationRef = new IdRef("03");

        EventContext ctx3 = new DocumentEventContext(
                null, new SimplePrincipal("tata"), srcDoc, destinationRef);

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
