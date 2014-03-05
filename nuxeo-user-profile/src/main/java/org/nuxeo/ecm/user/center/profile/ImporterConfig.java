/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.user.center.profile;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.9.3
 */
@XObject("importerConfig")
public class ImporterConfig {

    private static final Log log = LogFactory.getLog(ImporterConfig.class);

    public static final String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";

    public static final String DEFAULT_LIST_SEPARATOR_REGEX = "\\|";

    public static final int DEFAULT_BATCH_SIZE = 50;

    @XNode("dataFile")
    protected String dataFileName;

    @XNode("dateFormat")
    protected String dateFormat = DEFAULT_DATE_FORMAT;

    @XNode("listSeparatorRegex")
    protected String listSeparatorRegex = DEFAULT_LIST_SEPARATOR_REGEX;

    @XNode("updateExisting")
    protected boolean updateExisting = true;

    @XNode("batchSize")
    protected int batchSize = DEFAULT_BATCH_SIZE;

    public String getDataFileName() {
        return dataFileName;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getListSeparatorRegex() {
        return listSeparatorRegex;
    }

    public boolean isUpdateExisting() {
        return updateExisting;
    }

    public int getBatchSize()
    {
        return batchSize;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
