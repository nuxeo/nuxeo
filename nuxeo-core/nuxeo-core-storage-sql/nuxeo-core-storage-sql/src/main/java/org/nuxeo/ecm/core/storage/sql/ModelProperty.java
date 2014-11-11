/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

/**
 * Model of a property (simple or array) of a {@link Node}.
 */
public class ModelProperty {

    public static final ModelProperty NONE = new ModelProperty(
            PropertyType.STRING, "", "", false);

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
