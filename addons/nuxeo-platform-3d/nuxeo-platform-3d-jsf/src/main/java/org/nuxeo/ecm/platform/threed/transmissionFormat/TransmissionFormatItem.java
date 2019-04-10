/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.transmissionFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.threed.ThreeDInfo;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

import java.io.Serializable;
import java.util.Map;

/**
 * Small DTO to precompute transmission formats URLs for JSF
 *
 * @since 8.4
 */
public class TransmissionFormatItem {

    public static final Log log = LogFactory.getLog(org.nuxeo.ecm.platform.threed.renderView.RenderViewItem.class);

    protected final DocumentModel doc;

    protected final int position;

    protected final String blobPropertyName;

    protected String filename;

    protected Long percPoly;

    protected Long maxPoly;

    protected Long percTex;

    protected String maxTex;

    protected Long size;

    protected ThreeDInfo info;

    protected String name;

    public TransmissionFormatItem(DocumentModel doc, String basePropertyPath, int position) {
        this.doc = doc;
        this.position = position;
        String propertyPath = basePropertyPath + "/" + position;
        blobPropertyName = propertyPath + "/content";
        try {
            Blob blob = (Blob) doc.getPropertyValue(blobPropertyName);
            filename = blob.getFilename();
            percPoly = (Long) doc.getPropertyValue(propertyPath + "/percPoly");
            maxPoly = (Long) doc.getPropertyValue(propertyPath + "/maxPoly");
            percTex = (Long) doc.getPropertyValue(propertyPath + "/percTex");
            maxTex = (String) doc.getPropertyValue(propertyPath + "/maxTex");
            name = (String) doc.getPropertyValue(propertyPath + "/name");
            info = new ThreeDInfo((Map<String, Serializable>) doc.getPropertyValue(propertyPath + "/info"));
            size = blob.getLength();
        } catch (PropertyException e) {
            log.warn(e);
        }
    }

    private String formatAsReadable(long value, long base, String unit) {
        if (value < base) {
            return value + " " + unit;
        }
        int exp = (int) (Math.log(value) / Math.log(base));
        String pre = String.valueOf(("kMGTPE").charAt(exp - 1));
        return String.format("%.1f %s%s", value / Math.pow(base, exp), pre, unit);
    }

    public String getSrc() {
        return DocumentModelFunctions.bigFileUrl(doc, blobPropertyName, filename);
    }

    public String getPercPoly() {
        return (percPoly == null) ? null : percPoly.toString();
    }

    public String getMaxPoly() {
        if (maxPoly == null) {
            return null;
        }
        return formatAsReadable(maxPoly, 1000, "");
    }

    public String getPercTex() {
        return (percTex == null) ? null : percTex.toString();
    }

    public String getMaxTex() {
        return (maxTex == null) ? null : maxTex;
    }

    public String getSize() {
        return size.toString();
    }

    public ThreeDInfo getInfo() {
        return info;
    }

    public String getPolygons() {
        Long polygons = info.getPolygons();
        if (polygons == null) {
            return "-";
        }
        return formatAsReadable(polygons, 1000, "");
    }

    public String getTextureSize() {
        Long texturesSize = info.getTexturesSize();
        if (texturesSize == null) {
            return "-";
        }
        return formatAsReadable(texturesSize, 1024, "B");
    }

    public Boolean getHasTextures() {
        return info.getTexturesSize() > 0;
    }

    public Boolean getGeometryLodSuccess() {
        return info.getGeometryLodSuccess();
    }

    public Boolean getTextureLodSuccess() {
        return info.getTextureLodSuccess();
    }

    public String getName() {
        return name;
    }

}
