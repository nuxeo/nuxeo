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

package org.nuxeo.ecm.diff.content.adapter;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Antoine Taillefer
 * @since 5.6
 */
@XObject("contentDiffer")
public class MimeTypeContentDifferDescriptor {

    /**
     * @since 7.4
     */
    @XNode("@name")
    private String name;

    @XNode("pattern")
    private String pattern;

    @XNode("@class")
    private Class<? extends MimeTypeContentDiffer> klass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Class<? extends MimeTypeContentDiffer> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends MimeTypeContentDiffer> klass) {
        this.klass = klass;
    }

}
