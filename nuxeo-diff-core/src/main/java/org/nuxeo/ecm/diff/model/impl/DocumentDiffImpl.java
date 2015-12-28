/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.SchemaDiff;

/**
 * Implementation of DocumentDiff using a HashMap.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffImpl implements DocumentDiff {

    private static final long serialVersionUID = -5117078340766697371L;

    /**
     * Map holding the doc diff.
     * <p>
     * Keys are schema names. Values represent the differences between the fields of the schema.
     */
    private Map<String, SchemaDiff> docDiff;

    /**
     * Instantiates a new document diff impl.
     */
    public DocumentDiffImpl() {
        docDiff = new HashMap<String, SchemaDiff>();
    }

    public Map<String, SchemaDiff> getDocDiff() {
        return docDiff;
    }

    public int getSchemaCount() {
        return docDiff.size();
    }

    public boolean isDocDiffEmpty() {
        return getSchemaCount() == 0;
    }

    public List<String> getSchemaNames() {
        return new ArrayList<String>(docDiff.keySet());
    }

    public SchemaDiff getSchemaDiff(String schema) {
        return docDiff.get(schema);
    }

    public SchemaDiff initSchemaDiff(String schema) {
        SchemaDiff schemaDiff = new SchemaDiffImpl();
        docDiff.put(schema, schemaDiff);
        return schemaDiff;
    }

}
