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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.scan;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Scanner {

    public final static String PATH_ANNO = "Ljavax/ws/rs/Path;";

    public final static String PROVIDER_ANNO = "Ljavax/ws/rs/ext/Provider;";

    protected Bundle bundle;

    protected String packageBase;

    protected Map<String, Collection<Class<?>>> collectors;

    public Scanner(Bundle bundle, String packageBase) {
        this(bundle, packageBase, PATH_ANNO, PROVIDER_ANNO);
    }

    public Scanner(Bundle bundle, String packageBase, String... annotations) {
        this.bundle = bundle;
        this.packageBase = packageBase == null ? "/" : packageBase;
        this.collectors = new HashMap<>();
        for (String annotation : annotations) {
            addCollector(annotation);
        }
    }

    public void addCollector(String annotation) {
        collectors.put(annotation, new ArrayList<Class<?>>());
    }

    public void addCollector(String annotation, Collection<Class<?>> collector) {
        collectors.put(annotation, collector);
    }

    public Collection<Class<?>> getCollector(String annotation) {
        return collectors.get(annotation);
    }

    public Map<String, Collection<Class<?>>> getCollectors() {
        return collectors;
    }

    @SuppressWarnings("unchecked")
    public void scan() throws ReflectiveOperationException, IOException {
        Enumeration<URL> urls = bundle.findEntries(packageBase, "*.class", true);
        if (urls == null) {
            return;
        }
        Set<String> annotations = collectors.keySet();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            InputStream in = url.openStream();
            try {
                ClassReader cr = new ClassReader(in);
                AnnotationReader reader = new AnnotationReader(annotations);
                cr.accept(reader, null, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                if (reader.hasResults()) {
                    String cname = reader.getClassName();
                    for (String anno : reader.getResults()) {
                        collectors.get(anno).add(bundle.loadClass(cname));
                    }
                }
            } finally {
                in.close();
            }
        }
    }

    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> result = new HashSet<>();
        for (Collection<Class<?>> c : collectors.values()) {
            result.addAll(c);
        }
        return result;
    }

    public Set<Class<?>> getClasses(String anno) {
        return new HashSet<>(collectors.get(anno));
    }
}
