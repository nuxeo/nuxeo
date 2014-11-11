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

import java.util.List;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;

public abstract class AbstractNegotiator implements Negotiator {

    private static final String SPEC_PREFIX = "nxtheme://theme";

    private final String strategy;

    private final Object context;

    public abstract String getTemplateEngineName();

    protected AbstractNegotiator(String strategy, Object context) {
        this.strategy = strategy;
        this.context = context;
    }

    public final String getSpec() throws NegotiationException {
        return String.format("%s/%s/%s/%s/%s/%s", SPEC_PREFIX,
                negotiate("engine"), negotiate("mode"),
                getTemplateEngineName(), negotiate("theme"),
                negotiate("perspective"));
    }

    public final synchronized String negotiate(String object)
            throws NegotiationException {
        if (strategy == null) {
            throw new NegotiationException("No negotiation strategy is set.");
        }
        NegotiationType negotiation = (NegotiationType) Manager.getTypeRegistry().lookup(
                TypeFamily.NEGOTIATION,
                String.format("%s/%s", strategy, object));
        if (negotiation == null) {
            throw new NegotiationException("Could not obtain negotiation for: "
                    + strategy + " (strategy) " + object + " (object)");
        }
        final List<Scheme> schemes = negotiation.getSchemes();

        String outcome = null;
        if (schemes != null) {
            for (Scheme scheme : negotiation.getSchemes()) {
                outcome = scheme.getOutcome(context);
                if (outcome != null) {
                    break;
                }
            }
        }
        if (outcome == null) {
            throw new NegotiationException(
                    "No negotiation outcome found for:  " + strategy
                            + " (strategy) " + object + " (object)");
        }
        return outcome;
    }
}
