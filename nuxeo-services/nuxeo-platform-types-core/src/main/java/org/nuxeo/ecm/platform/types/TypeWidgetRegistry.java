/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated use the nuxeo-platform-layouts-core module descriptors instead
 */
@Deprecated
public class TypeWidgetRegistry {

    final Map<String, String> typewidgets = new HashMap<String, String>();

    public synchronized void addTypeWidget(TypeWidget typeWidget) {
        String fieldType = typeWidget.getFieldtype();
        // do not add twice an type
        if (!typewidgets.containsKey(fieldType)) {
            typewidgets.put(fieldType, typeWidget.getJsfComponent());
        }
    }

    public synchronized String removeTypeWidget(String fieldType) {
        return typewidgets.remove(fieldType);
    }

    // TODO for tests
    public Map<String, String> getMap() {
        return typewidgets;
    }

}
