/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.Path;

public class EmbeddedDocExtractor {

    public static final String DOC_PREFIX = "doc/";

    public static final String PARENT_DOC_PREFIX = "doc-parent/";

    public static void extractEmbeddedDoc(ZipFile jarFile, BundleInfoImpl bi) throws IOException {

        Enumeration<? extends ZipEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            try (InputStream is = jarFile.getInputStream(entry)) {
                if (entry.getName().startsWith(PARENT_DOC_PREFIX) && !entry.isDirectory()) {
                    String content = IOUtils.toString(is, StandardCharsets.UTF_8);
                    String name = new Path(entry.getName()).lastSegment();
                    if (name.length() >= 6 && name.substring(0, 6).equalsIgnoreCase("readme")) {
                        // TODO
                    }
                }
                if (entry.getName().startsWith(DOC_PREFIX) && !entry.isDirectory()) {
                    String content = IOUtils.toString(is, StandardCharsets.UTF_8);
                    String name = new Path(entry.getName()).lastSegment();
                    if (name.length() >= 6 && name.substring(0, 6).equalsIgnoreCase("readme")) {
                        // TODO
                    }
                }
            }
        }
    }
}
