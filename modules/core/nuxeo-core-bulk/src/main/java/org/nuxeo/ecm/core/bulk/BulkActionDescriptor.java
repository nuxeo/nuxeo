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

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 10.2
 */
@XObject("action")
public class BulkActionDescriptor implements Descriptor {

    public static final Integer DEFAULT_BUCKET_SIZE = 100;

    public static final Integer DEFAULT_BATCH_SIZE = 25;

    // @since 11.1
    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@name")
    public String name;

    // @since 11.1
    @XNode("@inputStream")
    public String inputStream;

    @XNode("@bucketSize")
    public Integer bucketSize = DEFAULT_BUCKET_SIZE;

    @XNode("@batchSize")
    public Integer batchSize = DEFAULT_BATCH_SIZE;

    @XNode("@httpEnabled")
    public Boolean httpEnabled = Boolean.FALSE;

    @XNode("@sequentialCommands")
    public Boolean sequentialCommands = Boolean.FALSE;

    @XNode("@validationClass")
    public Class<? extends BulkActionValidation> validationClass;

    // @since 11.1
    @XNode("@defaultScroller")
    public String defaultScroller;

    @Override
    public String getId() {
        return name;
    }

    public Integer getBucketSize() {
        return bucketSize;
    }

    public Integer getBatchSize() {
        return batchSize;
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
    @Override
    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    // @since 11.1
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // @since 11.1
    public String getDefaultScroller() {
        return defaultScroller;
    }

    // @since 11.1
    public String getInputStream() {
        return defaultIfBlank(inputStream, name);
    }

    @Override
    public BulkActionDescriptor merge(Descriptor o) {
        var other = (BulkActionDescriptor) o;
        var merged = new BulkActionDescriptor();
        merged.name = other.name;
        merged.enabled = defaultIfNull(other.enabled, enabled);
        merged.inputStream = defaultIfBlank(other.inputStream, inputStream);
        merged.bucketSize = defaultIfNull(other.bucketSize, bucketSize);
        merged.batchSize = defaultIfNull(other.batchSize, batchSize);
        merged.httpEnabled = defaultIfNull(other.httpEnabled, httpEnabled);
        merged.sequentialCommands = defaultIfNull(other.sequentialCommands, sequentialCommands);
        merged.validationClass = defaultIfNull(other.validationClass, validationClass);
        merged.defaultScroller = defaultIfBlank(other.defaultScroller, defaultScroller);
        return merged;
    }
}
