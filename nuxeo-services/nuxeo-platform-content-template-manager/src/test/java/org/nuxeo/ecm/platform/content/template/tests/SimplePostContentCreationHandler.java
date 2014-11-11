/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.content.template.tests;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.service.PostContentCreationHandler;

/**
 * Simple {@link PostContentCreationHandler} creating only one document in the
 * root.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class SimplePostContentCreationHandler implements
        PostContentCreationHandler {

    public static final String DOC_TYPE = "Domain";

    public static final String DOC_NAME = "postContentCreationDoc";

    @Override
    public void execute(CoreSession session) {
        try {
            DocumentModel root = session.getRootDocument();
            DocumentModel doc = session.createDocumentModel(
                    root.getPathAsString(), DOC_NAME, DOC_TYPE);
            session.createDocument(doc);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

}
