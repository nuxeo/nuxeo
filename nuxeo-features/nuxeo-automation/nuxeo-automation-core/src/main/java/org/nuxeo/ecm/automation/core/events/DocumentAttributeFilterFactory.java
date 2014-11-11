/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * Create filters that are able to filter documents on their attribute (Regular
 * Doc, Published Doc, Version, Link, Proxy, Immutable, Mutable)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentAttributeFilterFactory {

    public static final String ANY_DOC = "Any";

    public static final String REGULAR_DOC = "Regular Document";

    public static final String LINK_DOC = "Document Link";

    public static final String PUBLISHED_DOC = "Published Document";

    public static final String PROXY_DOC = "Document Proxy";

    public static final String VERSION_DOC = "Document Version";

    public static final String IMMUTABLE_DOC = "Immutable Document";

    public static final String MUTABLE_DOC = "Mutable Document";

    protected static Map<String, Filter> filters = new HashMap<String, Filter>();
    static {
        filters.put(REGULAR_DOC, new RegularDocFilter());
        filters.put(LINK_DOC, new LinkDocFilter());
        filters.put(PUBLISHED_DOC, new PublishedDocFilter());
        filters.put(PROXY_DOC, new ProxyDocFilter());
        filters.put(VERSION_DOC, new VersionDocFilter());
        filters.put(IMMUTABLE_DOC, new ImmutableDocFilter());
        filters.put(MUTABLE_DOC, new MutableDocFilter());
    }

    public static Filter getFilter(String attr) {
        return filters.get(attr);
    }

    static class RegularDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return !doc.isImmutable() && !doc.isProxy();
        }
    }

    static class LinkDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return !doc.isImmutable() && doc.isProxy();
        }
    }

    static class PublishedDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return doc.isImmutable() && doc.isProxy();
        }
    }

    static class ProxyDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return doc.isProxy();
        }
    }

    static class VersionDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return doc.isVersion();
        }
    }

    static class ImmutableDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return doc.isImmutable();
        }
    }

    static class MutableDocFilter implements Filter {
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel doc) {
            return !doc.isImmutable();
        }
    }

}
