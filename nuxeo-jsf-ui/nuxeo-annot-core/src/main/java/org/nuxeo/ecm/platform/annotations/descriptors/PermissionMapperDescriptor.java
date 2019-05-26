/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
@XObject("permissionMapper")
public class PermissionMapperDescriptor {

    @XNode("readAnnotation")
    private String readAnnotationValue;

    @XNode("createAnnotation")
    private String createAnnotationValue;

    @XNode("updateAnnotation")
    private String updateAnnotationValue;

    @XNode("deleteAnnotation")
    private String deleteAnnotationValue;

    public String getReadAnnotationValue() {
        return readAnnotationValue;
    }

    public void setReadAnnotationValue(String readAnnotationValue) {
        this.readAnnotationValue = readAnnotationValue;
    }

    public String getCreateAnnotationValue() {
        return createAnnotationValue;
    }

    public void setCreateAnnotationValue(String createAnnotationValue) {
        this.createAnnotationValue = createAnnotationValue;
    }

    public String getUpdateAnnotationValue() {
        return updateAnnotationValue;
    }

    public void setUpdateAnnotationValue(String updateAnnotationValue) {
        this.updateAnnotationValue = updateAnnotationValue;
    }

    public String getDeleteAnnotationValue() {
        return deleteAnnotationValue;
    }

    public void setDeleteAnnotationValue(String deleteAnnotationValue) {
        this.deleteAnnotationValue = deleteAnnotationValue;
    }

}
