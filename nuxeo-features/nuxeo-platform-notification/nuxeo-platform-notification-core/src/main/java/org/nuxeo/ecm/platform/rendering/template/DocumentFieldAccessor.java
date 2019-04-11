/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class DocumentFieldAccessor {

    protected final String name;

    public static final Map<String, DocumentFieldAccessor> accessors = new HashMap<>();

    protected DocumentFieldAccessor(String name) {
        this.name = name;
        accessors.put(name, this);
    }

    public static DocumentFieldAccessor get(String name) {
        return accessors.get(name);
    }

    public static void put(DocumentFieldAccessor accessor) {
        accessors.put(accessor.name, accessor);
    }

    public static Collection<String> getFieldNames() {
        return accessors.keySet();
    }

    public static Collection<DocumentFieldAccessor> getAcessors() {
        return accessors.values();
    }

    public static int getAcessorsCount() {
        return accessors.size();
    }

    public String getName() {
        return name;
    }

    public abstract Object getValue(DocumentModel doc);

    public static final DocumentFieldAccessor ID = new DocumentFieldAccessor("id") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getId();
        }
    };

    public static final DocumentFieldAccessor NAME = new DocumentFieldAccessor("name") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getName();
        }
    };

    public static final DocumentFieldAccessor TYPE = new DocumentFieldAccessor("type") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getType();
        }
    };

    public static final DocumentFieldAccessor PATH = new DocumentFieldAccessor("path") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getPathAsString();
        }
    };

    public static final DocumentFieldAccessor FACETS = new DocumentFieldAccessor("facets") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getFacets();
        }
    };

    public static final DocumentFieldAccessor SCHEMAS = new DocumentFieldAccessor("schemas") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getSchemas();
        }
    };

    public static final DocumentFieldAccessor SYSTEM = new DocumentFieldAccessor("system") {
        @Override
        public Object getValue(DocumentModel doc) {
            return null; // TODO
        }
    };

}
