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

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;

public abstract class AbstractNegotiator implements Negotiator {

    private static final String SPEC_PREFIX = "nxtheme://theme";

    // FIXME: can be called 'web' under webengine
    private static final String DEFAULT_NEGOTIATION_STRATEGY = "default";

    protected final String strategy;

    protected final Object context;

    protected final HttpServletRequest request;

    public abstract String getTemplateEngineName();

    protected AbstractNegotiator(String strategy, Object context,
            HttpServletRequest request) {
        this.strategy = strategy;
        this.context = context;
        this.request = request;
    }

    public final String getSpec() throws NegotiationException {
        return String.format("%s/%s/%s/%s/%s/%s/%s", SPEC_PREFIX,
                negotiate("engine"), negotiate("mode"),
                getTemplateEngineName(), negotiate("theme"),
                negotiate("perspective"), negotiate("collection"));
    }

    public final synchronized String negotiate(String object)
            throws NegotiationException {
        if (strategy == null) {
            throw new NegotiationException("No negotiation strategy is set.");
        }
        NegotiationType negotiation = (NegotiationType) Manager.getTypeRegistry().lookup(
                TypeFamily.NEGOTIATION,
                String.format("%s/%s", strategy, object));
        // Try with the 'default' strategy
        if (negotiation == null) {
            negotiation = (NegotiationType) Manager.getTypeRegistry().lookup(
                    TypeFamily.NEGOTIATION,
                    String.format("%s/%s", DEFAULT_NEGOTIATION_STRATEGY, object));
        }
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
        } else {
            // add result to the request
            request.setAttribute(NEGOTIATION_RESULT_PREFIX + object, outcome);
        }
        return outcome;
    }
}
