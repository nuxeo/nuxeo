/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.rpc.webcontent.result;

import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContentResult implements Result {
    private static final long serialVersionUID = 1L;

    private WebContentData data;

    private Map<String, Boolean> permissions;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialisation purpose only
    private CreateWebContentResult() {
    }

    public CreateWebContentResult(WebContentData data,
            Map<String, Boolean> permissions) {
        this.data = data;
        this.permissions = permissions;
    }

    public WebContentData getData() {
        return data;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }
}
