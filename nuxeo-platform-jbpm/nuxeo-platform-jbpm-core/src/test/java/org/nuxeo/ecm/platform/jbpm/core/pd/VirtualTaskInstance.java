/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.pd;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author arussel
 *
 */
public class VirtualTaskInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> actors;

    public VirtualTaskInstance(String actor) {
        actors = Collections.singletonList(actor);
    }

    public VirtualTaskInstance(List<String> actors) {
        this.actors = actors;
    }

    public List<String> getActors() {
        return actors;
    }
}
