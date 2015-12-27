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
 * Holds a map of document resources, with a document reference as key, and a list of RDF resources as values.
 * <p>
 * Actual statements to manage for export/import are not kept here.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IORelationResources implements IOResources {

    private static final long serialVersionUID = 3613545698356485035L;

    final Map<String, String> namespaces;

    final Map<DocumentRef, Set<Resource>> documentResources;

    final List<Statement> statements;

    public IORelationResources(Map<String, String> namespaces, Map<DocumentRef, Set<Resource>> documentResources,
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
