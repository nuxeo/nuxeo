/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.api;

import java.util.EnumSet;

/**
 * Enum for types on Content associated to a {@link TemplateInput}
 * 
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public enum ContentInputType {

    HtmlPreview("htmlPreview"), BlobContent("blobContent");

    private final String value;

    ContentInputType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ContentInputType getByValue(String value) {
        ContentInputType returnValue = null;
        for (final ContentInputType element : EnumSet.allOf(ContentInputType.class)) {
            if (element.toString().equals(value)) {
                returnValue = element;
            }
        }
        return returnValue;
    }
}
