/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public class CSSClassManager {
    private Element element;

    public CSSClassManager(Element element) {
        this.element = element;
    }

    /**
     * remove the class from the specified element.
     *
     * @return true if the class was removed, false if the class was not found.
     */
    public boolean removeClass(String name) {
        String className = element.getClassName();
        if (isClassPresent(name)) {
            element.setClassName(className.replaceAll("\\b" + name + "\\b\\s*", ""));
            return true;
        }
        return false;
    }

    public boolean isClassPresent(String name) {
        String cssClass = element.getClassName();
        if (cssClass == null) {
            return false;
        }
        return cssClass.matches(".*\\b" + name + "\\b.*");
    }

    /**
     * Add the class to the element.
     *
     * @return true if the class was add, false if the class was already present.
     */
    public boolean addClass(String name) {
        String className = element.getClassName();
        if (!isClassPresent(name)) {
            element.setClassName(className + " " + name);
            return true;
        }
        return false;
    }
}
