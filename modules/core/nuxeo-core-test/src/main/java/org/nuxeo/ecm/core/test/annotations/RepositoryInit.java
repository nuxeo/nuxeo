/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test.annotations;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Implement this class to provide an initializer for the {@link RepositoryConfig} annotation in tests.
 */
public interface RepositoryInit {

    /**
     * Creates the default objects in an empty repository.
     *
     * @param session the session to use to create objects
     */
    void populate(CoreSession session);

}
