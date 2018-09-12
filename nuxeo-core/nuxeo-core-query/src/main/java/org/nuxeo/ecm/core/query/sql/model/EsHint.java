/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.query.sql.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class EsHint implements Operand {

    private static final long serialVersionUID = 4590329982296853715L;

    public final String index;

    public final String analyzer;

    public final String operator;

    public EsHint(EsIdentifierList index, String analyzer, String operator) {
        this.index = (index == null) ? null : index.toString();
        this.analyzer = analyzer;
        this.operator = operator;
    }

    @Override
    public void accept(IVisitor visitor) {
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("/*+ES: ");
        if (index != null) {
            buf.append(String.format("INDEX(%s) ", index));
        }
        if (analyzer != null) {
            buf.append(String.format("ANALYZER(%s) ", analyzer));
        }
        if (operator != null) {
            buf.append(String.format("OPERATOR(%s) ", operator));
        }
        buf.append("*/");
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EsHint)) {
            return false;
        }
        return equals((EsHint) obj);
    }

    private boolean equals(EsHint other) {
        return StringUtils.equals(index, other.index) && StringUtils.equals(analyzer, other.analyzer)
                && StringUtils.equals(operator, other.operator);
    }

    /**
     * Get Index field hints
     */
    public List<FieldHint> getIndex() {
        if (index == null) {
            return Collections.emptyList();
        }
        String[] fields = index.split(",");
        return Arrays.stream(fields).map(FieldHint::new).collect(Collectors.toList());
    }

    /**
     * Get Index field names (without boost)
     */
    public String[] getIndexFieldNames() {
        return getIndex().stream().map(FieldHint::getField).toArray(String[]::new);
    }

    /**
     * A field specified using a hint, with optional boost value
     */
    public static class FieldHint {
        public static final float DEFAULT_BOOST = 1.0F;

        protected final String field;

        protected final float boost;

        public FieldHint(String indexField) {
            String[] parsed = indexField.split("\\^");
            this.field = parsed[0];
            if (parsed.length > 1) {
                this.boost = Float.parseFloat(parsed[1]);
            } else {
                this.boost = DEFAULT_BOOST;
            }
        }

        public FieldHint(String field, Float boost) {
            this.field = field;
            this.boost = boost == null ? DEFAULT_BOOST : boost;
        }

        public String getField() {
            return field;
        }

        public float getBoost() {
            return boost;
        }
    }
}
