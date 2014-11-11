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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;

public abstract class AbstractDirectory implements Directory {

    protected DirectoryFieldMapper fieldMapper;

    protected final Map<String, Reference> references = new HashMap<String, Reference>();

    // simple cache system for entry lookups, disabled by default
    protected final DirectoryCache cache = new DirectoryCache();

    /**
     * Invalidate my cache and the caches of linked directories by references.
     */
    public void invalidateCaches() throws DirectoryException {
        cache.invalidateAll();
        for (Reference ref : references.values()) {
            Directory targetDir = ref.getTargetDirectory();
            if (targetDir != null) {
                targetDir.invalidateDirectoryCache();
            }
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
     */
    public void orderEntries(List<DocumentModel> entries,
            Map<String, String> orderBy) throws DirectoryException {
        Collections.sort(entries, new DocumentModelComparator(getSchema(),
                orderBy));
    }

    public DirectoryCache getCache() {
        return cache;
    }

    public void invalidateDirectoryCache() throws DirectoryException{
        getCache().invalidateAll();
    }

}
