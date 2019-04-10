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
 *     Thierry Delprat
 *     Gagnavarslan ehf
 *     Vitalii Siryi
 */
package org.nuxeo.ecm.webdav.backend;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;

public class SearchRootBackend extends SearchVirtualBackend {

    private static final String QUERY = "select * from Workspace where ecm:mixinType != 'HiddenInNavigation' "
            + "AND  ecm:isTrashed = 0 AND ecm:isProxy = 0 order by ecm:path";

    public SearchRootBackend(CoreSession session) {
        super("", "", QUERY, session, new SimpleRealBackendFactory());
    }

    @Override
    public Backend getBackend(String uri) {
        if (StringUtils.isEmpty(uri) || "/".equals(uri)) {
            return this;
        } else {
            return super.getBackend(uri);
        }

    }

    @Override
    public boolean isRoot() {
        return true;
    }
}
