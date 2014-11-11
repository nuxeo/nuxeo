/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: TestWAPISerialization.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.common.utils.SerializableHelper;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstanceIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMProcessInstanceIteratorImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMWorkItemIteratorImpl;

/**
 * Test WAPI elements serialization.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestWAPISerialization extends TestCase {

    public void testWMWorkItemIterator() throws Exception {

        // Empty
        WMWorkItemIterator it = new WMWorkItemIteratorImpl();
        assertTrue(SerializableHelper.isSerializable(it));

        // With empty content
        List<WMWorkItemInstance> items = new ArrayList<WMWorkItemInstance>();
        it = new WMWorkItemIteratorImpl(items);
        assertTrue(SerializableHelper.isSerializable(it));

    }

    public void testWMProcessInstanceIterator() throws Exception {

        // Empty
        WMProcessInstanceIterator it = new WMProcessInstanceIteratorImpl();
        assertTrue(SerializableHelper.isSerializable(it));

        // With empty content
        List<WMProcessInstance> items = new ArrayList<WMProcessInstance>();
        it = new WMProcessInstanceIteratorImpl(items);
        assertTrue(SerializableHelper.isSerializable(it));

    }

}
