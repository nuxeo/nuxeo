/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test.protocols.inline;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;

public class InlineURLFactory {

    public static void install() {
        try {
            URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(new InlineURLStreamHandlerFactory());
        } catch (Exception e) {
            throw new Error("Cannot install inline URLs", e);
        }
    }

    public static <T> byte[] marshall(T content) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(content);
        return bos.toByteArray();
    }

    public static <T> T unmarshall(Class<T> clazz, byte[] data) throws IOException {
        InputStream is = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(is);
        try {
            return clazz.cast(ois.readObject());
        } catch (ClassNotFoundException e) {
            throw new Error("Cannot decode, object is not of class " + clazz.getSimpleName(), e);
        }
    }

    public static <T> URL newURL(T content) throws IOException {
        byte[] data = marshall(content);
        return newURL("application/java",data);
    }

    public static URL newURL(String mimetype, byte[] data) throws IOException {
        return new URL("inline:".concat(mimetype).concat(";base64,".concat(Base64.encodeBytes(data))));
    }

    public static <T> T newObject(Class<T> clazz, URL url) throws IOException {
        byte[] data = getBytes(url);
        return unmarshall(clazz, data);
    }

    protected static final Pattern pattern = Pattern.compile("inline:(.*);base64,(.*)");

    public static byte[] getBytes(URL url) throws IOException {
        Matcher matcher = pattern.matcher(url.toExternalForm());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + url + "' should be 'inline:mimetype;base64,content'");
        }
        @SuppressWarnings("unused")
        String mimetype = matcher.group(1);
        String data = matcher.group(2);
        return Base64.decode(data);
    }

}
