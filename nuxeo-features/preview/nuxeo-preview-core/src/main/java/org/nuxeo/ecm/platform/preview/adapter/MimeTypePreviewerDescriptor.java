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

package org.nuxeo.ecm.platform.preview.adapter;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexandre Russel
 */
@XObject("previewer")
public class MimeTypePreviewerDescriptor {

    @XNodeList(value = "pattern", type = ArrayList.class, componentType = String.class)
    private List<String> patterns;

    @XNode("@class")
    private Class<? extends MimeTypePreviewer> klass;

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public Class<? extends MimeTypePreviewer> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends MimeTypePreviewer> klass) {
        this.klass = klass;
    }

}
