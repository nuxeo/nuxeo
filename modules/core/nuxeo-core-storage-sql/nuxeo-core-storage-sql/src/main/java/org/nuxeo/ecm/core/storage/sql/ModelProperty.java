/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
     * Creates a model for a scalar property, or the last segment of a complex property.
     */
    public ModelProperty(PropertyType propertyType, String fragmentName, String fragmentKey, boolean readonly) {
        this.propertyType = propertyType;
        this.fragmentName = fragmentName;
        this.fragmentKey = fragmentKey;
        this.readonly = readonly;
        isIntermediateSegment = false;
        // TODO use some config to decide this
        fulltext = (propertyType.equals(PropertyType.STRING) || propertyType.equals(PropertyType.BINARY) || propertyType.equals(PropertyType.ARRAY_STRING))
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
     * @param propertyName the canonical segment name
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
     * Checks if this is a pseudo-model for an intermediate segment of a complex property.
     *
     * @since 5.7.3
     */
    public boolean isIntermediateSegment() {
        return isIntermediateSegment;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + fragmentName + ", " + fragmentKey + ", " + propertyType
                + (readonly ? ", RO" : "") + (fulltext ? ", FT" : "") + ')';
    }

}
