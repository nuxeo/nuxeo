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
 * $Id: SerializableHelper.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Helper to test object serialization. Used only in tests.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class SerializableHelper {

    // This is an utility class.
    private SerializableHelper() {
    }

    /**
     * Checks if a given object is serializable.
     *
     * @param ob the actual object we want to test
     * @return true if the object is serializable.
     */
    // XXX AT: since class loader isolation, this module is not aware anymore of
    // nuxeo.ear classes => ClassCastException can be thrown is tested object is
    // a DocumentModel for instance.
    public static boolean isSerializable(Object ob) {
        if (!(ob instanceof Serializable)) {
            return false;
        }
        try {
            ob = serializeUnserialize(ob);
            return ob != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Serializes and unserializes back an object to test whether it is correctly
     * rebuilt (to be used in unit tests as sanity checks).
     *
     * @param ob the actual object we want to test
     * @return true if the object is serializable.
     */
    public static Object serializeUnserialize(Object ob) throws Exception {
        Serializable in = (Serializable) ob;
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteOutStream);
        outStream.writeObject(in);
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(
                byteOutStream.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(byteInStream);
        return inStream.readObject();
    }

}
