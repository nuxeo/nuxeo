/*
 * (C) Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 */

package org.nuxeo.ecm.web.resources.jsf;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.render.Renderer;

import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import com.sun.faces.util.FacesLogger;

/**
 * Override of the default JSF class to override #encodeChildren
 *
 * @since 7.4
 */
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public abstract class ScriptStyleBaseRenderer extends Renderer implements ComponentSystemEventListener {

    private static final String COMP_KEY = ScriptStyleBaseRenderer.class.getName() + "_COMPOSITE_COMPONENT";

    // Log instance for this class
    protected static final Logger logger = FacesLogger.RENDERKIT.getLogger();

    /*
     * Indicates that the component associated with this Renderer has already been added to the facet in the view.
     */

    /*
     * When this method is called, we know that there is a component with a script renderer somewhere in the view. We
     * need to make it so that when an element with a name given by the value of the optional "target" component
     * attribute is encountered, this component can be called upon to render itself. This method will add the component
     * (associated with this Renderer) to a facet in the view only if a "target" component attribute is set.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        UIComponent component = event.getComponent();
        if (ComponentUtils.isRelocated(component)) {
            return;
        }
        String target = verifyTarget((String) component.getAttributes().get("target"));
        if (target != null) {
            ComponentUtils.relocate(component, target, COMP_KEY);
        }
    }

    @Override
    public final void decode(FacesContext context, UIComponent component) {
        // no-op
    }

    @Override
    public final boolean getRendersChildren() {
        return true;
    }

    /**
     * If overridden, this method (i.e. super.encodeEnd) should be called <em>last</em> within the overridding
     * implementation.
     */
    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {

        // Remove the key to prevent issues with state saving...
        String ccID = (String) component.getAttributes().get(COMP_KEY);
        if (ccID != null) {
            // the first pop maps to the component we're rendering.
            // the second pop maps to the composite component that was pushed
            // in this renderer's encodeBegin implementation.
            // re-push the current component to reset the original context
            component.popComponentFromEL(context);
            component.popComponentFromEL(context);
            component.pushComponentToEL(context, component);
        }
    }

    /**
     * If overridden, this method (i.e. super.encodeBegin) should be called <em>first</em> within the overridding
     * implementation.
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {

        String ccID = (String) component.getAttributes().get(COMP_KEY);
        if (null != ccID) {
            UIComponent cc = context.getViewRoot().findComponent(':' + ccID);
            UIComponent curCC = UIComponent.getCurrentCompositeComponent(context);
            if (cc != curCC) {
                // the first pop maps to the component we're rendering.
                // push the composite component to the 'stack' and then re-push
                // the component we're rendering so the current component is
                // correct.
                component.popComponentFromEL(context);
                component.pushComponentToEL(context, cc);
                component.pushComponentToEL(context, component);
            }
        }

    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        encodeChildren(context, component, false);
    }

    public final void encodeChildren(FacesContext context, UIComponent component, boolean warnOnChildren)
            throws IOException {
        int childCount = component.getChildCount();
        boolean renderChildren = (0 < childCount);

        if (warnOnChildren) {
            Map<String, Object> attributes = component.getAttributes();
            boolean hasName = attributes.get("name") != null;
            boolean hasSrc = attributes.get("src") != null;
            // If we have no "name" attribute...
            if (!hasName && !hasSrc) {
                // and no child content...
                if (0 == childCount) {
                    // this is user error, so put up a message if desired
                    if (context.isProjectStage(ProjectStage.Development)) {
                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "outputScript with no library, no name, and no body content",
                                "Is body content intended?");
                        context.addMessage(component.getClientId(context), message);
                    }
                    // We have no children, but don't bother with the method
                    // invocation anyway.
                    renderChildren = false;
                }
            } else if (0 < childCount) {
                // If we have a "name" and also have child content, ignore
                // the child content and log a message.
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("outputScript with \"name\" attribute and nested content.  Ignoring nested content.");
                }
                renderChildren = false;
            }
        }
        if (renderChildren) {
            @SuppressWarnings("resource")
            ResponseWriter writer = context.getResponseWriter();
            startElement(writer, component);
            super.encodeChildren(context, component);
            endElement(writer);
        }

    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>
     * Allow the subclass to customize the start element content
     * </p>
     */
    protected abstract void startElement(ResponseWriter writer, UIComponent component) throws IOException;

    /**
     * <p>
     * Allow the subclass to customize the start element content
     * </p>
     */
    protected abstract void endElement(ResponseWriter writer) throws IOException;

    /**
     * <p>
     * Allow a subclass to control what's a valid value for "target".
     */
    protected String verifyTarget(String toVerify) {
        return ComponentUtils.verifyTarget(toVerify, toVerify);
    }

}
