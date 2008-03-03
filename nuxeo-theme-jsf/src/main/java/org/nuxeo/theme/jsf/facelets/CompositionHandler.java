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

package org.nuxeo.theme.jsf.facelets;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.jsf.negotiation.JSFNegotiator;
import org.nuxeo.theme.negotiation.NegotiationException;
import org.nuxeo.theme.types.TypeFamily;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.TemplateClient;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.ui.DefineHandler;
import com.sun.facelets.tag.ui.ParamHandler;

public final class CompositionHandler extends TagHandler implements
        TemplateClient {

    private static final Log log = LogFactory.getLog(CompositionHandler.class);

    public static final String Name = "theme";

    protected final Map<String, DefineHandler> handlers;

    protected final ParamHandler[] params;

    static {
        Manager.initializeProtocols();
    }

    /**
     * @param config
     */
    @SuppressWarnings("unchecked")
    public CompositionHandler(TagConfig config) {

        super(config);

        handlers = new HashMap<String, DefineHandler>();
        Iterator itr = findNextByType(DefineHandler.class);
        DefineHandler d;
        while (itr.hasNext()) {
            d = (DefineHandler) itr.next();
            handlers.put(d.getName(), d);
            log.debug(tag + " found Define[" + d.getName() + ']');
        }
        final List paramC = new ArrayList();
        itr = findNextByType(ParamHandler.class);
        while (itr.hasNext()) {
            paramC.add(itr.next());
        }
        if (!paramC.isEmpty()) {
            params = new ParamHandler[paramC.size()];
            for (int i = 0; i < params.length; i++) {
                params[i] = (ParamHandler) paramC.get(i);
            }
        } else {
            params = null;
        }

    }

    @SuppressWarnings("unchecked")
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        final VariableMapper orig = ctx.getVariableMapper();
        if (params != null) {
            VariableMapper vm = new VariableMapperWrapper(orig);
            ctx.setVariableMapper(vm);
            for (ParamHandler element : params) {
                element.apply(ctx, parent);
            }
        }
        ctx.extendClient(this);

        try {
            final FacesContext facesContext = ctx.getFacesContext();

            // Get the negotiation strategy
            final ExternalContext external = facesContext.getExternalContext();
            final Map<String, Object> requestMap = external.getRequestMap();
            final String root = external.getRequestContextPath();
            final ApplicationType application = (ApplicationType) Manager.getTypeRegistry().lookup(
                    TypeFamily.APPLICATION, root);
            String strategy = null;
            if (application != null) {
                final NegotiationDef negotiation = application.getNegotiation();
                if (negotiation != null) {
                    requestMap.put("nxthemesDefaultTheme",
                            negotiation.getDefaultTheme());
                    requestMap.put("nxthemesDefaultEngine",
                            negotiation.getDefaultEngine());
                    requestMap.put("nxthemesDefaultPerspective",
                            negotiation.getDefaultPerspective());
                    strategy = negotiation.getStrategy();
                }
            }

            if (strategy == null) {
                log.error("Could not obtain the negotiation strategy for "
                        + root);
                external.redirect("/nuxeo/nxthemes/error/negotiationStrategyNotSet.faces");

            } else {
                try {
                    final String spec = new JSFNegotiator(strategy,
                            facesContext).getSpec();
                    final URL themeUrl = new URL(spec);
                    requestMap.put("nxthemesThemeUrl", themeUrl);
                    ctx.includeFacelet(parent, themeUrl);
                } catch (NegotiationException e) {
                    log.error("Could not get default negotiation settings.", e);
                    external.redirect("/nuxeo/nxthemes/error/negotiationDefaultValuesNotSet.faces");
                }
            }

        } finally {
            ctx.popClient(this);
            ctx.setVariableMapper(orig);
        }
    }

    public boolean apply(FaceletContext ctx, UIComponent parent, String name)
            throws IOException, FacesException, ELException {
        if (name != null) {
            final FaceletHandler handler = handlers.get(name);
            if (handler != null) {
                handler.apply(ctx, parent);
                return true;
            } else {
                return false;
            }
        } else {
            nextHandler.apply(ctx, parent);
            return true;
        }

    }

}
