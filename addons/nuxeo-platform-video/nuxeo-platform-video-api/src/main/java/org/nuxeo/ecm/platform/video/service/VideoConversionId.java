/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.api.DocumentLocation;

/**
 * Unique identifier for a given video conversion on one document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 * @deprecated since 5.7.3, use other APIs instead
 */
@Deprecated
public class VideoConversionId {

    private final DocumentLocation documentLocation;

    private final String conversionName;

    public VideoConversionId(DocumentLocation documentLocation, String conversionName) {
        this.documentLocation = documentLocation;
        this.conversionName = conversionName;
    }

    public DocumentLocation getDocumentLocation() {
        return documentLocation;
    }

    public String getConversionName() {
        return conversionName;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return String.format("VideoConversionId [documentLocation=%s, conversionName=%s]", documentLocation,
                conversionName);
    }
}
