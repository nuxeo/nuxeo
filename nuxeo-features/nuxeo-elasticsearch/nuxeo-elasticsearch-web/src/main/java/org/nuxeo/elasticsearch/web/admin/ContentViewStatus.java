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

public class ContentViewStatus implements Comparable<ContentViewStatus>{

    String cvName;

    String ppName;

    String type;

    public ContentViewStatus(String cvName, String ppName, String type) {
        this.cvName = cvName;
        this.ppName = ppName;
        this.type = type;
    }

    public String getCvName() {
        return cvName;
    }

    public String getPpName() {
        return ppName;
    }

    public String getType() {
        return type;
    }

    public String getColor() {
        if ("core".equals(type)) {
            return "#000000";
        } else if ("elasticsearch".equals(type)) {
            return "#00CC00";
        } else {
            return "#0000FF";
        }
    }

    @Override
    public int compareTo(ContentViewStatus other) {
        return getCvName().toLowerCase().compareTo(other.getCvName().toLowerCase());
    }

}
