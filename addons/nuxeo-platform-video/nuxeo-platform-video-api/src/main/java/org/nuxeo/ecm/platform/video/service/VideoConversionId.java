/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public VideoConversionId(DocumentLocation documentLocation,
            String conversionName) {
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
        return String.format(
                "VideoConversionId [documentLocation=%s, conversionName=%s]",
                documentLocation, conversionName);
    }
}
