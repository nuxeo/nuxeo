/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger (troger@nuxeo.com)
 */

package org.nuxeo.ecm.platform.picture.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Object to store the definition of a picture conversion, to be used when computing views for a given image.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 7.1
 */
@XObject("pictureConversion")
@XRegistry(enable = false)
public class PictureConversion implements Comparable<PictureConversion> {

    private static final int DEFAULT_ORDER = 0;

    private static final boolean DEFAULT_ENABLED = true;

    private static final boolean DEFAULT_RENDITION_VISIBLE = true;

    private static final boolean DEFAULT_ISRENDITION = true;

    @XRegistryId
    @XNode(value = "@id", defaultAssignment = "")
    protected String id;

    @XNode("@order")
    protected Integer order;

    @XNode("@description")
    protected String description;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected Boolean enabled;

    @XNode("@chainId")
    protected String chainId;

    @XNode("@tag")
    protected String tag;

    @XNode("@maxSize")
    protected Integer maxSize;

    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    /**
     * @since 7.2
     */
    @XNode("@rendition")
    protected Boolean rendition;

    /**
     * @since 7.2
     */
    @XNode("@renditionVisible")
    protected Boolean renditionVisible;

    public PictureConversion() {
        super();
    }

    public PictureConversion(String id, String description, String tag, Integer maxSize) {
        this.id = id;
        this.description = description;
        this.tag = tag;
        this.maxSize = maxSize;
    }

    public String getId() {
        return id;
    }

    public int getOrder() {
        return order == null ? DEFAULT_ORDER : order.intValue();
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public boolean isEnabled() {
        return enabled == null ? DEFAULT_ENABLED : enabled.booleanValue();
    }

    public String getChainId() {
        return chainId;
    }

    /**
     * For compat with {@link org.nuxeo.ecm.platform.picture.api.PictureTemplate}.
     *
     * @deprecated since 7.1. Use {@link #getId()}.
     */
    @Deprecated
    public String getTitle() {
        return id;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(Boolean enabled) {
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

    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    public boolean isRenditionVisible() {
        return renditionVisible == null ? DEFAULT_RENDITION_VISIBLE : renditionVisible.booleanValue();
    }

    public boolean isRendition() {
        return rendition == null ? DEFAULT_ISRENDITION : rendition.booleanValue();
    }

    public void setRendition(Boolean rendition) {
        this.rendition = rendition;
    }

    public void setRenditionVisible(Boolean renditionVisible) {
        this.renditionVisible = renditionVisible;
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
    public String toString() {
        return String.format(
                "PictureConversion [id=%s, description=%s, tag=%s, maxSize=%d, order=%d, chainId=%s, enabled=%s]", id,
                description, tag, maxSize, order, chainId, enabled);
    }
}
