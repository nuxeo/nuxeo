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

package org.nuxeo.theme;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("negotiation")
public final class NegotiationDef {

    @XNode("strategy")
    private String strategy;

    @XNode("default-engine")
    private String defaultEngine;

    @XNode("default-theme")
    private String defaultTheme;

    @XNode("default-perspective")
    private String defaultPerspective;

    public String getDefaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(final String defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public String getDefaultPerspective() {
        return defaultPerspective;
    }

    public void setDefaultPerspective(final String defaultPerspective) {
        this.defaultPerspective = defaultPerspective;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public void setDefaultTheme(final String defaultTheme) {
        this.defaultTheme = defaultTheme;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(final String strategy) {
        this.strategy = strategy;
    }

}
