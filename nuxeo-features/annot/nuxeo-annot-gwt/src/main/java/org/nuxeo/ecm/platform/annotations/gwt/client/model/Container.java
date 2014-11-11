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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.model;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */

public class Container {

    private String xpath;

    private int offset;

    public static Container fromString(String endContainer) {
        String[] values = endContainer.split(", ");
        return new Container(values[0], Integer.parseInt(values[1]));
    }

    public Container(String xpath, int offset) {
        this.xpath = xpath;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public String getXpath() {
        return xpath;
    }

    public String generateString() {
        return xpath + ", " + offset;
    }

}
