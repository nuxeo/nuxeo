/*
 * Copyright (c) 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.io.impl.transformers;

import java.io.IOException;

import org.dom4j.Element;
import org.nuxeo.ecm.core.io.DocumentTransformer;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * Removes a Schema
 *
 * @since 7.4
 */
public class SchemaRemover implements DocumentTransformer {

    protected final String docType;

    protected final String schema;

    public SchemaRemover(String docType, String schema) {
        this.docType = docType;
        this.schema = schema;
    }

    @Override
    public boolean transform(ExportedDocument xdoc) throws IOException {
        if (docType == null || xdoc.getType().equals(docType)) {
            Element root = xdoc.getDocument().getRootElement();
            for (Object f : root.elements("schema")) {
                if (schema.equals(((Element) f).attribute("name").getText())) {
                    ((Element) f).detach();
                }
            }
        }
        return true;
    }
}
