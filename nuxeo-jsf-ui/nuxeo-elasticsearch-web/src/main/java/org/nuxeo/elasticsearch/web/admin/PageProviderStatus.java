/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.web.admin;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

public class PageProviderStatus implements Comparable<PageProviderStatus> {

    protected static final String CORE_QUERY_TYPE = "CoreQueryDocumentPageProvider";

    protected static final String ELASTIC_TYPE = "elasticsearch";

    String cvName;

    String ppName;

    String type;

    public PageProviderStatus(String ppName, String klass) {
        this.ppName = ppName;
        switch (klass) {
        case "org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider":
            this.type = CORE_QUERY_TYPE;
            break;
        case "org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider":
            this.type = ELASTIC_TYPE;
            break;
        default:
            this.type = klass;
        }
    }

    public String getPpName() {
        return ppName;
    }

    public String getType() {
        return type;
    }

    public String getColor() {
        if (CORE_QUERY_TYPE.equals(type)) {
            return "#0c8abb";
        } else if (ELASTIC_TYPE.equals(type)) {
            return "#0aca00";
        } else {
            return "#444444";
        }
    }

    @Override
    public int compareTo(PageProviderStatus other) {
        return getPpName().toLowerCase().compareTo(other.getPpName().toLowerCase());
    }

}
