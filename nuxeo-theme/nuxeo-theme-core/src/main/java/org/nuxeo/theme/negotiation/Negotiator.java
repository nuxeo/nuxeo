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

package org.nuxeo.theme.negotiation;

public interface Negotiator {

    public static final String NEGOTIATION_RESULT_PREFIX = "org.nuxeo.theme.negotiation.result.";

    public static enum NEGOTIATION_OBJECT {
        engine, mode, theme, perspective, collection
    }

    String getSpec() throws NegotiationException;

    String negotiate(String object) throws NegotiationException;

}
