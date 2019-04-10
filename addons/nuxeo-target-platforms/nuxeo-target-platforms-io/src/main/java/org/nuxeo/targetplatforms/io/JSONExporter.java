/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;

/**
 * JSON exporter for target platforms, packages and related info.
 *
 * @since 5.9.3
 */
public class JSONExporter {

    public static void exportToJson(TargetPlatform tp, OutputStream out,
            boolean pretty) throws IOException {
        exportToJson((Object) tp, out, pretty);
    }

    public static void exportToJson(List<TargetPlatform> tp, OutputStream out,
            boolean pretty) throws IOException {
        exportToJson((Object) tp, out, pretty);
    }

    public static void exportToJson(TargetPlatformInfo tpi, OutputStream out,
            boolean pretty) throws IOException {
        exportToJson((Object) tpi, out, pretty);
    }

    public static void exportInfosToJson(List<TargetPlatformInfo> tpi,
            OutputStream out, boolean pretty) throws IOException {
        exportToJson(tpi, out, pretty);
    }

    public static void exportToJson(TargetPlatformInstance tpi,
            OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tpi, out, pretty);
    }

    public static void exportToJson(TargetPackage tp, OutputStream out,
            boolean pretty) throws IOException {
        exportToJson((Object) tp, out, pretty);
    }

    public static void exportToJson(TargetPackageInfo tpi, OutputStream out,
            boolean pretty) throws IOException {
        exportToJson((Object) tpi, out, pretty);
    }

    protected static JsonFactory createFactory() {
        JsonFactory factory = new JsonFactory();
        final ObjectMapper oc = new ObjectMapper(factory);
        oc.configure(
                SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY,
                true);
        factory.setCodec(oc);
        return factory;
    }

    protected static void exportToJson(Object object, OutputStream out,
            boolean pretty) throws IOException {
        JsonFactory factory = createFactory();
        JsonGenerator jg = factory.createJsonGenerator(out);
        if (pretty) {
            jg.useDefaultPrettyPrinter();
        }
        jg.writeObject(object);
        jg.flush();
    }

}
