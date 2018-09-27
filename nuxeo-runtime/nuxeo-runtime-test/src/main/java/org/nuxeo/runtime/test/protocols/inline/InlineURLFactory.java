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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;

public class InlineURLFactory {

    public static final String INLINE_PREFIX = "inline:";

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

    public static URL newURL(String content) throws IOException {
        byte[] data = content.getBytes(UTF_8);
        return new URL(INLINE_PREFIX + Base64.encodeBase64String(data));
    }

    public static String newString(URL url) throws IOException {
        String extUrl = url.toExternalForm();
        if (!extUrl.startsWith(INLINE_PREFIX)) {
            throw new IllegalArgumentException("'" + url + "' should be 'inline:base64content'");
        }
        byte[] data = Base64.decodeBase64(extUrl.substring(INLINE_PREFIX.length()));
        return new String(data, UTF_8);
    }

}
