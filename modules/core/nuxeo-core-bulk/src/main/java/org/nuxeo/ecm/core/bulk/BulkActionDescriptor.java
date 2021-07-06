/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.time.Duration;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 10.2
 */
@XObject("action")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class BulkActionDescriptor {

    protected static final String DEFAULT_BUCKET_SIZE = "100";

    protected static final String DEFAULT_BATCH_SIZE = "25";

    // @since 11.5
    protected static final String DEFAULT_BATCH_TRANSACTION_TIMEOUT = "0s";

    // @since 11.1
    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean isEnabled = true;

    @XNode("@name")
    @XRegistryId
    public String name;

    // @since 11.1
    @XNode("@inputStream")
    public String inputStream;

    @XNode(value = "@bucketSize", defaultAssignment = DEFAULT_BUCKET_SIZE)
    public Integer bucketSize;

    @XNode(value = "@batchSize", defaultAssignment = DEFAULT_BATCH_SIZE)
    public Integer batchSize;

    @XNode(value = "@batchTransactionTimeout", defaultAssignment = DEFAULT_BATCH_TRANSACTION_TIMEOUT)
    public Duration batchTransactionTimeout;

    @XNode("@httpEnabled")
    public Boolean httpEnabled = Boolean.FALSE;

    @XNode("@sequentialCommands")
    public Boolean sequentialCommands = Boolean.FALSE;

    @XNode("@validationClass")
    public Class<? extends BulkActionValidation> validationClass;

    // @since 11.1
    @XNode("@defaultScroller")
    public String defaultScroller;

    // @since 11.4 the maximum number of items that the scroller can return
    @XNode("@defaultQueryLimit")
    public Long defaultQueryLimit;

    public String getId() {
        return name;
    }

    public Integer getBucketSize() {
        return bucketSize;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    // @since 11.5
    public Duration getBatchTransactionTimeout() {
        return batchTransactionTimeout;
    }

    // @since 11.4
    public Long getDefaultQueryLimit() {
        return defaultQueryLimit;
    }

    /**
     * @since 10.10
     */
    public BulkActionValidation newValidationInstance() {
        try {
            return validationClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Cannot create validation class of type " + validationClass.getName(), e);
        }
    }

    // @since 11.1
    public boolean isEnabled() {
        return isEnabled;
    }

    // @since 11.1
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    // @since 11.1
    public String getDefaultScroller() {
        return defaultScroller;
    }

    // @since 11.1
    public String getInputStream() {
        return defaultIfBlank(inputStream, name);
    }
}
