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

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

public class SiteTreeSerializer extends JSonTreeSerializer {

    @Override
    public String getUrl(TreeItem item) {
        WebContext ctx = WebEngine.getActiveContext();
        StringBuilder sb = new StringBuilder(SiteUtils.getWebContainersPath());
        DocumentModel doc = (DocumentModel) ctx.getUserSession().get(
                JsonAdapter.ROOT_DOCUMENT);
        if (doc != null) {
            sb.append('/').append(SiteUtils.getString(doc,
                    SiteConstants.WEBCONTAINER_URL, ""));
        }
        sb.append(URIUtils.quoteURIPathComponent(item.getPath().toString(),
                false));
        return sb.toString();
    }

}
