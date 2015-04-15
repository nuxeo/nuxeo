/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nc@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.codec;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;

/**
 * @since 7.3
 */
public class DocumentModelCodec extends
        AbstractMarshallingRegistryCodec<DocumentModel> {

    public DocumentModelCodec() {
        super(DocumentModel.class, "document", DocumentModelJsonReader.class,
                DocumentModelJsonWriter.class);
    }
}
