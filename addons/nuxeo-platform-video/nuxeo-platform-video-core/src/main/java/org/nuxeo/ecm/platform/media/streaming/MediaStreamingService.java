/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
package org.nuxeo.ecm.platform.media.streaming;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author Benjamin JALON <bjalon@nuxeo.com>
 *
 */
public interface MediaStreamingService {

    boolean isServiceActivated();

    String getStreamingServerBaseURL();

    String getStreamURLFromDocumentModel(DocumentModel mediaDoc)
            throws ClientException;

    boolean isStreamableMedia(DocumentModel doc);

}
