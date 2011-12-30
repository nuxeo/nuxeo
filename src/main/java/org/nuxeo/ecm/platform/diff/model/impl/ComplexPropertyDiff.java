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
 *     ataillefer
 */
package org.nuxeo.ecm.platform.diff.model.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.diff.model.PropertyDiff;

/**
 * Implementation of PropertyDiff for a complex property.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class ComplexPropertyDiff implements PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    private Map<String, PropertyDiff> diffMap;

    /**
     * Instantiates a new complex property diff.
     */
    public ComplexPropertyDiff() {
        diffMap = new LinkedHashMap<String, PropertyDiff>();
    }

    public void getDiff(String item) {
        diffMap.get(item);
    }

    public void putDiff(String item, PropertyDiff diff) {
        diffMap.put(item, diff);
    }

    public Map<String, PropertyDiff> getDiffMap() {
        return diffMap;
    }

    public void setDiffMap(Map<String, PropertyDiff> diffMap) {
        this.diffMap = diffMap;
    }
}
