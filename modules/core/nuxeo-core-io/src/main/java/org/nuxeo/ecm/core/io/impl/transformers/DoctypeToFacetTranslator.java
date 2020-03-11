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
 * Migrate a DocumentType to a facet
 *
 * @since 7.4
 */
public class DoctypeToFacetTranslator implements DocumentTransformer {

    protected final String docType;

    protected final String newDocType;

    protected final String facet;

    public DoctypeToFacetTranslator(String docType, String newDocType, String facet) {
        this.docType = docType;
        this.newDocType = newDocType;
        this.facet = facet;
    }

    @Override
    public boolean transform(ExportedDocument xdoc) throws IOException {
        if (xdoc.getType().equals(docType)) {
            Element root = xdoc.getDocument().getRootElement();
            Element sys = root.element("system");
            sys.element("type").setText(newDocType);
            sys.addElement("facet").setText(facet);
        }
        return true;
    }

}
