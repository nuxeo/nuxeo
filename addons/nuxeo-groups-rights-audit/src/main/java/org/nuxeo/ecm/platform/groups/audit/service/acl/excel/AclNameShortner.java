/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.excel;

import java.util.Set;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Holds a short name translation for each existing ACL. The implementation based on {@link HashBiMap} ensures no two
 * short name can coexist. Any attempt to register a permission short name with an existing one will lead to an
 * {@link IllegalArgumentException}.
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AclNameShortner {
    protected BiMap<String, String> mapping;
    {
        mapping = HashBiMap.create();

        mapping.put(SecurityConstants.EVERYTHING, "A");

        mapping.put(SecurityConstants.BROWSE, "B");

        mapping.put(SecurityConstants.READ, "R");
        mapping.put(SecurityConstants.READ_CHILDREN, "RC");
        mapping.put(SecurityConstants.READ_LIFE_CYCLE, "RL");
        mapping.put(SecurityConstants.READ_PROPERTIES, "RP");
        mapping.put(SecurityConstants.READ_SECURITY, "RS");
        mapping.put(SecurityConstants.READ_VERSION, "RV");

        mapping.put(SecurityConstants.READ_WRITE, "RW");

        mapping.put(SecurityConstants.WRITE, "W");
        mapping.put(SecurityConstants.WRITE_LIFE_CYCLE, "WL");
        mapping.put(SecurityConstants.WRITE_PROPERTIES, "WP");
        mapping.put(SecurityConstants.WRITE_SECURITY, "WS");
        mapping.put(SecurityConstants.WRITE_VERSION, "WV");

        mapping.put(SecurityConstants.ADD_CHILDREN, "AC");
        mapping.put(SecurityConstants.REMOVE_CHILDREN, "DC");

        mapping.put(SecurityConstants.MANAGE_WORKFLOWS, "MW");
        mapping.put(SecurityConstants.VIEW_WORKLFOW, "VW");

        mapping.put(SecurityConstants.RESTRICTED_READ, "RR");
        mapping.put(SecurityConstants.UNLOCK, "U");
        mapping.put(SecurityConstants.VERSION, "V");
        mapping.put(SecurityConstants.REMOVE, "RE");
    }

    /**
     * Return the short name of a given permission.
     *
     * @throws an IllegalArgumentException if the permission is unknown.
     */
    public String getShortName(String permission) {
        if (!mapping.containsKey(permission)) {
            // Generate one with capitalized letters
            String s = permission.replaceAll("[a-z\\s]", "");
            String shortName = s;
            int index = 1;
            while (mapping.values().contains(shortName)) {
                shortName = s + index;
                index++;
            }
            mapping.put(permission, shortName);
        }

        return mapping.get(permission);
    }

    public String getFullName(String shortname) {
        return mapping.inverse().get(shortname);
    }

    public void register(String permission, String shortname) {
        mapping.put(permission, shortname);
    }

    public BiMap<String, String> getMapping() {
        return mapping;
    }

    public Set<String> getShortNames() {
        return getMapping().inverse().keySet();
    }

    public Set<String> getFullNames() {
        return getMapping().keySet();
    }
}
