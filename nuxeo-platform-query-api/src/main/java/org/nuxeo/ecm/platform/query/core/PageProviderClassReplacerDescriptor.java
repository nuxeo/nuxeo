/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 *
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;

/**
 * @since 5.9.6
 */
@XObject(value = "replacers")
public class PageProviderClassReplacerDescriptor implements
        PageProviderClassReplacerDefinition {

    @XNodeMap(value = "replacer", key = "@withClass", type = HashMap.class, componentType = String.class)
    Map<String, String> replacers = new HashMap<String, String>();

    Map<String, List<String>> replacerMap;

    @XNode("@enabled")
    protected boolean enabled = true;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Map<String, List<String>> getReplacerMap() {
        if (replacerMap == null) {
            replacerMap = new HashMap<>(replacers.size());
            for (String className: replacers.keySet()) {
                String providers = replacers.get(className).replace("," ," ").replace("\n", " ");
                replacerMap.put(className, Arrays.asList(StringUtils.split(providers)));
            }
        }
        return replacerMap;
    }

    @Override
    public void setReplacerMap(Map<String, List<String>> map) {
        replacerMap = map;
    }
}
