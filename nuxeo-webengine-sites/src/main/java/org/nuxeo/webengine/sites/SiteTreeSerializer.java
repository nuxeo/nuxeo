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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;

public class SiteTreeSerializer extends JSonTreeSerializer{

    @Override
    public String getUrl(TreeItem item) {
        WebContext ctx = WebEngine.getActiveContext();
        StringBuffer sb = new StringBuffer(ctx.getModulePath());
        DocumentModel d = (DocumentModel)ctx.getUserSession().get(JsonAdapter.ROOT_DOCUMENT);
        if (d != null ) {
            sb.append("/").append(SiteHelper.getString(d, "webc:url", ""));
        }
        sb.append(item.getPath().toString());
        return sb.toString();
    }

}
