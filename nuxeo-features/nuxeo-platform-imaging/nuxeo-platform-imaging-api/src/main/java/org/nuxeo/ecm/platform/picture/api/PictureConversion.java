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
 * Object to store the definition of a picture conversion, to be used when
 * computing views for a given image.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 7.1
 */
@XObject("pictureConversion")
public class PictureConversion implements Comparable<PictureConversion> {

    @XNode("@id")
    protected String id;

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

    public PictureConversion() {
        super();
    }

    public PictureConversion(String id, String description, String tag,
                             Integer maxSize) {
        this(id, description, tag, maxSize, -1, null, true);
    }

    public PictureConversion(String id, String description, String tag,
                             Integer maxSize, Integer order, String chainId, Boolean enabled) {
        this.id = id;
        this.description = description;
        this.tag = tag;
        this.order = order;
        this.maxSize = maxSize;
        this.chainId = chainId;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public Integer getOrder() {
        return order == null ? 0 : order;
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getChainId() {
        return chainId;
    }

    /**
     * For compat with {@link org.nuxeo.ecm.platform.picture.api.PictureTemplate}.
     * @deprecated since 7.1. Use {@link #getId()}.
     */
    @Deprecated
    public String getTitle() {
        return id;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setOrder(Integer order) {
        this.order = order;
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

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int compareTo(PictureConversion other) {
        return Integer.compare(getOrder(), other.getOrder());
    }

    @Override
    public PictureConversion clone() {
        return new PictureConversion(id, description, tag, maxSize, order,
                chainId, enabled);
    }

    @Override
    public String toString() {
        return String.format(
                "PictureTemplate [title=%s, description=%s, tag=%s, maxSize=%d, order=%d, chainId=%s, enabled=%s]",
                id, description, tag, maxSize, order, chainId, enabled);
    }
}
