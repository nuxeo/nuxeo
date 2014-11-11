/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

/**
 * Model of a property.
 */
public class ModelProperty {

    public final PropertyType propertyType;

    public final String fragmentName;

    public final String fragmentKey;

    public final boolean readonly;

    public final boolean fulltext;

    public ModelProperty(PropertyType propertyType, String fragmentName,
            String fragmentKey, boolean readonly) {
        this.propertyType = propertyType;
        this.fragmentName = fragmentName;
        this.fragmentKey = fragmentKey;
        this.readonly = readonly;
        // TODO use some config to decide this
        fulltext = (propertyType.equals(PropertyType.STRING)
                || propertyType.equals(PropertyType.BINARY) || propertyType.equals(PropertyType.ARRAY_STRING))
                && (fragmentKey == null || !fragmentKey.equals(Model.MAIN_KEY))
                && !fragmentName.equals(Model.HIER_TABLE_NAME)
                && !fragmentName.equals(Model.MAIN_TABLE_NAME)
                && !fragmentName.equals(Model.VERSION_TABLE_NAME)
                && !fragmentName.equals(Model.PROXY_TABLE_NAME)
                && !fragmentName.equals(Model.FULLTEXT_TABLE_NAME)
                && !fragmentName.equals(Model.LOCK_TABLE_NAME)
                && !fragmentName.equals(Model.UID_SCHEMA_NAME)
                && !fragmentName.equals(Model.MISC_TABLE_NAME);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + fragmentName + ", "
                + fragmentKey + ", " + propertyType + (readonly ? ", RO" : "")
                + (fulltext ? ", FT" : "") + ')';
    }

}
