/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
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
