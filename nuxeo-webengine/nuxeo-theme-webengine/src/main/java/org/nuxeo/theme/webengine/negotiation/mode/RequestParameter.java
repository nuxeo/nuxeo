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

package org.nuxeo.theme.webengine.negotiation.mode;

import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.negotiation.Scheme;

public final class RequestParameter implements Scheme {

    public String getOutcome(final Object context) {
        return ((WebContext) context).getRequest().getParameter("mode");
    }

}
