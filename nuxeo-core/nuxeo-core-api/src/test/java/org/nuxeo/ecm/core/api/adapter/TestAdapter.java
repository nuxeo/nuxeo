/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestAdapter.java 21546 2007-06-28 11:46:32Z sfermigier $
 */

package org.nuxeo.ecm.core.api.adapter;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestAdapter {

    @SuppressWarnings({"InterfaceNeverImplemented"})
    private interface AnInterface {
    }

    @Test
    public void test() {
        DocumentAdapterService das = new DocumentAdapterService();
        das.activate(null);

        DocumentAdapterDescriptor dad = new DocumentAdapterDescriptor();
        dad.setInterface(AnInterface.class);

        das.registerAdapterFactory(dad);
        assertEquals(dad, das.getAdapterDescriptor(AnInterface.class));

        das.unregisterAdapterFactory(AnInterface.class);
        das.deactivate(null);
    }

}
