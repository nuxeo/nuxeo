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

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class NullRangeXPointer implements XPointer {

    private String xpointerString;

    public NullRangeXPointer(String xpointer) {
        this.xpointerString = xpointer;
    }

    public String getMethod() {
        return null;
    }

    public String getUrl() {
        return null;
    }

    public String getXPath() {
        return null;
    }

    public String getXpointerString() {
        return xpointerString;
    }

}
