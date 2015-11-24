/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.utils;

import com.google.gwt.user.client.Element;

public class ElementUtils {
    // This method is a way to remove all class style (by giving a class prefix)
    // from an element.
    public static void removeStyle(Element element, String stylePrefix) {
        String temp = element.getClassName();
        for (String classStyle : temp.split(" ")) {
            if (classStyle.startsWith(stylePrefix)) {
                temp = temp.replace(classStyle, "");
            }
        }
        element.setClassName(temp);
    }
}
