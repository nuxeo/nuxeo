/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.webengine.negotiation;

import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.negotiation.AbstractNegotiator;

public final class WebNegotiator extends AbstractNegotiator {

    public String getTemplateEngineName() {
        return "freemarker";
    }

    public WebNegotiator(final String strategy, final WebContext context) {
        super(strategy, context);
    }

}
