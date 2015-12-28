/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.util.Collection;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor.ProcessorStatus;

public interface IDataProcessor {
    public void analyze(CoreSession session);

    public void analyze(CoreSession session, DocumentModel doc, int timeout);

    public Set<String> getUserAndGroups();

    public Set<String> getPermissions();

    /** The maximum doc tree depth */
    public int getDocumentTreeMaxDepth();

    /**
     * The minimum doc tree depth, 0 if analysis was run on repository root, >0 if the analysis was run on a child
     * document of repository root.
     */
    public int getDocumentTreeMinDepth();

    public int getNumberOfDocuments();

    public Collection<DocumentSummary> getAllDocuments();

    /** A status concerning data analysis */
    public ProcessorStatus getStatus();

    /** Some text information related to the status */
    public String getInformation();
}
