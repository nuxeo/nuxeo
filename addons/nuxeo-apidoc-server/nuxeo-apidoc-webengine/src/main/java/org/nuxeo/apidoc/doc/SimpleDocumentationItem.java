/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

public class SimpleDocumentationItem extends AbstractDocumentationItem implements DocumentationItem {

    protected final List<String> applicableVersion = new ArrayList<>();

    protected String content = "";

    protected String id;

    protected String renderingType = "";

    protected String target = "";

    protected String targetType = "";

    protected String title = "";

    protected String type = "";

    protected String uuid = "";

    protected boolean approved = false;

    protected Map<String, String> attachments = new LinkedHashMap<>();

    public SimpleDocumentationItem(String typeLabel) {
        super(typeLabel);
    }

    public SimpleDocumentationItem(NuxeoArtifact nxItem) {
        super(typeLabelOf(nxItem.getArtifactType()));
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
