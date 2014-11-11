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
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.view.facelets.Facelet;

import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.facelets.impl.DefaultFaceletFactory;

public class UIFragment extends UIOutput {

    final String templateEngine = "jsf-facelets";

    private String uid;

    private String engine;

    private String mode;

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        Map<String, Object> attributes = getAttributes();
        uid = (String) attributes.get("uid");
        engine = (String) attributes.get("engine");
        mode = (String) attributes.get("mode");

        final ResponseWriter response = context.getResponseWriter();
        final UIViewRoot oldViewRoot = context.getViewRoot();

        // Create a string writer for rendering the view
        final StringWriter stringWriter = new StringWriter();
        context.setResponseWriter(response.cloneWithWriter(stringWriter));

        // Set up a transient view root
        final UIViewRoot viewRoot = new UIViewRoot();
        viewRoot.setRendererType(oldViewRoot.getRendererType());
        viewRoot.setRenderKitId(oldViewRoot.getRenderKitId());
        viewRoot.setViewId(oldViewRoot.getViewId());
        viewRoot.setLocale(oldViewRoot.getLocale());
        context.setViewRoot(viewRoot);

        // Render the view
        final String faceletId = String.format("nxtheme://element/%s/%s/%s/%s",
                engine, mode, templateEngine, uid);
        ApplicationAssociate associate = ApplicationAssociate.getCurrentInstance();
        DefaultFaceletFactory faceletFactory = associate.getFaceletFactory();
        final Facelet facelet = faceletFactory.getFacelet(context, faceletId);
        facelet.apply(context, viewRoot);
        renderChildren(context, viewRoot);

        // Write the rendered view into the response
        response.write(stringWriter.getBuffer().toString());

        // Restore the response and the original view root
        context.setResponseWriter(response);
        context.setViewRoot(oldViewRoot);
    }

    @Override
    public boolean isTransient() {
        // The UIFragment component is created dynamically by the
        // FragmentTag filter. It must be declared as transient otherwise it
        // will not be handled correctly by Facelets.
        return true;
    }

    private static void renderChildren(FacesContext context,
            UIComponent component) throws IOException {
        List<UIComponent> children = component.getChildren();
        for (Object child : children) {
            renderChild(context, (UIComponent) child);
        }
    }

    private static void renderChild(FacesContext context, UIComponent child)
            throws IOException {
        if (child.isRendered()) {
            child.encodeBegin(context);
            if (child.getRendersChildren()) {
                child.encodeChildren(context);
            } else {
                renderChildren(context, child);
            }
            child.encodeEnd(context);
        }
    }

    // Component properties
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
