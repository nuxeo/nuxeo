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

package org.nuxeo.ecm.core.io.impl.extensions;

import org.dom4j.Element;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.io.ExportExtension;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * Allows to export Lock information as part of the XML strem
 *
 * @since 7.4
 */
public class LockExporterExtension implements ExportExtension {

    @Override
    public void updateExport(DocumentModel docModel, ExportedDocumentImpl result) throws Exception {

        if (docModel.isLocked()) {
            Element lockElement = result.getDocument().getRootElement().addElement("lockInfo");
            Lock lock = docModel.getLockInfo();
            long created = lock.getCreated().getTimeInMillis();
            String owner = lock.getOwner();
            lockElement.addElement("created").setText(String.valueOf(created));
            lockElement.addElement("owner").setText(owner);
        }
    }

}
