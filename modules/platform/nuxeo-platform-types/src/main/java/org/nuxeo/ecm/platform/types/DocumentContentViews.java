/*
 * (C) Copyright 2010-2021 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;

/**
 * Content view descriptor put on document types.
 *
 * @since 5.4
 */
@XObject("contentViews")
public class DocumentContentViews {

    @XNodeList(value = "contentView", type = DocumentContentView[].class, componentType = DocumentContentView.class)
    @XMerge(value = "@append", defaultAssignment = false) // compat
    protected DocumentContentView[] contentViews = new DocumentContentView[0];

    public DocumentContentView[] getContentViews() {
        return contentViews;
    }

    public String[] getContentViewNames() {
        if (contentViews != null) {
            String[] res = new String[contentViews.length];
            for (int i = 0; i < contentViews.length; i++) {
                res[i] = contentViews[i].getContentViewName();
            }
            return res;
        }
        return new String[0];
    }

    public String[] getContentViewNamesForExport() {
        List<String> res = new ArrayList<>();
        if (contentViews != null) {
            for (DocumentContentView contentView : contentViews) {
                if (contentView.getShowInExportView()) {
                    res.add(contentView.getContentViewName());
                }
            }
        }
        return res.toArray(new String[] {});
    }

}
