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

package org.nuxeo.theme.models;

public final class Region extends AbstractModel {

    public final String name;

    public final String defaultBody;

    public final String defaultSrc;

    public Region(String name, String defaultBody, String defaultSrc) {
        this.name = name;
        this.defaultBody = defaultBody;
        this.defaultSrc = defaultSrc;
    }

    public Region() {
        this(null, null, null);
    }

    public Region(String name, String defaultBody) {
        this(name, defaultBody, null);
    }

}
