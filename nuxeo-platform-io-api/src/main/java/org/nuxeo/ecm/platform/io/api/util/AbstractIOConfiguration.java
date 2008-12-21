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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.io.api.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractIOConfiguration implements IOConfiguration, Serializable  {

    private static final long serialVersionUID = 1L;

    protected final List<DocumentRef> docRefs;
    protected String repositoryName;
    protected final Map<String,Object> properties;

    protected AbstractIOConfiguration() {
        docRefs = new ArrayList<DocumentRef>();
        properties = new HashMap<String, Object>();
    }

    public Collection<DocumentRef> getDocuments() {
        return docRefs;
    }

    public DocumentRef getFirstDocument() {
        if (docRefs.isEmpty()) {
            return null;
        }
        return docRefs.get(0);
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void addDocument(DocumentRef docRef) {
        docRefs.add(docRef);
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    public Map<String,Object > getProperties() {
        return properties;
    }

}
