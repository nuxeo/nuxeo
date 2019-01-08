/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.transientstore.api;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * {@link XMap} descriptor for representing the Configuration of a {@link TransientStore}
 *
 * @since 7.2
 */
@XObject("store")
public class TransientStoreConfig implements Descriptor {

    public static final int DEFAULT_TARGET_MAX_SIZE_MB = -1;

    public static final int DEFAULT_ABSOLUTE_MAX_SIZE_MB = -1;

    public static final int DEFAULT_FIRST_LEVEL_TTL = 60 * 2;

    public static final int DEFAULT_SECOND_LEVEL_TTL = 10;

    @XNode("@name")
    public String name;

    @XNode("@path")
    protected String path;

    // target size that ideally should never be exceeded
    @XNode("targetMaxSizeMB")
    protected Integer targetMaxSizeMB;

    // size that must never be exceeded
    @XNode("absoluteMaxSizeMB")
    protected Integer absoluteMaxSizeMB;

    @XNode("firstLevelTTL")
    protected Integer firstLevelTTL;

    @XNode("secondLevelTTL")
    protected Integer secondLevelTTL;

    @XNode("@class")
    public Class<? extends TransientStoreProvider> implClass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class, nullByDefault = true)
    protected Map<String, String> properties  = new HashMap<>();

    public TransientStoreConfig() {
    }

    public TransientStoreConfig(String name) {
        this.name = name;
    }

    /**
     * Copy constructor.
     *
     * @since 10.10
     */
    public TransientStoreConfig(TransientStoreConfig other) {
        name = other.name;
        path = other.path;
        targetMaxSizeMB = other.targetMaxSizeMB;
        absoluteMaxSizeMB = other.absoluteMaxSizeMB;
        firstLevelTTL = other.firstLevelTTL;
        secondLevelTTL = other.secondLevelTTL;
        implClass = other.implClass;
        properties.putAll(other.properties);
    }

    @Override
    public TransientStoreConfig merge(Descriptor o) {
        TransientStoreConfig other = (TransientStoreConfig) o;
        TransientStoreConfig merged = new TransientStoreConfig();
        merged.name = other.name;
        merged.path = defaultValue(other.path, path);
        merged.targetMaxSizeMB = defaultValue(other.targetMaxSizeMB, targetMaxSizeMB);
        merged.absoluteMaxSizeMB = defaultValue(other.absoluteMaxSizeMB, absoluteMaxSizeMB);
        merged.firstLevelTTL = defaultValue(other.firstLevelTTL, firstLevelTTL);
        merged.secondLevelTTL = defaultValue(other.secondLevelTTL, secondLevelTTL);
        merged.implClass = defaultValue(other.implClass, implClass);
        merged.properties.putAll(properties);
        merged.properties.putAll(other.properties);
        return merged;
    }

    protected static <T> T defaultValue(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public int getTargetMaxSizeMB() {
        return targetMaxSizeMB == null ? DEFAULT_TARGET_MAX_SIZE_MB : targetMaxSizeMB.intValue();
    }

    /** @deprecated since 10.10, unused */
    @Deprecated
    public void setTargetMaxSizeMB(int targetMaxSizeMB) {
        this.targetMaxSizeMB = Integer.valueOf(targetMaxSizeMB);
    }

    public int getAbsoluteMaxSizeMB() {
        return absoluteMaxSizeMB == null ? DEFAULT_ABSOLUTE_MAX_SIZE_MB : absoluteMaxSizeMB.intValue();
    }

    /** @deprecated since 10.10, unused */
    @Deprecated
    public void setAbsoluteMaxSizeMB(int absoluteMaxSizeMB) {
        this.absoluteMaxSizeMB = Integer.valueOf(absoluteMaxSizeMB);
    }

    public int getFirstLevelTTL() {
        return firstLevelTTL == null ? DEFAULT_FIRST_LEVEL_TTL : firstLevelTTL.intValue();
    }

    /** @deprecated since 10.10, unused */
    @Deprecated
    public void setFirstLevelTTL(int firstLevelTTL) {
        this.firstLevelTTL = Integer.valueOf(firstLevelTTL);
    }

    public int getSecondLevelTTL() {
        return secondLevelTTL == null ? DEFAULT_SECOND_LEVEL_TTL : secondLevelTTL.intValue();
    }

    /** @deprecated since 10.10, unused */
    @Deprecated
    public void setSecondLevelTTL(int secondLevelTTL) {
        this.secondLevelTTL = Integer.valueOf(secondLevelTTL);
    }

    /**
     * Returns the directory where blobs will be stored.
     *
     * @since 9.1
     */
    public String getDataDir() {
        return path;
    }

    /**
     * Returns properties.
     *
     * @since 10.1
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns the implementation class, or {@code null} if not defined.
     *
     * @since 10.10
     */
    public Class<? extends TransientStoreProvider> getKlass() {
        return implClass;
    }

}
