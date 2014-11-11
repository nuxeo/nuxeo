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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestSerializableHelper.java 20633 2007-06-17 11:54:03Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.io.Serializable;

import junit.framework.TestCase;

/**
 * @author sfermigier
 */
public class TestSerializableHelper extends TestCase {

    public void test() {
        assertTrue(SerializableHelper.isSerializable(new SerializableClass()));
        assertFalse(SerializableHelper.isSerializable(new NonSerializableClass()));
    }

}

class SerializableClass implements Serializable {
    private static final long serialVersionUID = -2338745642609144868L;
}

class NonSerializableClass {
}
