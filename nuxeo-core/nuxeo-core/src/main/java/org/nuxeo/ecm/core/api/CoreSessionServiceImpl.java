/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the service giving access to {@link CoreSession} creation. INTERNAL, not to be used by application
 * code.
 *
 * @since 8.4
 */
public class CoreSessionServiceImpl extends DefaultComponent implements CoreSessionService {

    @Override
    public CoreSession createCoreSession() {
        return LocalSession.createInstance();
    }

}
