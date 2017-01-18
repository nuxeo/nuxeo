/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.filter;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This service filters disallowed characters in strings and document properties.
 * <p>
 * Disallowed characters are control characters that could prevent XML validation or in some cases database writes.
 *
 * @since 9.1
 */
public interface CharacterFilteringService {

    /**
     * Removes all disallowed characters from the input value.
     *
     * @param value the value to filter
     * @return the filtered value
     */
    String filter(String value);

    /**
     * Removes all disallowed characters from the dirty document properties.
     *
     * @param docModel the document to filter
     */
    void filter(DocumentModel docModel);

}
