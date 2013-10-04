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
package org.nuxeo.ecm.platform.computedgroups;

/**
 * Group Computer implementing this interface will expose Group with specific
 * label.
 * Group Computer implementing only {@link GroupComputer} will expose group with
 * label is the same as groupId
 *
 * @since 5.7.3
 *
 */
public interface GroupComputerLabelled extends GroupComputer {

    String getLabel(String groupName);

}
