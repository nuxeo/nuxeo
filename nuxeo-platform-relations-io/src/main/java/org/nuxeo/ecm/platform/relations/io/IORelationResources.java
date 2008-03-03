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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IORelationResources.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.io.api.IOResources;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;

/**
 * IO Resources for relations
 * <p>
 * Holds a map of document resources, with a document reference as key, and a
 * list of RDF resources as values.
 * <p>
 * Actual statements to manage for export/import are not kept here.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class IORelationResources implements IOResources {

    private static final long serialVersionUID = 3613545698356485035L;

    final Map<String, String> namespaces;

    final Map<DocumentRef, Set<Resource>> documentResources;

    final List<Statement> statements;

    public IORelationResources(Map<String, String> namespaces,
            Map<DocumentRef, Set<Resource>> documentResources,
            List<Statement> statements) {
        this.namespaces = namespaces;
        this.documentResources = documentResources;
        this.statements = statements;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public Map<DocumentRef, Set<Resource>> getResourcesMap() {
        return Collections.unmodifiableMap(documentResources);
    }

    public Set<Resource> getDocumentResources(DocumentRef docRef) {
        Set<Resource> res = documentResources.get(docRef);
        if (res != null) {
            return Collections.unmodifiableSet(res);
        }
        return null;
    }

    public List<Statement> getStatements() {
        return statements;
    }

}
