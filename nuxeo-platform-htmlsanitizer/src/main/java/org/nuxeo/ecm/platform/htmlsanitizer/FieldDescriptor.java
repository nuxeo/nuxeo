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

package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "field")
public class FieldDescriptor {

    @XContent
    private String contentField;

    @XNode("@filter")
    private String filterField;

    @XNode("@filterValue")
    private String filterValue;

    @XNode("@sanitize")
    private boolean sanitize = true;

    public String getContentField() {
        if (contentField != null) {
            String result = contentField.trim();
            result = result.replace("\n", "");
            return result;
        }
        return contentField;
    }

    public String getFilterField() {
        return filterField;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public boolean doSanitize() {
        return sanitize;
    }

    @Override
    public String toString() {
        if (filterField != null) {
            return contentField + "if " + filterField + "=" + filterValue;
        } else {
            return contentField;
        }
    }

}
