/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.web.admin;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
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
            return "#0000FF";
        } else if (ELASTIC_TYPE.equals(type)) {
            return "#22BB22";
        } else {
            return "#424242";
        }
    }

    @Override
    public int compareTo(PageProviderStatus other) {
        return getPpName().toLowerCase().compareTo(other.getPpName().toLowerCase());
    }

}
