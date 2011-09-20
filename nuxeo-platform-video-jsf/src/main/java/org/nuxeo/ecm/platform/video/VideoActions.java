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
package org.nuxeo.ecm.platform.video;

import java.io.Serializable;
import java.util.Calendar;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 *
 */
@Name("videoActions")
@Install(precedence = Install.FRAMEWORK)
public class VideoActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public String getURLForPlayer(DocumentModel videoDoc)
            throws ClientException {
        return DocumentModelFunctions.bigFileUrl(videoDoc, "file:content",
                "file:filename");
    }

    public String getURLForStaticPreview(DocumentModel videoDoc)
            throws ClientException {
        String lastModification = ""
                + (((Calendar) videoDoc.getPropertyValue("dc:modified")).getTimeInMillis());
        return DocumentModelFunctions.fileUrl("downloadPicture", videoDoc,
                "StaticPlayerView:content", lastModification);
    }

}
