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
public class DOMNodeInsertedEvent extends DOMMutationEvent {

    private static final long serialVersionUID = 2062844930910763189L;

    private final int position;
    private final String fragment;

    public DOMNodeInsertedEvent(String target, String fragment, int position) {
        super(target);
        this.fragment = fragment;
        this.position = position;
    }

    public String getFragment() {
        return fragment;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DOMNodeInsertedEvent) {
            DOMNodeInsertedEvent other = (DOMNodeInsertedEvent) obj;
            return super.equals(obj) && position == other.position
                    && fragment.equals(other.fragment);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMNodeInsertedEvent " + super.toString() + " position=" + position
                + " fragment=" + fragment;
    }

}
