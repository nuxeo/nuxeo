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
 *     Max Stepanov
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.domsync.core.events;

/**
 * @author Max Stepanov
 */
public class DOMAttrModifiedEvent extends DOMMutationEvent {

    private static final long serialVersionUID = -1903741037336659664L;

    private final String attrName;

    private final short attrChange;

    private final String newValue;

    public DOMAttrModifiedEvent(String target, String attrName, short attrChange, String newValue) {
        super(target);
        this.attrName = attrName;
        this.attrChange = attrChange;
        this.newValue = newValue;
    }

    public String getAttrName() {
        return attrName;
    }

    public short getAttrChange() {
        return attrChange;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DOMAttrModifiedEvent) {
            DOMAttrModifiedEvent other = (DOMAttrModifiedEvent) obj;
            return super.equals(obj) && attrName.equals(other.attrName) && attrChange == other.attrChange
                    && newValue.equals(other.newValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMAttrModifiedEvent " + super.toString() + " attrName=" + attrName + " attrChange=" + attrChange
                + " newValue=" + newValue;
    }

}
