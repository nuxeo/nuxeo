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

package org.nuxeo.opensocial.container.client.external.picture;

import java.util.Map;

import org.nuxeo.opensocial.container.client.external.HasPermissions;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;

/**
 * @author Stéphane Fourrier
 */
public class PictureModel implements HasPermissions {
    private PictureData data;

    private Map<String, Boolean> permissions;

    public PictureModel(PictureData data, Map<String, Boolean> permissions  ) {
        this.data = data;
        this.permissions = permissions;
    }

    public PictureData getData() {
        return data;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public Boolean hasPermission(String permission) {
        return permissions.containsKey(permission);
    }
}
