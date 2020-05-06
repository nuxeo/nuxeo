/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 */
package org.nuxeo.ecm.core.api.repository;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor of the pool of low-level Nuxeo Sessions.
 *
 * @since 5.6
 */
@XObject("pool")
public class PoolConfiguration {

    public static final int DEFAULT_MAX_POOL_SIZE = 20;

    public static final int DEFAULT_MIN_POOL_SIZE = 0;

    public static final int DEFAULT_BLOCKING_TIMEOUT_MILLIS = 100;

    @XNode("@maxPoolSize")
    private Integer maxPoolSize;

    @XNode("@minPoolSize")
    private Integer minPoolSize;

    @XNode("@blockingTimeoutMillis")
    private Integer blockingTimeoutMillis;

    public PoolConfiguration() {
    }

    /** Copy constructor. */
    public PoolConfiguration(PoolConfiguration other) {
        maxPoolSize = other.maxPoolSize;
        minPoolSize = other.minPoolSize;
        blockingTimeoutMillis = other.blockingTimeoutMillis;
    }

    public void merge(PoolConfiguration other) {
        if (other.maxPoolSize != null) {
            maxPoolSize = other.maxPoolSize;
        }
        if (other.minPoolSize != null) {
            minPoolSize = other.minPoolSize;
        }
        if (other.blockingTimeoutMillis != null) {
            blockingTimeoutMillis = other.blockingTimeoutMillis;
        }
    }

    private static int defaultInt(Integer value, int def) {
        return value == null ? def : value.intValue();
    }

    public int getMaxPoolSize() {
        return defaultInt(maxPoolSize, DEFAULT_MAX_POOL_SIZE);
    }

    public int getMinPoolSize() {
        return defaultInt(minPoolSize, DEFAULT_MIN_POOL_SIZE);
    }

    public int getBlockingTimeoutMillis() {
        return defaultInt(blockingTimeoutMillis, DEFAULT_BLOCKING_TIMEOUT_MILLIS);
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = Integer.valueOf(maxPoolSize);
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = Integer.valueOf(minPoolSize);
    }

    public void setBlockingTimeoutMillis(int blockingTimeoutMillis) {
        this.blockingTimeoutMillis = Integer.valueOf(blockingTimeoutMillis);
    }

    @XNode("@maxActive")
    public void setMaxActive(int num) {
        maxPoolSize = num;
    }

    @XNode("@maxIdle")
    public void setMaxIdle(int num) {
        minPoolSize = num;
    }

    @XNode("@maxWait")
    public void setMaxWait(int num) {
        blockingTimeoutMillis = num;
    }

}
