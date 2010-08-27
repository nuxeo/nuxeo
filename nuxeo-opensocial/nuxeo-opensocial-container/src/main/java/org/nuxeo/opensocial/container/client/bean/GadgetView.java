/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.container.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GadgetView implements IsSerializable {

    private String view;

    private String contentType;

    public GadgetView() {
    }

    public GadgetView(String view, String contentType) {
        this.view = view;
        this.contentType = contentType;
    }

    public String getView() {
        return view;
    }

    public String getContentType() {
        return contentType;
    }

}
