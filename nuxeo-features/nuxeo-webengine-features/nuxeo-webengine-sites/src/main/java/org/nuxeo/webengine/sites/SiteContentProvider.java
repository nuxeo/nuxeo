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

import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.ui.tree.document.DocumentContentProvider;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

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
        Object[] objects = super.getChildren(obj);
        List<Object> v = new Vector<Object>();
        for (Object o : objects) {
            DocumentModel doc = (DocumentModel) o;
            // filter pages
            // WEB-214
            try {
                if (SiteUtils.getBoolean(doc, SiteConstants.WEBPAGE_PUSHTOMENU, false)
                        && !SiteConstants.DELETED.equals(doc.getCurrentLifeCycleState())) {
                    v.add(doc);
                }
            } catch (ClientException e) {
                log.error(e);
            }
        }
        return v.toArray();
    }

    @Override
    public String getLabel(Object obj) {
        String label = super.getLabel(obj);
        return label == null ? null : StringEscapeUtils.escapeXml(label);
    }

}
