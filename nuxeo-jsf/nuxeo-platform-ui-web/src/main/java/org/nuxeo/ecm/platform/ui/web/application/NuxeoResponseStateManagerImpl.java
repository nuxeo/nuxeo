/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 */

package org.nuxeo.ecm.platform.ui.web.application;

import static com.sun.faces.config.WebConfiguration.WebContextInitParameter.StateSavingMethod;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.application.StateManager;
import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.sun.faces.config.WebConfiguration;
import com.sun.faces.renderkit.ClientSideStateHelper;
import com.sun.faces.renderkit.ResponseStateManagerImpl;
import com.sun.faces.renderkit.StateHelper;
import com.sun.faces.util.RequestStateManager;

/**
 * @since 6.0
 */
public class NuxeoResponseStateManagerImpl extends ResponseStateManagerImpl {

    public static final String MULTIPART_SIZE_ERROR_FLAG = "NX_MULTIPART_SIZE_ERROR";

    public static final Object MULTIPART_SIZE_ERROR_COMPONENT_ID = "NX_MULTIPART_SIZE_COMPONENTID";

    private final StateHelper helper;

    public NuxeoResponseStateManagerImpl() {

        WebConfiguration webConfig = WebConfiguration.getInstance();
        String stateMode = webConfig.getOptionValue(StateSavingMethod);
        helper = ((StateManager.STATE_SAVING_METHOD_CLIENT.equalsIgnoreCase(stateMode) ? new ClientSideStateHelper()
                : new NuxeoServerSideStateHelper()));

    }

    // --------------------------------------- Methods from ResponseStateManager

    @Override
    public String getCryptographicallyStrongTokenFromSession(FacesContext context) {
        return helper.getCryptographicallyStrongTokenFromSession(context);
    }

    /**
     * @see ResponseStateManager#getState(javax.faces.context.FacesContext, java.lang.String)
     */
    @Override
    public Object getState(FacesContext context, String viewId) {

        Object state = RequestStateManager.get(context, RequestStateManager.FACES_VIEW_STATE);
        if (state == null) {
            try {
                state = helper.getState(context, viewId);
                if (state != null) {
                    RequestStateManager.set(context, RequestStateManager.FACES_VIEW_STATE, state);
                }
            } catch (IOException e) {
                throw new FacesException(e);
            }
        }
        return state;

    }

    /**
     * @see ResponseStateManager#writeState(javax.faces.context.FacesContext, java.lang.Object)
     */
    @Override
    public void writeState(FacesContext context, Object state) throws IOException {

        helper.writeState(context, state, null);

    }

    /**
     * @see ResponseStateManager#getViewState(javax.faces.context.FacesContext, java.lang.Object)
     */
    @Override
    public String getViewState(FacesContext context, Object state) {

        StringBuilder sb = new StringBuilder(32);
        try {
            helper.writeState(context, state, sb);
        } catch (IOException e) {
            throw new FacesException(e);
        }
        return sb.toString();

    }

    /**
     * @param facesContext the Faces context.
     * @param viewId the view id.
     * @return true if "stateless" was found, false otherwise.
     * @throws IllegalStateException when the request is not a postback.
     */
    @Override
    public boolean isStateless(FacesContext facesContext, String viewId) {
        return helper.isStateless(facesContext, viewId);
    }

    /**
     * @since 7.1
     */
    @Override
    public boolean isPostback(FacesContext context) {
        boolean result = super.isPostback(context);
        if (!result) {
            HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
            final String contentType = req.getContentType();
            if (contentType != null && contentType.contains("multipart/form-data")) {
                try {
                    req.getParts();
                } catch (IllegalStateException e) {
                    context.getAttributes().put(MULTIPART_SIZE_ERROR_FLAG, true);
                    if (e.getCause() != null) {
                        final String componentId = getComponentId(e.getCause().getMessage());
                        if (componentId != null) {
                            context.getAttributes().put(MULTIPART_SIZE_ERROR_COMPONENT_ID, componentId);
                        }
                    }
                } catch (IOException e) {
                    // Do nothing
                } catch (ServletException e) {
                    // Do nothing
                }
            }
        }
        return result;
    }

    /**
     * @since 7.1
     */
    private static String getComponentId(final String excetionMessage) {
        String sep = ":";
        if (excetionMessage.indexOf(sep) > 0) {
            String[] split = excetionMessage.split(sep);
            String result = "";
            if (split != null && split.length > 0) {
                result += split[0].substring(split[0].lastIndexOf(" ") + 1);
                for (int i = 1; i < split.length - 1; ++i) {
                    result += sep + split[i];
                }
                result += sep + split[split.length - 1].substring(0, split[split.length - 1].indexOf(' '));
            }
            return result;
        }
        return null;
    }
}
