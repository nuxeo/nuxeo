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

import java.net.URL;

import org.nuxeo.theme.jsf.facelets.vendor.DefaultFacelet;
import org.nuxeo.theme.jsf.facelets.vendor.DefaultFaceletFactory;

import com.sun.facelets.FaceletException;
import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.impl.ResourceResolver;

public final class NXThemesFaceletFactory extends DefaultFaceletFactory {

    public NXThemesFaceletFactory(Compiler compiler, ResourceResolver resolver) {
        this(compiler, resolver, -1);
    }

    public NXThemesFaceletFactory(Compiler compiler, ResourceResolver resolver,
            long refreshPeriod) {
        super(compiler, resolver, refreshPeriod);
    }

    @Override()
    protected boolean needsToBeRefreshed(DefaultFacelet facelet) {
        URL url = facelet.getSource();

        if (url.getProtocol().equals("nxtheme")) {
            try {
                if (url.openConnection().getLastModified() > facelet.getCreateTime()) {
                    return true;
                }
            } catch (Exception e) {
                throw new FaceletException("Error Checking Last Modified for "
                        + facelet.getAlias(), e);
            }

        } else if (refreshPeriod != -1) {
            long ttl = facelet.getCreateTime() + refreshPeriod;
            if (System.currentTimeMillis() > ttl) {
                try {
                    long atl = url.openConnection().getLastModified();
                    return atl == 0 || atl > ttl;
                } catch (Exception e) {
                    throw new FaceletException(
                            "Error Checking Last Modified for "
                                    + facelet.getAlias(), e);
                }
            }
        }

        return false;
    }

}
