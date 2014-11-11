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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("provider")
public final class ProviderType {

    @XNode("@name")
    private String name;

    @XNode("description")
    private String description;

    @XNode("component-name")
    private String componentName;

    public String getName() {
        return name;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getDescription() {
        return description;
    }

}
