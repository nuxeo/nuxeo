/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeException;

/**
 * Static helper methods for document models.
 *
 * @deprecated unused
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Deprecated
public final class DocumentHelper {

    private DocumentHelper() {
    }

    /**
     * Fills a DocumentModel with data from a given data map.
     *
     * @deprecated unused
     */
    @Deprecated
    public static void loadData(DocumentModel doc, String schemaName,
            Map<String, Object> dataMap) throws TypeException {
        DocumentType docType = doc.getDocumentType();

        Schema schema = docType.getSchema(schemaName);
        if (schema == null) {
            throw new IllegalArgumentException("No such schema: " + schemaName);
        }

        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String name = entry.getKey();
            Field field = schema.getField(name);
            if (field == null) {
                throw new IllegalArgumentException("No such field: " + name
                        + " in schema " + schemaName);
            }
            Type fieldType = field.getType();

            entry.setValue(fieldType.convert(entry.getValue()));
        }
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static void loadData(DocumentModel doc, Map<String, Object> dataMap)
            throws TypeException {
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            loadData(doc, entry.getKey(),
                    (Map<String, Object>) entry.getValue());
        }
    }

}
