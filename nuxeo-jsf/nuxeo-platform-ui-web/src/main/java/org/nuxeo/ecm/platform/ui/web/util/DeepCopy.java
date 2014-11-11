/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DeepCopy.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Deep copy utils.
 * <p>
 * Most classes implementing the {@link Cloneable} interface only perform a
 * shallow copy of an object. This class performs deep cloning serializing and
 * deserializing an object. Therefore only serializable objects can be copied
 * using this util.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
// XXX AT: needs optimizing, see
// http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
public final class DeepCopy {

    // Utility class.
    private DeepCopy() {
    }

    public static Object deepCopy(Object object) {
        Object copy;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
            copy = in.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
        return copy;
    }

}
