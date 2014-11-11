/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.api;

import java.io.Serializable;

public class AnnotationLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String XPointer;

    public AnnotationLocation(String xPointer) {
        this.XPointer = xPointer;
    }

    public String getXPointer() {
        return XPointer;
    }

    public void setXPointer(String pointer) {
        XPointer = pointer;
    }

}
