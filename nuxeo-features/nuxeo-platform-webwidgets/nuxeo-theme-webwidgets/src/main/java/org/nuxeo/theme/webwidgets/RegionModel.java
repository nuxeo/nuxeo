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

package org.nuxeo.theme.webwidgets;

import org.nuxeo.theme.models.AbstractModel;

public final class RegionModel extends AbstractModel {

    public String name;

    public String provider;

    public String decoration;

    public RegionModel() {
    }

    public RegionModel(String name, String provider, String decoration) {
        this.name = name;
        this.provider = provider;
        this.decoration = decoration;
    }

}
