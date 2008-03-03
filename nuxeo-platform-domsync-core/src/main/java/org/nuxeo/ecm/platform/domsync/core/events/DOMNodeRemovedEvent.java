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
public class DOMNodeRemovedEvent extends DOMMutationEvent {

    private static final long serialVersionUID = -6175188243544084387L;

    public DOMNodeRemovedEvent(String target) {
        super(target);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DOMNodeRemovedEvent) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMNodeRemovedEvent "+super.toString();
    }

}
