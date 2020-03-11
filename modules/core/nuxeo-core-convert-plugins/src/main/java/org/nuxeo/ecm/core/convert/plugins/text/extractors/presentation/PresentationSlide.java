/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors.presentation;

import java.io.Serializable;

/**
 * Representation of a presentation document slide with its string content and its order.
 */
public class PresentationSlide implements Serializable, Comparable<PresentationSlide> {

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
        return content == null && otherContent == null || content != null && content.equals(otherContent);
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
        return Integer.valueOf(order).compareTo(Integer.valueOf(other.getOrder()));
    }
}
