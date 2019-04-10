/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
