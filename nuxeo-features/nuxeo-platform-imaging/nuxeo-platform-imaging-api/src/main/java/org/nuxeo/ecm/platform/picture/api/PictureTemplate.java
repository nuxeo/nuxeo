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
 *     Thomas Roger (troger@nuxeo.com)
 */

package org.nuxeo.ecm.platform.picture.api;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Object to store the definition of a picture template, to be used when
 * computing views for a given image.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@XObject("pictureTemplate")
public class PictureTemplate implements Comparable<PictureTemplate> {

    @XNode("@title")
    protected String title;

    @XNode("@order")
    protected Integer order;

    @XNode("@description")
    protected String description;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@chainId")
    protected String chainId;

    protected String tag;

    @XNode("@maxSize")
    protected Integer maxSize;

    public PictureTemplate() {
        super();
    }

    public PictureTemplate(String title, String description, String tag,
            Integer maxSize) {
        this(title, description, tag, maxSize, -1, null, true);
    }

    public PictureTemplate(String title, String description, String tag,
            Integer maxSize, Integer order, String chainId, Boolean enabled) {
        this.title = title;
        this.description = description;
        this.tag = tag;
        this.order = order;
        this.maxSize = maxSize;
        this.chainId = chainId;
        this.enabled = enabled;
    }

    @Override
    public int compareTo(PictureTemplate other) {
        return Integer.compare(order, other.order);
    }

    public String getChainId() {
        return chainId;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public Integer getSafeMaxSize() {
        return maxSize == null ? 1 : maxSize;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getOrder() {
        return order;
    }

    @Override
    public PictureTemplate clone() {
        return new PictureTemplate(title, description, tag, maxSize, order,
                chainId, enabled);
    }

    @Override
    public String toString() {
        return String.format(
                "PictureTemplate [title=%s, description=%s, tag=%s, maxSize=%d, order=%d, chainId=%s, enabled=%s]",
                title, description, tag, maxSize, order, chainId, enabled);
    }
}