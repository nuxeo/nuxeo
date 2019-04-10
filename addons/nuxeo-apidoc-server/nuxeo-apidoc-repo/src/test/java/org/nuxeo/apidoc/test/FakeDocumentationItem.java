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
package org.nuxeo.apidoc.test;

import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.DocumentationItem;

public class FakeDocumentationItem extends AbstractDocumentationItem implements DocumentationItem {

    public List<String> applicableVersion;

    public String content;

    public String id;

    public String renderingType;

    public String target;

    public String targetType;

    public String title;

    public String type;

    public String typeLabel;

    public String uuid;

    public boolean approved = false;


    public FakeDocumentationItem(DocumentationItem item) {
        super(item.getTypeLabel());
        applicableVersion = item.getApplicableVersion();
        content = item.getContent();
        id = item.getId();
        renderingType = item.getRenderingType();
        target = item.getTarget();
        title = item.getTitle();
        type = item.getType();
        typeLabel = item.getTypeLabel();
        uuid = item.getUUID();
        approved = item.isApproved();
        type = item.getTargetType();
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
    public String getTitle() {
        return title;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTypeLabel() {
        return typeLabel;
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
    public String getTargetType() {
        return targetType;
    }

    @Override
    public Map<String, String> getAttachments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPlaceHolder() {
        return false;
    }

    @Override
    public String getEditId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

}
