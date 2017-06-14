/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.Serializable;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for meta-data mapping
 *
 * @author Thierry Delprat
 */
@XObject("fieldMapping")
public class ScanFileFieldMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final FastDateFormat DEFAULT_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");

    @XNode("@sourceXPath")
    protected String sourceXPath;

    @XNode("@sourceAttribute")
    protected String sourceAttribute = "TEXT";

    @XNode("@targetXPath")
    protected String targetXPath;

    @XNode("@targetType")
    protected String targetType = "String";

    @XNode("@dateFormat")
    protected String dateFormatStr;

    protected FastDateFormat dateFormat;

    public FastDateFormat getDateFormat() {
        if (dateFormat == null) {
            if (dateFormatStr != null) {
                dateFormat = FastDateFormat.getInstance(dateFormatStr);
            } else {
                dateFormat = DEFAULT_DATE_FORMAT;
            }
        }
        return dateFormat;
    }

    public String getSourceXPath() {
        return sourceXPath;
    }

    public String getSourceAttribute() {
        return sourceAttribute;
    }

    public String getTargetXPath() {
        return targetXPath;
    }

    public String getTargetType() {
        return targetType;
    }

}
