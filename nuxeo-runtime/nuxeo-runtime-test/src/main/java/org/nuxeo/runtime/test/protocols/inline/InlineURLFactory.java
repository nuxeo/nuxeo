/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.net.URLStreamHandlerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;

public class InlineURLFactory {

    public static void install() {
        shf = new InlineURLStreamHandlerFactory();
        try {
            URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(shf);
        } catch (Exception e) {
            throw new RuntimeException("Cannot install inline URLs", e);
        }
    }

    protected static URLStreamHandlerFactory shf;

    public static void uninstall() {
        try {
            URLStreamHandlerFactoryInstaller.uninstallURLStreamHandlerFactory(shf);
        } catch (Exception cause) {
            throw new RuntimeException("Cannot uninstall inline URLs", cause);
        } finally {
            shf = null;
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
            throw new RuntimeException("Cannot decode, object is not of class " + clazz.getSimpleName(), e);
        }
    }

    public static <T> URL newURL(T content) throws IOException {
        byte[] data = marshall(content);
        return newURL("application/java", data);
    }

    public static URL newURL(String mimetype, byte[] data) throws IOException {
        return new URL("inline:".concat(mimetype).concat(";base64,".concat(Base64.encodeBase64String(data))));
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
        return Base64.decodeBase64(data);
    }

}
