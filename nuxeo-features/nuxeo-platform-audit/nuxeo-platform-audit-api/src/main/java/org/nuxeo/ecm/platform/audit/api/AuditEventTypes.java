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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AuditEventTypes.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;

/**
 * NXAudit event types.
 * <p>
 * For now, used by the content history Seam listener to be notified for logs invalidation using the Seam event service.
 * <p>
 * Might useful for other kind of event based notifications.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class AuditEventTypes {

    public static final String HISTORY_CHANGED = "historyChanged";

    // Constant utility class.
    private AuditEventTypes() {
    }

}
