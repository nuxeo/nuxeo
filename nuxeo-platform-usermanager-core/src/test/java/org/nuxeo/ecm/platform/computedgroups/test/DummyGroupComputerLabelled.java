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
 *     Benjamin JALON<bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.platform.computedgroups.test;

import org.nuxeo.ecm.platform.computedgroups.GroupComputerLabelled;

/**
 * @since 5.7.3
 *
 */
public class DummyGroupComputerLabelled extends DummyGroupComputer implements
        GroupComputerLabelled {

    @Override
    public String getLabel(String groupName) {
        if ("Grp1".equals(groupName)) {
            return "Groupe 1";
        }
        if ("Grp2".equals(groupName)) {
            return "Groupe 2";
        }
        return null;
    }
}
