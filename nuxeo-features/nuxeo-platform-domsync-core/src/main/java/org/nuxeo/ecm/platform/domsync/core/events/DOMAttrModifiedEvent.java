/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Max Stepanov
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.domsync.core.events;

/**
 * @author Max Stepanov
 *
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
        if(obj instanceof DOMAttrModifiedEvent) {
            DOMAttrModifiedEvent other = (DOMAttrModifiedEvent) obj;
            return super.equals(obj) && attrName.equals(other.attrName)
                    && attrChange == other.attrChange && newValue.equals(other.newValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMAttrModifiedEvent " + super.toString() + " attrName=" + attrName
                + " attrChange=" + attrChange + " newValue=" + newValue;
    }

}
