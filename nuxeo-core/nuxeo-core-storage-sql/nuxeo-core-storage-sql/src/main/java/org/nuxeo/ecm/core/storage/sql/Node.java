/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.storage.StateAccessor;

/**
 * A {@code Node} implementation. The actual data is stored in contained objects that are {@link Fragment}s.
 */
public class Node implements StateAccessor {

    /** The persistence context used. */
    private final PersistenceContext context;

    private final Model model;

    /** The hierarchy/main fragment. */
    protected final SimpleFragment hierFragment;

    /** Fragment information for each additional mixin or inherited fragment. */
    protected final FragmentsMap fragments;

    /**
     * Path, only for immediate consumption after construction (will be reset to null afterwards).
     */
    protected String path;

    /**
     * Cache of property objects already retrieved. They are dumb objects, just providing an indirection to an
     * underlying {@link Fragment}.
     */
    private final Map<String, BaseProperty> propertyCache;

    private Boolean isVersion;

    /**
     * Creates a Node.
     *
     * @param context the persistence context
     * @param fragmentGroup the group of fragments for the node
     * @param path the path, if known at construction time
     */
    @SuppressWarnings("unchecked")
    protected Node(PersistenceContext context, FragmentGroup fragmentGroup, String path) {
        this.context = context;
        model = context.model;
        hierFragment = fragmentGroup.hier;
        if (fragmentGroup.fragments == null) {
            fragments = new FragmentsMap();
        } else {
            fragments = fragmentGroup.fragments;
        }
        this.path = path;
        // memory-sensitive
        propertyCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    // ----- basics -----

    /**
     * Gets the node unique id, usually a Long or a String.
     *
     * @return the node id
     */
    public Serializable getId() {
        /*
         * We don't cache the id as it changes between the initial creation and the first save.
         */
        return hierFragment.getId();
    }

    public String getName() {
        return getHierFragment().getString(model.HIER_CHILD_NAME_KEY);
    }

    public Long getPos() {
        return (Long) getHierFragment().get(model.HIER_CHILD_POS_KEY);
    }

    public String getPrimaryType() {
        return hierFragment.getString(model.MAIN_PRIMARY_TYPE_KEY);
    }

    public Serializable getParentId() {
        return getHierFragment().get(model.HIER_PARENT_KEY);
    }

    /**
     * Gets the path that was assigned at {@link Node} construction time. Then it's reset to {@code null}. Should only
     * be used once.
     *
     * @return the path, or {@code null} for unknown
     */
    public String getPath() {
        String p = path;
        if (p != null) {
            path = null;
        }
        return p;
    }

    protected SimpleFragment getHierFragment() {
        return hierFragment;
    }

    // cache the isVersion computation
    public boolean isVersion() {
        if (isVersion == null) {
            isVersion = (Boolean) getSimpleProperty(model.MAIN_IS_VERSION_PROP).getValue();
            if (isVersion == null) {
                isVersion = Boolean.FALSE;
            }
        }
        return isVersion.booleanValue();
    }

    public boolean isProxy() {
        String primaryType = getPrimaryType();
        if (primaryType == null) {
            throw new NullPointerException(this.toString());
        }
        return primaryType.equals(model.PROXY_TYPE);
    }

    public boolean isRecord() {
        return Boolean.TRUE.equals(getSimpleProperty(model.MAIN_IS_RECORD_PROP).getValue());
    }

    private static final String[] NO_MIXINS = {};

    /**
     * Gets the instance mixins. Mixins from the type are not returned.
     * <p>
     * Never returns {@code null}.
     */
    public String[] getMixinTypes() {
        String[] value = (String[]) hierFragment.get(model.MAIN_MIXIN_TYPES_KEY);
        return value == null ? NO_MIXINS : value.clone();
    }

    /**
     * Gets the mixins. Includes mixins from the type. Returns a fresh set.
     */
    public Set<String> getAllMixinTypes() {
        // linked for deterministic result
        Set<String> mixins = new LinkedHashSet<String>(model.getDocumentTypeFacets(getPrimaryType()));
        mixins.addAll(Arrays.asList(getMixinTypes()));
        return mixins;
    }

    /**
     * Checks the mixins. Includes mixins from the type.
     */
    public boolean hasMixinType(String mixin) {
        if (model.getDocumentTypeFacets(getPrimaryType()).contains(mixin)) {
            return true; // present in type
        }
        for (String m : getMixinTypes()) {
            if (m.equals(mixin)) {
                return true; // present in node
            }
        }
        return false;
    }

    /**
     * Clears the properties cache, used when removing mixins.
     */
    protected void clearCache() {
        // some properties have now become invalid
        propertyCache.clear();
    }

    // ----- properties -----

    /**
     * Gets a simple property from the node, given its name.
     *
     * @param name the property name
     * @return the property
     * @throws PropertyNotFoundException if the name is invalid
     */
    public SimpleProperty getSimpleProperty(String name) {
        SimpleProperty property = (SimpleProperty) propertyCache.get(name);
        if (property == null) {
            ModelProperty propertyInfo = getPropertyInfo(name);
            if (propertyInfo == null) {
                throw new PropertyNotFoundException(name);
            }
            property = makeSimpleProperty(name, propertyInfo);
            propertyCache.put(name, property);
        }
        return property;
    }

    protected SimpleProperty makeSimpleProperty(String name, ModelProperty propertyInfo) {
        String fragmentName = propertyInfo.fragmentName;
        Fragment fragment = fragments.get(fragmentName);
        if (fragment == null) {
            // lazy fragment, fetch from session
            RowId rowId = new RowId(fragmentName, getId());
            fragment = context.get(rowId, true);
            fragments.put(fragmentName, fragment);
        }
        return new SimpleProperty(name, propertyInfo.propertyType, propertyInfo.readonly, (SimpleFragment) fragment,
                propertyInfo.fragmentKey);
    }

    /**
     * Gets a collection property from the node, given its name.
     *
     * @param name the property name
     * @return the property
     * @throws PropertyNotFoundException if the name is invalid
     */
    public CollectionProperty getCollectionProperty(String name) {
        CollectionProperty property = (CollectionProperty) propertyCache.get(name);
        if (property == null) {
            ModelProperty propertyInfo = getPropertyInfo(name);
            if (propertyInfo == null) {
                throw new PropertyNotFoundException(name);
            }
            property = makeCollectionProperty(name, propertyInfo);
            propertyCache.put(name, property);
        }
        return property;
    }

    protected CollectionProperty makeCollectionProperty(String name, ModelProperty propertyInfo) {
        String fragmentName = propertyInfo.fragmentName;
        Fragment fragment = fragments.get(fragmentName);
        if (fragment == null) {
            // lazy fragment, fetch from session
            RowId rowId = new RowId(fragmentName, getId());
            fragment = context.get(rowId, true);
        }
        if (fragment instanceof CollectionFragment) {
            return new CollectionProperty(name, propertyInfo.propertyType, propertyInfo.readonly,
                    (CollectionFragment) fragment);
        } else {
            fragments.put(fragmentName, fragment);
            return new CollectionProperty(name, propertyInfo.propertyType, propertyInfo.readonly,
                    (SimpleFragment) fragment, propertyInfo.fragmentKey);
        }
    }

    protected ModelProperty getPropertyInfo(String name) {
        // check primary type
        ModelProperty propertyInfo = model.getPropertyInfo(getPrimaryType(), name);
        if (propertyInfo != null) {
            return propertyInfo;
        }
        // check mixins
        for (String mixin : getMixinTypes()) {
            propertyInfo = model.getMixinPropertyInfo(mixin, name);
            if (propertyInfo != null) {
                return propertyInfo;
            }
        }
        // check proxy schemas
        if (isProxy()) {
            propertyInfo = model.getProxySchemasPropertyInfo(name);
            if (propertyInfo != null) {
                return propertyInfo;
            }
        }
        return null;
    }

    public void setSimpleProperty(String name, Object value) {
        SimpleProperty property = getSimpleProperty(name);
        property.setValue(value);
    }

    public void setCollectionProperty(String name, Object[] value) {
        CollectionProperty property = getCollectionProperty(name);
        property.setValue(value);
    }

    // ----- locking -----

    // ----- lifecycle -----

    // ----- versioning -----

    // ----- activities, baselines, configurations -----

    // ----- shared nodes -----

    // ----- retention -----

    /*
     * ----- equals/hashcode -----
     */

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Node) {
            return equals((Node) other);
        }
        return false;
    }

    private boolean equals(Node other) {
        return getId() == other.getId();
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(uuid=" + getId() + ", name=" + getName() + ", primaryType="
                + getPrimaryType() + ", parentId=" + getParentId() + ")";
    }

    @Override
    public Object getSingle(String name) throws PropertyException {
        return getSimpleProperty(name).getValue();
    }

    @Override
    public Object[] getArray(String name) throws PropertyException {
        return getCollectionProperty(name).getValue();
    }

    @Override
    public void setSingle(String name, Object value) throws PropertyException {
        getSimpleProperty(name).setValue(value);
    }

    @Override
    public void setArray(String name, Object[] value) throws PropertyException {
        getCollectionProperty(name).setValue(value);
    }

}
