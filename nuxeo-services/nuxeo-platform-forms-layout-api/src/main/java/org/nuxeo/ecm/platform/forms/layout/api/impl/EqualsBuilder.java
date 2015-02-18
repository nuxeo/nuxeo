/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.forms.layout.api.impl;

/**
 * Utility class for definitions comparison.
 * <p>
 * Excerpts copied here to avoid dependencies to third party libs.
 *
 * @since 7.2
 */
public class EqualsBuilder {

    protected boolean isEquals = true;

    public boolean isEquals() {
        return isEquals;
    }

    protected void setEquals(boolean isEquals) {
        this.isEquals = isEquals;
    }

    @SuppressWarnings("rawtypes")
    public EqualsBuilder append(Object lhs, Object rhs) {
        if (isEquals == false) {
            return this;
        }
        if (lhs == rhs) {
            return this;
        }
        if (lhs == null || rhs == null) {
            this.setEquals(false);
            return this;
        }
        Class lhsClass = lhs.getClass();
        if (!lhsClass.isArray()) {
            // The simple case, not an array, just test the element
            isEquals = lhs.equals(rhs);
        } else if (lhs.getClass() != rhs.getClass()) {
            // Here when we compare different dimensions, for example: a boolean[][] to a boolean[]
            this.setEquals(false);
        } else if (lhs instanceof Object[]) {
            append((Object[]) lhs, (Object[]) rhs);
        } else {
            // assume there are no arrays of primitives
            append(lhs, rhs);
        }
        return this;
    }

    public EqualsBuilder append(long lhs, long rhs) {
        if (isEquals == false) {
            return this;
        }
        isEquals = (lhs == rhs);
        return this;
    }

    public EqualsBuilder append(boolean lhs, boolean rhs) {
        if (isEquals == false) {
            return this;
        }
        isEquals = (lhs == rhs);
        return this;
    }

    public EqualsBuilder append(Object[] lhs, Object[] rhs) {
        if (isEquals == false) {
            return this;
        }
        if (lhs == rhs) {
            return this;
        }
        if (lhs == null || rhs == null) {
            this.setEquals(false);
            return this;
        }
        if (lhs.length != rhs.length) {
            this.setEquals(false);
            return this;
        }
        for (int i = 0; i < lhs.length && isEquals; ++i) {
            append(lhs[i], rhs[i]);
        }
        return this;
    }

}