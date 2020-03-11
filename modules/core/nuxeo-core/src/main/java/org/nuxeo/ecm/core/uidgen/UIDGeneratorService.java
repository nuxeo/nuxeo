/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.uidgen;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.uidgen.UIDGenerator;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;

public interface UIDGeneratorService {

    /**
     * Retrieves the default {@link UIDSequencer}
     *
     * @return the default {@link UIDSequencer}
     * @since 7.3
     */
    UIDSequencer getSequencer();

    /**
     * Retrieves {@link UIDSequencer} by it's name
     *
     * @param name the name of the {@link UIDSequencer}
     * @return the {@link UIDSequencer} matching the name
     * @since 7.3
     */
    UIDSequencer getSequencer(String name);

    /**
     * Returns the uid generator to use for this document.
     * <p>
     * Choice is made following the document type and the generator configuration.
     */
    UIDGenerator getUIDGeneratorFor(DocumentModel doc);

    /**
     * Creates a new UID for the given doc and sets the field configured in the generator component with this value.
     */
    void setUID(DocumentModel doc) throws PropertyNotFoundException;

    /**
     * @return a new UID for the given document
     */
    String createUID(DocumentModel doc);

}
