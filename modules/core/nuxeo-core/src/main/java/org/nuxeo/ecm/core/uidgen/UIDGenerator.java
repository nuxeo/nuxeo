/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.core.uidgen;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;

/**
 * Common interface for UID generators. All UID generators must implement this interface.
 *
 * @author <a href="mailto:dm@nuxeo.com>Dragos Mihalache</a>
 */
public interface UIDGenerator {

    /**
     * Sets the property name used to set the identifier value.
     * <p>
     * The property must be a string like 'schemaPrefix:fieldName' ; the syntax 'schemaName:fieldName' is also accepted.
     * Could be used as a convenient method when there is only one property to set.
     *
     * @param propertyName
     * @see #setPropertyNames(String[])
     */
    void setPropertyName(String propertyName);

    /**
     * Get the property name used to set the identifier value.
     *
     * @see #getPropertyNames()
     */
    String getPropertyName();

    /**
     * Set the properties used to set the identifier value.
     *
     * @param propertyNames
     */
    void setPropertyNames(String[] propertyNames);

    /**
     * Gets the property name used to set the identifier value
     *
     * @return
     */
    String[] getPropertyNames();

    /**
     * The sequencer used to generate unique numbers sequencially.
     *
     * @param sequencer
     */
    void setSequencer(UIDSequencer sequencer);

    String getSequenceKey(DocumentModel document);

    /**
     * Returns a new UID for the given doc.
     */
    String createUID(DocumentModel document);

    /**
     * Creates a new UID for the given doc and sets the field configured in the generator component with this value.
     */
    void setUID(DocumentModel document) throws PropertyNotFoundException;

}
