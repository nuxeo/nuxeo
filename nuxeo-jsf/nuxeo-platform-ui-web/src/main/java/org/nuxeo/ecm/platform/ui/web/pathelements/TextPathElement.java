/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.pathelements;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class TextPathElement implements PathElement {

    public static final String TYPE = "TextPathElement";

    private static final long serialVersionUID = -2697484542006976062L;

    private final String name;

    public TextPathElement(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return TYPE;
    }

    public boolean isLink() {
        return false;
    }

}
