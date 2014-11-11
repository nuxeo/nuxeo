/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.html;

public class WebLength {

    final Integer value;

    final String unit;

    public WebLength(Integer value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return String.format("%s%s", value, unit);
    }

}
