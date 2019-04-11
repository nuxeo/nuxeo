/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Aggregates a tag with its weight.
 *
 * @deprecated since 9.3, as we don't use the weight anymore
 */
@Deprecated
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The tag label.
     */
    public final String label;

    /**
     * The weight of the tag.
     */
    public long weight;

    public Tag(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + label + ',' + weight + ')';
    }

    protected static class TagLabelComparator implements Comparator<Tag> {
        @Override
        public int compare(Tag t1, Tag t2) {
            return t1.label.compareToIgnoreCase(t2.label);
        }
    }

    /**
     * Compare tags by label, case insensitive.
     */
    public static final Comparator<Tag> LABEL_COMPARATOR = new TagLabelComparator();

    protected static class TagWeightComparator implements Comparator<Tag> {
        @Override
        public int compare(Tag t1, Tag t2) {
            return t2.weight < t1.weight ? -1 : (t2.weight == t1.weight ? 0 : 1);
        }
    }

    /**
     * Compare tags by weight, decreasing.
     */
    public static final Comparator<Tag> WEIGHT_COMPARATOR = new TagWeightComparator();

}
