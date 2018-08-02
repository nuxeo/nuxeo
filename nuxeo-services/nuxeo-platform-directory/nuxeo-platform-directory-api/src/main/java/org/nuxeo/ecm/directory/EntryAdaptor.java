/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 *
 * $Id$
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface to be used by Directory implementations to perform arbitrary "fetch-time" adaptations on the entry fields
 * and the readonly flag.
 */
public interface EntryAdaptor {

    /**
     * Allow the directory initialization process to configure the adaptor by providing String valued parameters.
     */
    void setParameter(String name, String value);

    /**
     * Apply an arbitrary transformation of the fetched entry.
     *
     * @param directory the directory instance the entry is fetched from
     * @param entry the entry to transform
     * @return the adapted entry
     */
    DocumentModel adapt(Directory directory, DocumentModel entry);

}
