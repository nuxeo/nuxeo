/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.jsf.negotiation.JSFNegotiator;
import org.nuxeo.theme.negotiation.NegotiationException;
import org.nuxeo.theme.types.TypeFamily;

import com.sun.faces.facelets.FaceletContextImplBase;
import com.sun.faces.facelets.TemplateClient;
import com.sun.faces.facelets.el.VariableMapperWrapper;
import com.sun.faces.facelets.tag.TagHandlerImpl;
import com.sun.faces.facelets.tag.ui.DefineHandler;
import com.sun.faces.facelets.tag.ui.ParamHandler;

/**
 * @author Jacob Hookom
 * @version $Id: CompositionHandler.java,v 1.15 2009/02/02 22:58:59 driscoll
 *          Exp $
 */
public final class CompositionHandler extends TagHandlerImpl implements
        TemplateClient {

    private static final Log log = LogFactory.getLog(CompositionHandler.class);

    public static final String Name = "theme";

    protected final Map<String, DefineHandler> handlers;

    protected final TagAttribute strategyAttribute;

    protected final ParamHandler[] params;

    static {
        Manager.initializeProtocols();
    }

    /**
     * @param config
     */
    public CompositionHandler(TagConfig config) {
        super(config);

        handlers = new HashMap<String, DefineHandler>();
        Iterator<DefineHandler> itr = this.findNextByType(DefineHandler.class);
        DefineHandler d = null;
        while (itr.hasNext()) {
            d = itr.next();
            this.handlers.put(d.getName(), d);
            log.debug(tag + " found Define[" + d.getName() + "]");
        }
        final List paramC = new ArrayList();
        itr = this.findNextByType(ParamHandler.class);
        while (itr.hasNext()) {
            paramC.add(itr.next());
        }
        if (paramC.size() > 0) {
            this.params = new ParamHandler[paramC.size()];
            for (int i = 0; i < this.params.length; i++) {
                this.params[i] = (ParamHandler) paramC.get(i);
            }
        } else {
            this.params = null;
        }

        strategyAttribute = getAttribute("strategy");
    }

    /*
     * (non-Javadoc)
     * @see
     * com.sun.facelets.FaceletHandler#apply(com.sun.facelets.FaceletContext,
     * javax.faces.component.UIComponent)
     */
    @Override
    public void apply(FaceletContext ctxObj, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        FaceletContextImplBase ctx = (FaceletContextImplBase) ctxObj;

        VariableMapper orig = ctx.getVariableMapper();
        if (this.params != null) {
            VariableMapper vm = new VariableMapperWrapper(orig);
            ctx.setVariableMapper(vm);
            for (int i = 0; i < this.params.length; i++) {
                this.params[i].apply(ctx, parent);
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
                    requestMap.put("org.nuxeo.theme.default.theme",
                            negotiation.getDefaultTheme());
                    requestMap.put("org.nuxeo.theme.default.engine",
                            negotiation.getDefaultEngine());
                    requestMap.put("org.nuxeo.theme.default.perspective",
                            negotiation.getDefaultPerspective());
                    strategy = negotiation.getStrategy();
                }
            }
            // override startegy if defined
            if (strategyAttribute != null) {
                strategy = strategyAttribute.getValue(ctx);
            }
            String contextPath = BaseURL.getContextPath();
            if (strategy == null) {
                log.error("Could not obtain the negotiation strategy for "
                        + root);
                external.redirect(contextPath
                        + "/nxthemes/error/negotiationStrategyNotSet.faces");

            } else {
                try {
                    final String spec = new JSFNegotiator(strategy,
                            facesContext,
                            (HttpServletRequest) external.getRequest()).getSpec();
                    final URL themeUrl = new URL(spec);
                    requestMap.put("org.nuxeo.theme.url", themeUrl);
                    ctx.includeFacelet(parent, themeUrl);
                } catch (NegotiationException e) {
                    log.error("Could not get default negotiation settings.", e);
                    external.redirect(contextPath
                            + "/nxthemes/error/negotiationDefaultValuesNotSet.faces");
                }
            }

        } finally {
            ctx.popClient(this);
            ctx.setVariableMapper(orig);
        }

    }

    @Override
    public boolean apply(FaceletContext ctx, UIComponent parent, String name)
            throws IOException, FacesException, FaceletException, ELException {
        if (name != null) {
            if (this.handlers == null) {
                return false;
            }
            DefineHandler handler = this.handlers.get(name);
            if (handler != null) {
                handler.applyDefinition(ctx, parent);
                return true;
            } else {
                return false;
            }
        } else {
            this.nextHandler.apply(ctx, parent);
            return true;
        }
    }

}
