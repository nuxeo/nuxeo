/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DocumentViewCodecManagerBean.java 28583 2008-01-08 20:00:27Z sfermigier $
 */

package org.nuxeo.ecm.platform.url.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.api.ejb.DocumentViewCodecManagerLocal;
import org.nuxeo.ecm.platform.url.api.ejb.DocumentViewCodecManagerRemote;
import org.nuxeo.runtime.api.Framework;

/**
 * Document view codec manager bean
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@Stateless
@Local(DocumentViewCodecManagerLocal.class)
@Remote(DocumentViewCodecManagerRemote.class)
public class DocumentViewCodecManagerBean implements DocumentViewCodecManager {

    private static final long serialVersionUID = 7674057886808296899L;

    private static final Log log = LogFactory.getLog(DocumentViewCodecManagerBean.class);

    private DocumentViewCodecManager service;

    @PostConstruct
    public void initialize() {
        try {
            // get Runtime service
            service = Framework.getLocalService(DocumentViewCodecManager.class);
        } catch (Exception e) {
            log.error("Could not get document view codec service", e);
        }
    }

    public void remove() {}

    public String getDefaultCodecName() {
        return service.getDefaultCodecName();
    }

    public DocumentView getDocumentViewFromUrl(String url, boolean hasBaseUrl,
            String baseUrl) {
        return service.getDocumentViewFromUrl(url, hasBaseUrl, baseUrl);
    }

    public DocumentView getDocumentViewFromUrl(String codecName, String url,
            boolean hasBaseUrl, String baseUrl) {
        return service.getDocumentViewFromUrl(codecName, url, hasBaseUrl,
                baseUrl);
    }

    public String getUrlFromDocumentView(DocumentView docView,
            boolean needBaseUrl, String baseUrl) {
        return service.getUrlFromDocumentView(docView, needBaseUrl, baseUrl);
    }

    public String getUrlFromDocumentView(String codecName,
            DocumentView docView, boolean needBaseUrl, String baseUrl) {
        return service.getUrlFromDocumentView(codecName, docView, needBaseUrl,
                baseUrl);
    }

}
