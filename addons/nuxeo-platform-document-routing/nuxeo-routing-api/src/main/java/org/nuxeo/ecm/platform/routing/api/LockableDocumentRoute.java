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
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 */
public interface LockableDocumentRoute extends Serializable {

    void lockDocument(CoreSession session);

    void unlockDocument(CoreSession session);

    boolean isLocked(CoreSession session);

    boolean isLockedByCurrentUser(CoreSession session);

    String getLockOwner(CoreSession session);
}
