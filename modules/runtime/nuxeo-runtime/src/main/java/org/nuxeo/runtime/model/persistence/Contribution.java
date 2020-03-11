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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.io.InputStream;

import org.nuxeo.runtime.model.StreamRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Contribution extends StreamRef {

    /**
     * Gets the contribution name.
     */
    String getName();

    /**
     * Gets the contribution description.
     */
    String getDescription();

    /**
     * Sets the contribution description.
     */
    void setDescription(String description);

    /**
     * Whether this contribution should be automatically installed at startup.
     */
    boolean isDisabled();

    /**
     * Sets the auto install flag for this contribution.
     */
    void setDisabled(boolean isAutoStart);

    /**
     * Gets the contribution XML content. The content should be in Nuxeo XML component format.
     */
    @Override
    InputStream getStream();

    /**
     * Gets the contribution XML content. The content should be in Nuxeo XML component format.
     */
    String getContent();

}
