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
package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;

public class SampleRootSectionFinder extends DefaultRootSectionsFinder {

    public SampleRootSectionFinder(CoreSession userSession) {
        super(userSession);
    }

    @Override
    protected String buildQuery(String path) {
        String query = "SELECT * FROM Document WHERE (";
        int i = 0;
        for (String type : getSectionTypes()) {
            query = query + " ecm:primaryType = '" + type + "'";
            i++;
            if (i < getSectionTypes().size()) {
                query = query + " or ";
            } else {
                query = query + " )";
            }
        }
        query = query + " order by ecm:path ";
        return query;
    }

}
