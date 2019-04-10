/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for registering suggester names in a suggesterGroup contribution.
 *
 * @author ataillefer
 */
@XObject("suggesterName")
public class SuggesterGroupItemDescriptor implements Cloneable {

    @XNode("")
    protected String name;

    @XNode("@appendBefore")
    protected String appendBefore;

    @XNode("@appendAfter")
    protected String appendAfter;

    @XNode("@remove")
    protected boolean remove;

    // default constructor
    public SuggesterGroupItemDescriptor() {
    }

    public SuggesterGroupItemDescriptor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAppendBefore() {
        return appendBefore;
    }

    public String getAppendAfter() {
        return appendAfter;
    }

    public boolean isRemove() {
        return remove;
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object other) {

        if (other == this) {
            return true;
        }
        if (other == null || !(other instanceof SuggesterGroupItemDescriptor)) {
            return false;
        }

        String otherName = ((SuggesterGroupItemDescriptor) other).getName();
        return name == null && otherName == null || name != null && name.equals(otherName);
    }

    @Override
    public String toString() {
        return name;
    }
}
