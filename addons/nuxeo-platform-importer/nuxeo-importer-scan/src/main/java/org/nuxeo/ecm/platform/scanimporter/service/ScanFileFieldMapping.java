/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.Serializable;
import java.text.SimpleDateFormat;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * XMap descriptor for meta-data mapping
 *
 * @author Thierry Delprat
 *
 */
@XObject("fieldMapping")
public class ScanFileFieldMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'hh:mm:ss.sss'Z'");

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

    protected SimpleDateFormat dateFormat;

    public SimpleDateFormat getDateFormat() {
        if (dateFormat == null) {
            if (dateFormatStr != null) {
                dateFormat = new SimpleDateFormat(dateFormatStr);
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
