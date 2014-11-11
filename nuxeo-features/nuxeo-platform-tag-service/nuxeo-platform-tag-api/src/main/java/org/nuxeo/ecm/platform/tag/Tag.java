/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Aggregates a tag with its weight.
 */
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

    protected static class TagLabelComparator implements Comparator<Tag> {
        public int compare(Tag t1, Tag t2) {
            return t1.label.compareToIgnoreCase(t2.label);
        }
    }

    /**
     * Compare tags by label, case insensitive.
     */
    public static final Comparator<Tag> LABEL_COMPARATOR = new TagLabelComparator();

    protected static class TagWeightComparator implements Comparator<Tag> {
        public int compare(Tag t1, Tag t2) {
            return t2.weight < t1.weight ? -1
                    : (t2.weight == t1.weight ? 0 : 1);
        }
    }

    /**
     * Compare tags by weight, decreasing.
     */
    public static final Comparator<Tag> WEIGHT_COMPARATOR = new TagWeightComparator();

}
