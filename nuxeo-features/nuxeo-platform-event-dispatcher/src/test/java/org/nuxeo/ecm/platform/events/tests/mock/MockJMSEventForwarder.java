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

package org.nuxeo.ecm.platform.events.tests.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.jms.JMSBusNotActiveException;
import org.nuxeo.ecm.core.event.jms.JMSEventBundle;
import org.nuxeo.ecm.core.event.jms.JmsEventForwarder;

public class MockJMSEventForwarder extends JmsEventForwarder implements PostCommitEventListener {

    protected static int invocationCount = 0;

    public static int getInvocationCount() {
        return invocationCount;
    }

    public static void reset() {
        invocationCount=0;
    }
     public final static Object serialize(Object obj) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            out.flush();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            return in.readObject();
        }


    @Override
    protected void produceJMSMessage(JMSEventBundle message) throws JMSBusNotActiveException {

        invocationCount+=1;
        Object msg=null;
        try {
            msg = serialize(message);
        } catch (Exception e) {
            throw new JMSBusNotActiveException(e);
        }

        MockEventBundleJMSListener relay = new MockEventBundleJMSListener();
        relay.onMessage((JMSEventBundle)msg);
    }


}
