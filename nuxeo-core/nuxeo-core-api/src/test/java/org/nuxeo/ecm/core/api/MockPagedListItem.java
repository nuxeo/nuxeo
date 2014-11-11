/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;

/**
 * @author Anahide Tchertchian
 *
 */
public class MockPagedListItem implements Serializable {

    private static final long serialVersionUID = 1L;

    final String name;

    final long position;

    public MockPagedListItem(String name, long position) {
        super();
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public long getPosition() {
        return position;
    }

}
