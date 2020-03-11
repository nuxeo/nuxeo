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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.localconfiguration;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DetachedAdapter;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Interface that must be implemented by other interface representing a local configuration.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public interface LocalConfiguration<T> extends DetachedAdapter {

    /**
     * Returns the related {@code DocumentRef} of this local configuration.
     */
    DocumentRef getDocumentRef();

    /**
     * Returns {@code true} if this {@code LocalConfiguration} accepted to be merged with a parent configuration,
     * {@code false} otherwise.
     */
    boolean canMerge();

    /**
     * Merge this {@code LocalConfiguration} with another one.
     */
    T merge(T other);

    /**
     * Save this LocalConfiguration.
     *
     * @since 5.5
     */
    void save(CoreSession session);

}
