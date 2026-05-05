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
package org.nuxeo.snapshot.pageprovider;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.snapshot.Snapshot;

public class VFolderPageProvider extends AbstractPageProvider<DocumentModel> implements PageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    /**
     * @deprecated since 2025.20, use {@link org.nuxeo.ecm.platform.query.api.PageProviderSpec#CORE_SESSION_PROPERTY}
     *             instead
     */
    @Deprecated(since = "2025.20", forRemoval = true)
    public static final String CORE_SESSION_PROPERTY = PageProviderSpec.CORE_SESSION_PROPERTY;

    @Override
    public List<DocumentModel> getCurrentPage() {
        DocumentModel target = (DocumentModel) getParameters()[0];
        Snapshot snap = target.getAdapter(Snapshot.class);
        return snap.getChildren();
    }

}
