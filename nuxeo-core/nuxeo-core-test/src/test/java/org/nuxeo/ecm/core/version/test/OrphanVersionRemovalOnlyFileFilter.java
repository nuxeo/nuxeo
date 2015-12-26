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
package org.nuxeo.ecm.core.version.test;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;

public class OrphanVersionRemovalOnlyFileFilter implements OrphanVersionRemovalFilter {

    @Override
    public List<String> getRemovableVersionIds(CoreSession session, ShallowDocumentModel deletedLiveDoc,
            List<String> versionUUIDs) {

        if (deletedLiveDoc.getType().equals("File")) {
            return Collections.emptyList();
        }

        return versionUUIDs;
    }

}
