/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.apidoc.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;

public class SimpleDocumentationItem implements DocumentationItem {

    protected List<String> applicableVersion = new ArrayList<String>();

    protected String content ="";
    protected String id = null;
    protected String renderingType="";
    protected String target="";
    protected String targetType="";
    protected String title="";
    protected String type="";
    protected String uuid="";
    protected boolean approved=false;
    protected Map<String, String> attachements = new LinkedMap();

    public SimpleDocumentationItem() {
    }

    public SimpleDocumentationItem(NuxeoArtifact nxItem) {
        target = nxItem.getId();
        targetType = nxItem.getArtifactType();
    }

    public List<String> getApplicableVersion() {
        return applicableVersion;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public String getRenderingType() {
        return renderingType;
    }

    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return "";
    }

    public String getUUID() {
        return uuid;
    }

    public boolean isApproved() {
        return approved;
    }

    public Map<String, String> getAttachements() {
        return attachements;
    }

}
