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

package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.spaces.api.AbstractUnivers;
import org.nuxeo.ecm.spaces.api.Univers;

public class VirtualUnivers extends AbstractUnivers {

    private final String name;

    public VirtualUnivers(String name) {
        this.name = name;

    }

    public String getDescription() {
        return this.getName();
    }

    public String getId() {
        return "virtual-" + this.getName();
    }

    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return getName();
    }

    public boolean isEqualTo(Univers univers) {
        return univers.getClass() == this.getClass()
                && univers.getName() == getName();
    }

}
