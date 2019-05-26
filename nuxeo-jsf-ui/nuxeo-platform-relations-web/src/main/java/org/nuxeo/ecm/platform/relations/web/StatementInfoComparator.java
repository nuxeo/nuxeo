/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: StatementInfoComparator.java 20645 2007-06-17 13:16:54Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * Statement info comparator to sort relations.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class StatementInfoComparator implements Comparator<StatementInfo>, Serializable {

    private static final long serialVersionUID = -5117909579284277595L;

    @Override
    public int compare(StatementInfo stmt1, StatementInfo stmt2) {
        // XXX AT: always compare by modification date for now, will have to be
        // more pluggable when need to sort tables.
        Date date1 = stmt1.getModificationDate();
        Date date2 = stmt2.getModificationDate();
        int result = 0;
        if (date1 == null) {
            result = date2 == null ? 0 : -1;
        } else if (date2 == null) {
            result = 1;
        } else {
            result = date1.compareTo(date2);
        }
        return result;
    }

}
