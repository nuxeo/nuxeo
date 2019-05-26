/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        List<String> items = new ArrayList<>();

        items.add(getPrefix());
        items.add(viewId);

        return null;
    }

}
