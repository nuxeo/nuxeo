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
 * $Id: LogEntry.java 1362 2006-07-26 14:26:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;

/**
 * Logs interface.
 *
 * @see <a href="http://jira.nuxeo.org/browse/NXP-514">NXP-514</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface Logs extends AuditReader, AuditLogger, AuditAdmin {

}
