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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Element;

/**
 * @author Alexandre Russel
 *
 */
public class CSSClassManager {
    private Element element;
    public CSSClassManager(Element element) {
        this.element = element;
    }
    /**
     * remove the class from the specified element.
     * @return true if the class was removed, false if the class was not found.
     */
    public boolean removeClass(String name) {
        String className = element.getClassName();
        if(isClassPresent(name)) {
            element.setClassName(className.replaceAll("\\b" + name + "\\b\\s*",
                    ""));
            return true;
        }
        return false;
    }
    public boolean isClassPresent(String name) {
        String cssClass = element.getClassName();
        if(cssClass == null) {
            return false;
        }
        return cssClass.matches(".*\\b" + name + "\\b.*");
    }

    /**
     * Add the class to the element.
     * @return true if the class was add, false if the class was already present.
     */
    public boolean addClass(String name) {
        String className = element.getClassName();
        if(!isClassPresent(name)) {
            element.setClassName(className + " " + name);
            return true;
        }
        return false;
    }
}
