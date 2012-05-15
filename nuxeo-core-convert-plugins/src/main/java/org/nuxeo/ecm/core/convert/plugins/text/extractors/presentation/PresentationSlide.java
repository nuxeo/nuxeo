/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors.presentation;

import java.io.Serializable;

/**
 * Representation of a presentation document slide with its string content and
 * its order.
 */
public class PresentationSlide implements Serializable,
        Comparable<PresentationSlide> {

    private static final long serialVersionUID = 1534438297504069864L;

    protected String content;

    protected int order;

    public PresentationSlide(String content, int order) {
        this.content = content;
        this.order = order;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof PresentationSlide)) {
            return false;
        }

        String otherContent = ((PresentationSlide) other).getContent();
        int otherOrder = ((PresentationSlide) other).getOrder();
        if (order != otherOrder) {
            return false;
        }
        return content == null && otherContent == null || content != null
                && content.equals(otherContent);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(content);
        sb.append(" (");
        sb.append(order);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int compareTo(PresentationSlide other) {
        return Integer.valueOf(order).compareTo(
                Integer.valueOf(other.getOrder()));
    }
}
