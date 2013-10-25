/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benjamin Jalon
 */

package org.nuxeo.ecm.platform.computedgroups;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * @since 5.7.3
 */
@XObject("documentMetadataGroupComputer")
public class DocumentMetadataGroupComputerDescriptor extends
        GroupComputerDescriptor {

    private static final long serialVersionUID = 1L;

    @XNode("@whereClause")
    public String whereClause = "";

    @XNode("@groupPattern")
    public String groupPattern = "%s";

    @XNode("@xpath")
    public String xpathSelector = "ecm:uuid";

    @XNode("@name")
    public String name;

    @XNode("@enabled")
    public boolean enabled = true;

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        return computerClass.getSimpleName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public GroupComputer getComputer() throws ClientException {
        return new DocumentMetadataGroupComputer(whereClause, groupPattern,
                xpathSelector);
    };

}
