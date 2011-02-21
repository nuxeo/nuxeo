/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Content view descriptor put on document types.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("contentViews")
public class DocumentContentViews implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@append")
    boolean append = false;

    @XNodeList(value = "contentView", type = DocumentContentView[].class, componentType = DocumentContentView.class)
    DocumentContentView[] contentViews = new DocumentContentView[0];

    public DocumentContentView[] getContentViews() {
        return contentViews;
    }

    public boolean getAppend() {
        return append;
    }

    public String[] getContentViewNames() {
        if (contentViews != null) {
            String[] res = new String[contentViews.length];
            for (int i = 0; i < contentViews.length; i++) {
                res[i] = contentViews[i].getContentViewName();
            }
            return res;
        }
        return null;
    }

    public String[] getContentViewNamesForExport() {
        List<String> res = new ArrayList<String>();
        if (contentViews != null) {
            for (int i = 0; i < contentViews.length; i++) {
                if (contentViews[i].getShowInExportView()) {
                    res.add(contentViews[i].getContentViewName());
                }
            }
        }
        return res.toArray(new String[] {});
    }
}
