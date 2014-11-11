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

import java.io.Serializable;

/**
 * @author Max Stepanov
 *
 */
public abstract class DOMMutationEvent implements Serializable {

    private static final long serialVersionUID = -2969614822761407105L;

    private final String target;

    protected DOMMutationEvent(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DOMMutationEvent) {
            return target.equals(((DOMMutationEvent) obj).target);
        }
        return false;
    }

    @Override
    public String toString() {
        return "target=" + target;
    }
}
