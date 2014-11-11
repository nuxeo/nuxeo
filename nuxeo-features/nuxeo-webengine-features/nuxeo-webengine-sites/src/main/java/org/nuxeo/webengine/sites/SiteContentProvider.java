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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.webengine.sites;

import java.util.Collections;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.ui.tree.document.DocumentContentProvider;
import org.nuxeo.webengine.sites.utils.SiteConstants;

/**
 * Implementation of provider for the tree.
 */
public class SiteContentProvider extends DocumentContentProvider {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SiteContentProvider.class);

    public SiteContentProvider(CoreSession session) {
        super(session);
    }

    @Override
    public Object[] getChildren(Object obj) {
        DocumentModel parent = (DocumentModel) obj;

        String query = "SELECT * FROM Document WHERE ecm:parentId = '"
                + parent.getId()
                + "' AND "
                + SiteConstants.WEBPAGE_PUSHTOMENU + " = 1 AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title";
        try {
            DocumentModelList docs = session.query(query);
            return docs.toArray();
        } catch (ClientException e) {
            log.error(e);
        }
        return Collections.EMPTY_LIST.toArray();
    }

    @Override
    public String getLabel(Object obj) {
        String label = super.getLabel(obj);
        return label == null ? null : StringEscapeUtils.escapeXml(label);
    }

}
