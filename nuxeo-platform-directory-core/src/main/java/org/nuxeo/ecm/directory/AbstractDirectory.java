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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory;

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;

public abstract class AbstractDirectory implements Directory {

    protected DirectoryFieldMapper fieldMapper;

    protected final Map<String, Reference> references = new HashMap<String, Reference>();

    // simple cache system for entry lookups, disabled by default
    protected final DirectoryCache cache = new DirectoryCache();

    /**
     * Invalidate my cache and the caches of linked directories by references
     * @throws ClientException
     */
    public void invalidateCaches() throws DirectoryException {
        cache.invalidateAll();
        for (Reference ref: references.values()) {
            ref.getTargetDirectory().getCache().invalidateAll();
        }
    }

    public DirectoryFieldMapper getFieldMapper() {
        if (fieldMapper == null) {
            fieldMapper = new DirectoryFieldMapper();
        }
        return fieldMapper;
    }

    public Reference getReference(String referenceFieldName) {
        return references.get(referenceFieldName);
    }

    public boolean isReference(String referenceFieldName) {
        return references.containsKey(referenceFieldName);
    }

    public void addReference(Reference reference) throws ClientException {
        reference.setSourceDirectoryName(getName());
        references.put(reference.getFieldName(), reference);
    }

    public void addReferences(Reference[] references) throws ClientException {
        for (Reference reference : references) {
            addReference(reference);
        }
    }

    public Collection<Reference> getReferences() {
        return references.values();
    }

    /**
     * Helper method to order entries.
     *
     * @param entries the list of entries.
     * @param orderBy an ordered map of field name -> "asc" or "desc".
     * @throws DirectoryException
     */
    public void orderEntries(List<DocumentModel> entries,
            Map<String, String> orderBy) throws DirectoryException {
        Collections.sort(entries, new EntryComparator(getSchema(), orderBy));
    }

    public static final String ORDER_ASC = "asc";

    /**
     * DocumentModel comparator. Uses ordering independent of case or accent. If
     * two values are integers/longs, numbering comparison is used.
     */
    public static class EntryComparator implements Comparator<DocumentModel>,
            Serializable {

        private static final long serialVersionUID = -5027588251188034085L;

        static final Collator collator = Collator.getInstance();

        static {
            collator.setStrength(Collator.PRIMARY); // case+accent independent
        }

        final String schemaName;

        final Map<String, String> orderBy;

        EntryComparator(String schemaName, Map<String, String> orderBy) {
            this.schemaName = schemaName;
            this.orderBy = orderBy;
        }

        public int compare(DocumentModel e1, DocumentModel e2) {
            final DataModel d1 = e1.getDataModel(schemaName);
            final DataModel d2 = e2.getDataModel(schemaName);
            for (Entry<String, String> e : orderBy.entrySet()) {
                final String fieldName = e.getKey();
                final boolean asc = ORDER_ASC.equals(e.getValue());
                final Object v1 = d1.getData(fieldName);
                final Object v2 = d2.getData(fieldName);
                if (v1 == null && v2 == null) {
                    continue;
                } else if (v1 == null) {
                    return asc ? -1 : 1;
                } else if (v2 == null) {
                    return asc ? 1 : -1;
                }
                final int cmp;
                if (v1 instanceof Long && v2 instanceof Long) {
                    cmp = ((Long) v1).compareTo((Long) v2);
                } else if (v1 instanceof Integer && v2 instanceof Integer) {
                    cmp = ((Integer) v1).compareTo((Integer) v2);
                } else {
                    cmp = collator.compare(v1.toString(), v2.toString());
                }
                if (cmp == 0) {
                    continue;
                }
                return asc ? cmp : -cmp;
            }
            // everything being equal, provide consistent ordering
            if (e1.hashCode() == e2.hashCode()) {
                return 0;
            } else if (e1.hashCode() < e2.hashCode()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public DirectoryCache getCache() {
        return cache;
    }
}
