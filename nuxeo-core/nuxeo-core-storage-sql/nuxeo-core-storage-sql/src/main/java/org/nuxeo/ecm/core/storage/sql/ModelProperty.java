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

    public final PropertyType propertyType;

    public final String fragmentName;

    public final String fragmentKey;

    public final boolean readonly;

    public final boolean fulltext;

    protected final boolean isIntermediateSegment;

    /**
     * Creates a model for a scalar property, or the last segment of a complex
     * property.
     */
    public ModelProperty(PropertyType propertyType, String fragmentName,
            String fragmentKey, boolean readonly) {
        this.propertyType = propertyType;
        this.fragmentName = fragmentName;
        this.fragmentKey = fragmentKey;
        this.readonly = readonly;
        isIntermediateSegment = false;
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

    /**
     * Create a model for an intermediate segment of a complex property.
     *
     * @param name the canonical segment name
     *
     * @since 5.7.3
     */
    public ModelProperty(String propertyName) {
        this.fragmentName = propertyName;
        propertyType = null;
        fragmentKey = "";
        readonly = false;
        fulltext = false;
        isIntermediateSegment = true;
    }

    /**
     * Gets the segment name for an intermediate segment.
     *
     * @return the segment name
     * @since 5.7.3
     */
    public String getIntermediateSegment() {
        return fragmentName;
    }

    /**
     * Checks if this is a pseudo-model for an intermediate segment of a complex
     * property.
     *
     * @since 5.7.3
     */
    public boolean isIntermediateSegment() {
        return isIntermediateSegment;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + fragmentName + ", "
                + fragmentKey + ", " + propertyType + (readonly ? ", RO" : "")
                + (fulltext ? ", FT" : "") + ')';
    }

}
