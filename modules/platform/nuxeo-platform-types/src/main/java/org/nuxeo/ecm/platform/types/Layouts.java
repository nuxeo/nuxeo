/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: Layouts.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.types;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Layout list descriptor
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("layouts")
public class Layouts {

    @XNode("@append")
    boolean append;

    @XNodeList(value = "layout", type = String[].class, componentType = String.class)
    String[] layouts = new String[0];

    public String[] getLayouts() {
        return layouts;
    }

    public boolean getAppend() {
        return append;
    }

    /**
     * Clone to handle hot reload
     *
     * @since 5.6
     */
    @Override
    protected Layouts clone() {
        Layouts clone = new Layouts();
        clone.append = getAppend();
        String[] layouts = getLayouts();
        if (layouts != null) {
            clone.layouts = layouts.clone();
        }
        return clone;
    }

}
