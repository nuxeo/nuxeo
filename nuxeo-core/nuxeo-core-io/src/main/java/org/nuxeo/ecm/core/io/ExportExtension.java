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

package org.nuxeo.ecm.core.io;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * Interface for extension used to enrich the export
 *
 * @since 7.4
 */
public interface ExportExtension {

    void updateExport(DocumentModel docModel, ExportedDocumentImpl result) throws Exception;

}
