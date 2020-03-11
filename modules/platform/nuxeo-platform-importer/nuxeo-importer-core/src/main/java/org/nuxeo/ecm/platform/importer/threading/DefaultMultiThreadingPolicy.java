/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.threading;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Default multi-threading policy (creates a new thread on each branch until the max nb thread is reached)
 *
 * @author Thierry Delprat
 */
public class DefaultMultiThreadingPolicy implements ImporterThreadingPolicy {

    @Override
    public boolean needToCreateThreadAfterNewFolderishNode(DocumentModel parent, SourceNode node, long uploadedSources,
            int batchSize, int scheduledTasks) {

        if (uploadedSources < (batchSize / 3)) {
            return false;
        }

        if (scheduledTasks >= 5) {
            return false;
        } else {
            return true;
        }
    }

}
