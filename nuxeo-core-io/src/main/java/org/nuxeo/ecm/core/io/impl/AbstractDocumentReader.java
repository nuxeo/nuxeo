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
 * $Id: AbstractDocumentReader.java 30477 2008-02-22 10:02:15Z dmihalache $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.ExportedDocument;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractDocumentReader implements DocumentReader {

    private static final Log log = LogFactory.getLog(AbstractDocumentReader.class);

    // this abstract method is needed
    @Override
    public abstract ExportedDocument read() throws IOException;

    @Override
    public ExportedDocument[] read(int count) throws IOException {
        List<ExportedDocument> docs = new ArrayList<ExportedDocument>(count);
        for (int i = 0; i < count; i++) {
            ExportedDocument doc = read();
            if (doc  == null) {
                break;
            }

            /*NXP-1688 Rux: no ID, it should be a OS folder and not an exported one*/
            if (doc.getId() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding document to be transformed (path): "
                            + doc.getPath());
                }
                docs.add(doc);
            }
            else {
                log.warn("no ID for document, won't add " + doc);
            }
        }
        if (docs.isEmpty()) {
            return null;
        }
        return docs.toArray(new ExportedDocument[docs.size()]);
    }

}
