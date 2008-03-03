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

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;

import com.sun.facelets.FaceletFactory;
import com.sun.facelets.FaceletViewHandler;
import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.impl.DefaultResourceResolver;
import com.sun.facelets.impl.ResourceResolver;

public class NXThemesFaceletViewHandler extends FaceletViewHandler {

    // Seam
    private static final String SEAM_EXPRESSION_FACTORY = "org.jboss.seam.ui.facelet.SeamExpressionFactory";

    // Facelets
    private final static long DEFAULT_REFRESH_PERIOD = 2;

    private final static String PARAM_REFRESH_PERIOD = "facelets.REFRESH_PERIOD";

    private final static String PARAM_RESOURCE_RESOLVER = "facelets.RESOURCE_RESOLVER";

    public NXThemesFaceletViewHandler(ViewHandler parent) {
        super(parent);
    }

    @Override()
    protected Compiler createCompiler() {
        Compiler compiler = super.createCompiler();
        compiler.setFeature(Compiler.EXPRESSION_FACTORY,
                SEAM_EXPRESSION_FACTORY);
        return compiler;

    }

    @Override()
    protected FaceletFactory createFaceletFactory(Compiler c) {
        long refreshPeriod = DEFAULT_REFRESH_PERIOD;
        FacesContext ctx = FacesContext.getCurrentInstance();
        String userPeriod = ctx.getExternalContext().getInitParameter(
                PARAM_REFRESH_PERIOD);
        if (userPeriod != null && userPeriod.length() > 0) {
            refreshPeriod = Long.parseLong(userPeriod);
        }

        // resource resolver
        ResourceResolver resolver = new DefaultResourceResolver();
        String resolverName = ctx.getExternalContext().getInitParameter(
                PARAM_RESOURCE_RESOLVER);
        if (resolverName != null && resolverName.length() > 0) {
            try {
                resolver = (ResourceResolver) Class.forName(resolverName, true,
                        Thread.currentThread().getContextClassLoader()).newInstance();
            } catch (Exception e) {
                throw new FacesException("Error Initializing ResourceResolver["
                        + resolverName + "]", e);
            }
        }

        return new NXThemesFaceletFactory(c, resolver, refreshPeriod);
    }

}
