/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.user.center.profile;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.2
 */
@Experimental(comment="https://jira.nuxeo.com/browse/NXP-12200")
@XObject("importerConfig")
public class ImporterConfig {

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

    public int getBatchSize() {
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
