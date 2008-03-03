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

package org.nuxeo.ecm.core.search.backend.compass;

import java.util.ArrayList;
import java.util.List;

import org.compass.core.Property;
import org.compass.core.Resource;

/**
 * Helpers for Resource introspection.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public final class ResourceHelper {

    // Utility class.
    private ResourceHelper() {
    }

    public static List<String> getListProperty(Resource r, String propName) {
        Property[] props = r.getProperties(propName);
        List<String> propVals = new ArrayList<String>();
        for (Property propItem : props) {
            propVals.add(propItem.getStringValue());
        }
        return propVals;
    }

}
