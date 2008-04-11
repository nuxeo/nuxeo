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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.api;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.ecm.platform.site.template.SiteObject;

/**
 * interface SiteObject DocumentModel adapters
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */

public interface SiteAwareObject {

    // Http handling methods
    void doGet(SiteRequest request, HttpServletResponse response) throws SiteException;

    void doPut(SiteRequest request, HttpServletResponse response) throws SiteException;

    void doPost(SiteRequest request, HttpServletResponse response) throws SiteException;

    void doDelete(SiteRequest request, HttpServletResponse response) throws SiteException;

    void doHead(SiteRequest request, HttpServletResponse response) throws SiteException;

    boolean needsRendering(SiteRequest request);

    String getId();

    String getName();

    String getSlotId();

    boolean traverse(SiteRequest request, HttpServletResponse response) throws SiteException;

    // Rendering oriented methods
    public SiteObject getSiteConfiguration(SiteRequest request);

    String getURL(SiteRequest request);

    // Content oriented methods
    String getTitle();

    DocumentModelList getChildren() throws ClientException;

    void setSourceDocument(DocumentModel doc);

    DocumentModel getSourceDocument();

}
