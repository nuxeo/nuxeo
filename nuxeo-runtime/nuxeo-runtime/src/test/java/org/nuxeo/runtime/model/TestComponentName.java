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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.runtime.model;

import junit.framework.TestCase;

public class TestComponentName extends TestCase {

    public void test() {
        ComponentName cn1 = new ComponentName("foo:bar");
        ComponentName cn2 = new ComponentName("foo", "bar");
        ComponentName cn3 = new ComponentName("fu:baz");

        assertEquals("foo", cn1.getType());
        assertEquals("bar", cn1.getName());
        assertEquals("foo:bar", cn1.getRawName());

        assertEquals(cn1, cn2);
        assertEquals(cn1.hashCode(), cn2.hashCode());
        assertEquals(cn1.toString(), cn2.toString());

        assertNotNull(cn1);
        assertFalse(cn1.equals(cn3));
        assertFalse(cn3.equals(cn1));
    }

}
