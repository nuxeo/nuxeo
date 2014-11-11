/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: FulltextFieldDescriptor.java 21255 2007-06-25 02:19:39Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Full text field descriptor.
 * <p>
 * Defines how the fulltext should be computed accross resources.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@XObject("fullText")
public class FulltextFieldDescriptor implements Serializable {

    private static final long serialVersionUID = 7345024263514631285L;

    @XNode("@name")
    protected String name;

    @XNode("@analyzer")
    protected String analyzer = "default";

    @XNode("@blobExtractorName")
    protected String blobExtractorName;

    @XNodeList(value = "field", type = ArrayList.class, componentType = String.class)
    protected List<String> resourceFields = new ArrayList<String>();

    @XNodeMap(value = "mimetype", key = "@name", type = LinkedHashMap.class, componentType = String.class)
    protected Map<String, String> mimetypes = new LinkedHashMap<String, String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public void setMimetypes(LinkedHashMap<String, String> mimetypes) {
        this.mimetypes = mimetypes;
    }

    public List<String> getResourceFields() {
        return resourceFields;
    }

    public void setResourceFields(List<String> resourceFields) {
        this.resourceFields = resourceFields;
    }

    public String lookupTransformer(String mimeType) {
        for (String pattern : mimetypes.keySet()) {
            if (mimeType.matches(pattern)) {
                return mimetypes.get(pattern);
            }
        }
        return null;
    }

    public String getBlobExtractorName() {
        return blobExtractorName;
    }

    public void setBlobExtractorName(String blobExtractorName) {
        this.blobExtractorName = blobExtractorName;
    }

}
