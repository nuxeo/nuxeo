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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestAdapter.java 21546 2007-06-28 11:46:32Z sfermigier $
 */

package org.nuxeo.ecm.core.api.adapter;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestAdapter {

    @SuppressWarnings({ "InterfaceNeverImplemented" })
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
