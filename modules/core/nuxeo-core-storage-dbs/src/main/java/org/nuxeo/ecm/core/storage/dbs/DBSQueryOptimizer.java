/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.QueryOptimizer;

/**
 * DBS-specific query optimizer.
 * <p>
 * Knows how reference prefixes are computed, especially for the ACL case which has a storage structure different than
 * what the NXQL syntax suggests.
 *
 * @since 9.3
 */
public class DBSQueryOptimizer extends QueryOptimizer {

    protected static final Pattern CORRELATED_WILDCARD_SPLIT = Pattern.compile("(([^*]+)/(\\*\\d+))/(.*)");

    protected static final Pattern CORRELATED_ECM_TAG = Pattern.compile(NXQL.ECM_TAG + "/\\*\\d+");

    protected static final String CORRELATED_ECM_TAG_IMPLICIT = "__ecm_tag_correlated__";

    @Override
    public String getCorrelatedWildcardPrefix(String name) {
        if (name.startsWith(NXQL.ECM_TAG)) {
            if (name.equals(NXQL.ECM_TAG)) {
                // naked ecm:tag are always correlated with themselves
                return CORRELATED_ECM_TAG_IMPLICIT;
            } else if (CORRELATED_ECM_TAG.matcher(name).matches()) {
                return name;
            } else {
                return "";
            }
        }
        Matcher m = CORRELATED_WILDCARD_SPLIT.matcher(name);
        if (!m.matches()) {
            return "";
        }
        String start = m.group(2);
        String wildcard = m.group(3);
        String end = m.group(4);
        if (start.equals(NXQL.ECM_ACL)) {
            if (end.equals(NXQL.ECM_ACL_NAME)) {
                return KEY_ACP + '/' + wildcard;
            } else {
                return KEY_ACP + '/' + wildcard + '/' + KEY_ACL + '/' + wildcard;
            }
        } else {
            return m.group(1);
        }
    }

}
