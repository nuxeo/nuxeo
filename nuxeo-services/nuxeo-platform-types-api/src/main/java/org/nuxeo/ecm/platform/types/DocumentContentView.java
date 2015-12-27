/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.types;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.4.2
 */
@XObject("contentView")
public class DocumentContentView {

    @XNode("@showInExportView")
    protected boolean showInExportView = true;

    @XContent
    protected String contentView;

    public boolean getShowInExportView() {
        return showInExportView;
    }

    public String getContentViewName() {
        if (contentView != null) {
            return contentView.trim();
        }
        return null;
    }

    /**
     * Clone to handle hot reload
     *
     * @since 5.6
     */
    @Override
    public DocumentContentView clone() {
        DocumentContentView clone = new DocumentContentView();
        clone.showInExportView = getShowInExportView();
        clone.contentView = getContentViewName();
        return clone;
    }

}
