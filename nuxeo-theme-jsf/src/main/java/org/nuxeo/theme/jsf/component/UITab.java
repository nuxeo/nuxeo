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

package org.nuxeo.theme.jsf.component;

import java.io.IOException;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

public class UITab extends UIOutput {

    private String label;

    private String link;

    private String switchTo;

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSwitchTo() {
        return switchTo;
    }

    public void setSwitchTo(String switchTo) {
        this.switchTo = switchTo;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
