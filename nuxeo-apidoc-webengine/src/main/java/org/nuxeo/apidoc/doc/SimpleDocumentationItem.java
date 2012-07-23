/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.doc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;

public class SimpleDocumentationItem extends AbstractDocumentationItem
        implements DocumentationItem {

    protected final List<String> applicableVersion = new ArrayList<String>();

    protected String content = "";

    protected String id;

    protected String renderingType = "";

    protected String target = "";

    protected String targetType = "";

    protected String title = "";

    protected String type = "";

    protected String uuid = "";

    protected boolean approved = false;

    protected Map<String, String> attachments = new LinkedHashMap<String, String>();

    public SimpleDocumentationItem() {
    }

    public SimpleDocumentationItem(NuxeoArtifact nxItem) {
        target = nxItem.getId();
        targetType = nxItem.getArtifactType();
    }

    @Override
    public List<String> getApplicableVersion() {
        return applicableVersion;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRenderingType() {
        return renderingType;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getTargetType() {
        return targetType;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTypeLabel() {
        return "";
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public boolean isApproved() {
        return approved;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public boolean isPlaceHolder() {
        return false;
    }

    @Override
    public String getEditId() {
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

}
