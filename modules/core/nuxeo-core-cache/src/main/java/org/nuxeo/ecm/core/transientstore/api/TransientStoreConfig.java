/*
 * (C) Copyright 2015-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.nuxeo.common.utils.ByteSize;
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

    /**
     * @deprecated since 2025.11, use {@link ByteSize#unlimited()} instead
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    public static final int DEFAULT_TARGET_MAX_SIZE_MB = -1;

    /**
     * @deprecated since 2025.11, use {@link ByteSize#unlimited()} instead
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    public static final int DEFAULT_ABSOLUTE_MAX_SIZE_MB = -1;

    public static final int DEFAULT_FIRST_LEVEL_TTL = 60 * 2;

    public static final int DEFAULT_SECOND_LEVEL_TTL = 10;

    @XNode("@name")
    public String name;

    @XNode("@path")
    protected String path;

    // target size that ideally should never be exceeded
    /** @since 2025.11 */
    @XNode("targetMaxSize")
    protected ByteSize targetMaxSize;

    // size that must never be exceeded
    /** @since 2025.11 */
    @XNode("absoluteMaxSize")
    protected ByteSize absoluteMaxSize;

    @XNode("firstLevelTTL")
    protected Integer firstLevelTTL;

    @XNode("secondLevelTTL")
    protected Integer secondLevelTTL;

    @XNode("@class")
    public Class<? extends TransientStoreProvider> implClass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class, nullByDefault = true)
    protected Map<String, String> properties = new HashMap<>();

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
        targetMaxSize = other.targetMaxSize;
        absoluteMaxSize = other.absoluteMaxSize;
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
        merged.path = getIfNull(other.path, path);
        merged.targetMaxSize = getIfNull(other.targetMaxSize, targetMaxSize);
        merged.absoluteMaxSize = getIfNull(other.absoluteMaxSize, absoluteMaxSize);
        merged.firstLevelTTL = getIfNull(other.firstLevelTTL, firstLevelTTL);
        merged.secondLevelTTL = getIfNull(other.secondLevelTTL, secondLevelTTL);
        merged.implClass = getIfNull(other.implClass, implClass);
        merged.properties.putAll(properties);
        merged.properties.putAll(other.properties);
        return merged;
    }

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    /** @since 2025.11 */
    public Optional<ByteSize> getTargetMaxSize() {
        return Optional.ofNullable(targetMaxSize);
    }

    /**
     * @deprecated since 2025.11, use {@link #getTargetMaxSize()} instead
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    public int getTargetMaxSizeMB() {
        return targetMaxSize == null ? DEFAULT_TARGET_MAX_SIZE_MB : (int) targetMaxSize.toMebibytes();
    }

    /**
     * @since 2025.11
     * @deprecated since 2025.11, for backward compatibility purpose
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    @XNode("targetMaxSizeMB")
    protected void setTargetMaxSizeMB(int targetMaxSizeMB) {
        if (targetMaxSizeMB < 0) {
            this.targetMaxSize = null;
        } else {
            this.targetMaxSize = ByteSize.ofMebibytes(targetMaxSizeMB);
        }
    }

    /** @since 2025.11 */
    public Optional<ByteSize> getAbsoluteMaxSize() {
        return Optional.ofNullable(absoluteMaxSize);
    }

    /**
     * @deprecated since 2025.11, use {@link #getAbsoluteMaxSize()} instead
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    public int getAbsoluteMaxSizeMB() {
        return absoluteMaxSize == null ? DEFAULT_ABSOLUTE_MAX_SIZE_MB : (int) absoluteMaxSize.toMebibytes();
    }

    /**
     * @since 2025.11, for backward compatibility purpose
     * @deprecated since 2025.11
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    @XNode("absoluteMaxSizeMB")
    protected void setAbsoluteMaxSizeMB(int absoluteMaxSizeMB) {
        if (absoluteMaxSizeMB < 0) {
            this.absoluteMaxSize = null;
        } else {
            this.absoluteMaxSize = ByteSize.ofMebibytes(absoluteMaxSizeMB);
        }
    }

    public int getFirstLevelTTL() {
        return firstLevelTTL == null ? DEFAULT_FIRST_LEVEL_TTL : firstLevelTTL.intValue();
    }

    public int getSecondLevelTTL() {
        return secondLevelTTL == null ? DEFAULT_SECOND_LEVEL_TTL : secondLevelTTL.intValue();
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
