/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory.ui;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;

/**
 * Directory ui descriptor
 *
 * @author Anahide Tchertchian
 *
 */
@XObject("directory")
public class DirectoryUIDescriptor implements DirectoryUI {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("@layout")
    String layout;

    @XNode("@view")
    String view;

    @XNode("@sortField")
    String sortField;

    public String getName() {
        return name;
    }

    public String getLayout() {
        return layout;
    }

    public String getView() {
        return view;
    }

    public String getSortField() {
        return sortField;
    }

}
