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

package org.nuxeo.ecm.admin.url;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * Should provide a url binding for admin screens
 *
 * @author tiry
 */
public class AdminViewCodec extends AbstractDocumentViewCodec {

    public static final String PREFIX = "nxadm";

    // url = /nxadm/viewId/tab/subTab

    public AdminViewCodec() {
    }

    public AdminViewCodec(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        String viewId = docView.getViewId();
        List<String> items = new ArrayList<String>();

        items.add(getPrefix());
        items.add(viewId);

        return null;
    }

}
