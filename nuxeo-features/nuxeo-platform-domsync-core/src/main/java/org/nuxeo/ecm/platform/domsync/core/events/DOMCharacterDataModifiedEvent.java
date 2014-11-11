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
public class DOMCharacterDataModifiedEvent extends DOMMutationEvent {

    private static final long serialVersionUID = -5523031456812911018L;

    private final String newValue;

    public DOMCharacterDataModifiedEvent(String target, String newValue) {
        super(target);
        this.newValue = newValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DOMCharacterDataModifiedEvent) {
            return super.equals(obj)
                    && newValue.equals(((DOMCharacterDataModifiedEvent) obj).newValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMCharacterDataModifiedEvent " + super.toString() + " newValue=" + newValue;
    }
}
