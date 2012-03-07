/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventContext;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@XObject("videoProvider")
public class VideoProvider implements Cloneable {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@default")
    protected boolean isDefault = false;

    @XNodeList(value = "facets", type = ArrayList.class, componentType = String.class)
    protected List<String> facets = new ArrayList<String>();

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    @XNode("videoPlayerTemplate")
    protected String videoPlayerTemplate;

    @XNode("@keepOriginal")
    protected boolean keepOriginal = true;

    protected VideoProviderHandler videoProviderHandler;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public void setFacets(List<String> facets) {
        this.facets = facets;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setVideoPlayerTemplate(String videoPlayerTemplate) {
        this.videoPlayerTemplate = videoPlayerTemplate;
    }

    public void setKeepOriginal(boolean keepOriginal) {
        this.keepOriginal = keepOriginal;
    }

    @XNode("class")
    public void setVideoProviderHandler(
            Class<? extends VideoProviderHandler> videoProviderHandlerClass) {
        try {
            videoProviderHandler = videoProviderHandlerClass.newInstance();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    public VideoProviderHandler getVideoProviderHandler() {
        return videoProviderHandler;
    }

    public void setVideoProviderHandler(
            VideoProviderHandler videoProviderHandler) {
        this.videoProviderHandler = videoProviderHandler;
    }

    public List<String> getFacets() {
        return facets;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getVideoPlayerTemplate() {
        return videoPlayerTemplate;
    }

    public boolean isKeepOriginal() {
        return keepOriginal;
    }

    @Override
    public VideoProvider clone() throws CloneNotSupportedException {
        return (VideoProvider) super.clone();
    }

    public void onVideoCreated(DocumentModel doc, EventContext ctx) {
        // Add the contributed facets
        for (String facet : facets) {
            if (!doc.hasFacet(facet)) {
                doc.addFacet(facet);
            }
        }
        videoProviderHandler.onVideoCreated(this, doc, ctx);
    }

    public void onVideoModified(DocumentModel doc, EventContext ctx) {
        videoProviderHandler.onVideoModified(this, doc, ctx);
    }

    public void onVideoRemoved(DocumentModel doc, EventContext ctx) {
        videoProviderHandler.onVideoRemoved(this, doc, ctx);
    }

}
