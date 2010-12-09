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
 *
 */
public abstract class DocumentFieldAccessor {

    protected final String name;

    public static final Map<String, DocumentFieldAccessor> accessors = new HashMap<String, DocumentFieldAccessor>();


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


    public static DocumentFieldAccessor ID = new DocumentFieldAccessor("id") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getId();
        }
    };

    public static DocumentFieldAccessor NAME = new DocumentFieldAccessor("name") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getName();
        }
    };

    public static DocumentFieldAccessor TYPE = new DocumentFieldAccessor("type") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getType();
        }
    };

    public static DocumentFieldAccessor PATH = new DocumentFieldAccessor("path") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getPathAsString();
        }
    };

    public static DocumentFieldAccessor FACETS = new DocumentFieldAccessor("facets") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getFacets();
        }
    };

    public static DocumentFieldAccessor SCHEMAS = new DocumentFieldAccessor("schemas") {
        @Override
        public Object getValue(DocumentModel doc) {
            return doc.getSchemas();
        }
    };

    public static DocumentFieldAccessor SYSTEM = new DocumentFieldAccessor("system") {
        @Override
        public Object getValue(DocumentModel doc) {
            return null; //TODO
        }
    };

}
