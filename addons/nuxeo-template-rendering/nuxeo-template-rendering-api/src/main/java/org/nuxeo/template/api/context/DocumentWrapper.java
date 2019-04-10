/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.template.api.context;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * Wrapper interface used to wrap the Object that will be put inside the rendering context.
 * <p>
 * Because the rederning context wrapping requirements can depends on the actual rendering engine implementation, this
 * is just an interface so that several implemenations can be provided
 * </p>
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface DocumentWrapper {

    Object wrap(DocumentModel doc);

    Object wrap(List<LogEntry> auditEntries);
}
