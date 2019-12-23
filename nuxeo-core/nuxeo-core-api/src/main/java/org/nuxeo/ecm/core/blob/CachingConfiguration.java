/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.nuxeo.common.utils.SizeUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Configuration for a cache.
 *
 * @since 11.1
 */
public class CachingConfiguration extends PropertyBasedConfiguration {

    public static final String CACHE_SIZE_PROPERTY = "cachesize";

    public static final String CACHE_COUNT_PROPERTY = "cachecount";

    public static final String CACHE_MIN_AGE_PROPERTY = "cacheminage";

    public static final String DEFAULT_CACHE_SIZE = "100 mb";

    public static final String DEFAULT_CACHE_COUNT = "10000";

    public static final String DEFAULT_CACHE_MIN_AGE = "3600"; // 1h

    public final Path dir;

    public final long maxSize;

    public final long maxCount;

    public final long minAge;

    public CachingConfiguration(String systemPropertyPrefix, Map<String, String> properties) throws IOException {
        super(systemPropertyPrefix, properties);
        dir = Framework.createTempDirectory("nxbincache.");
        String maxSizeProp = getProperty(CACHE_SIZE_PROPERTY, DEFAULT_CACHE_SIZE);
        String maxCountProp = getProperty(CACHE_COUNT_PROPERTY, DEFAULT_CACHE_COUNT);
        String minAgeProp = getProperty(CACHE_MIN_AGE_PROPERTY, DEFAULT_CACHE_MIN_AGE);
        maxSize = SizeUtils.parseSizeInBytes(maxSizeProp);
        maxCount = Long.parseLong(maxCountProp);
        minAge = Long.parseLong(minAgeProp);
    }

    public CachingConfiguration(Path dir, long maxSize, long maxCount, long minAge) {
        super(null, null);
        this.dir = dir;
        this.maxSize = maxSize;
        this.maxCount = maxCount;
        this.minAge = minAge;
    }

}
