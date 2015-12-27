/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.userpreferences;

import org.nuxeo.ecm.core.api.CoreSession;

public interface UserPreferencesService {

    SimpleUserPreferences getSimpleUserPreferences(CoreSession session);

    @SuppressWarnings("rawtypes")
    <T extends UserPreferences> T getUserPreferences(CoreSession session, Class<T> configurationClass,
            String configurationFacet);

}
