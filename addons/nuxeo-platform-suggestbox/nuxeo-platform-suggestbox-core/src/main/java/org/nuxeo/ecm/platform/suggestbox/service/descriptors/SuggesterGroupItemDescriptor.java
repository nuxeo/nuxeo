/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for registering suggester names in a suggesterGroup
 * contribution.
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
        return name == null && otherName == null || name != null
                && name.equals(otherName);
    }

    @Override
    public String toString() {
        return name;
    }
}
