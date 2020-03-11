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
import java.util.List;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.nuxeo.ecm.core.io.DocumentTransformer;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * Renames a schema
 *
 * @since 7.4
 */
public class SchemaRenamer implements DocumentTransformer {

    protected final String srcSchema;

    protected final String dstSchema;

    protected final String dstPrefix;

    public SchemaRenamer(String srcSchema, String dstSchema, String dstPrefix) {
        this.srcSchema = srcSchema;
        this.dstSchema = dstSchema;
        this.dstPrefix = dstPrefix;
    }

    @Override
    public boolean transform(ExportedDocument xdoc) throws IOException {
        Element root = xdoc.getDocument().getRootElement();

        List<Element> schemas = root.elements("schema");
        if (schemas != null) {
            for (Element schema : schemas) {
                String name = schema.attribute("name").getText();
                if (srcSchema.equalsIgnoreCase(name)) {
                    Namespace ns = new Namespace(dstPrefix, "http://www.nuxeo.org/ecm/schemas/" + dstSchema);
                    schema.add(ns);
                    schema.setAttributeValue("name", dstSchema);
                    List<Element> fields = schema.elements();
                    for (Element field : fields) {
                        field.setQName(new QName(field.getName(), ns));
                    }
                }
            }
        }
        return true;
    }
}
