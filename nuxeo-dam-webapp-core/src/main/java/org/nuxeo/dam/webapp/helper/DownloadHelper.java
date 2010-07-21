/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.helper;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DownloadHelper {

    private static final Log log = LogFactory.getLog(DownloadHelper.class);

    private DownloadHelper() {
        // Helper class
    }

    public static void download(FacesContext context, DocumentModel doc,
            String filePropertyPath, String filename) {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        String bigDownloadURL = BaseURL.getBaseURL(request);
        bigDownloadURL += "nxbigfile" + "/";
        bigDownloadURL += doc.getRepositoryName() + "/";
        bigDownloadURL += doc.getRef().toString() + "/";
        bigDownloadURL += filePropertyPath + "/";
        bigDownloadURL += URIUtils.quoteURIPathComponent(filename, true);
        try {
            response.sendRedirect(bigDownloadURL);
        } catch (IOException e) {
            log.error("Error while redirecting for big file downloader", e);
        }
    }

}
