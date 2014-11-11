/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client.bean;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * PreferencesBean
 * 
 * @author Guillaume Cusnieux
 */
public class PreferencesBean implements IsSerializable {

    private String dataType;

    private String defaultValue;

    private String displayName;

    private List<ValuePair> enumValues;

    private String name;

    private String value;

    /**
     * Default construcor (Specification of Gwt)
     */
    public PreferencesBean() {
    }

    public PreferencesBean(String dataType, String defaultValue,
            String displayName, List<ValuePair> enumValues, String name,
            String value) {
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.displayName = displayName;
        this.enumValues = enumValues;
        this.name = name;
        this.value = value;
    }

    public String getDataType() {
        return this.dataType;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public List<ValuePair> getEnumValues() {
        return this.enumValues;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
