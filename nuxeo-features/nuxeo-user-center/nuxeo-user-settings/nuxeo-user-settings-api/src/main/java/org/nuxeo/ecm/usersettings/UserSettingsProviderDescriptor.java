/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Christophe Capon
 *
 */

package org.nuxeo.ecm.usersettings;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * 
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 * @author Laurent Doguin
 */
@XObject("userSettingsProvider")
public class UserSettingsProviderDescriptor {


    public UserSettingsProviderDescriptor() {
    }

    public UserSettingsProviderDescriptor(String type) {
        this.type = type;
    }

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("@type")
    private String type;

    @XNode("@category")
    private String category = "Default";

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
