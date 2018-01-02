/*
 * (C) Copyright 2017-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.core;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.elasticsearch.api.IndexNameGenerator;

/**
 * @since 9.3
 */
public class IncrementalIndexNameGenerator implements IndexNameGenerator {
    protected static final String SEP = "-";

    @Override
    public String getNextIndexName(String aliasName, String oldIndexName) {
        if (StringUtils.isEmpty(oldIndexName)) {
            return aliasName + SEP + "0000";
        }
        int i = oldIndexName.lastIndexOf(SEP);
        if (i < 0) {
            throw new IllegalArgumentException("Invalid index name: " + oldIndexName);
        }
        int index = Integer.parseInt(oldIndexName.substring(i + 1)) + 1;
        return String.format("%s%s%04d", aliasName, SEP, index);
    }
}
