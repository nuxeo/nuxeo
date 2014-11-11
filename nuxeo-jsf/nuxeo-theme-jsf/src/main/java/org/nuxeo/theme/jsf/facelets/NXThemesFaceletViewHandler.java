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

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletFactory;
import com.sun.facelets.FaceletViewHandler;
import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.impl.DefaultResourceResolver;
import com.sun.facelets.impl.ResourceResolver;
import com.sun.facelets.util.ReflectionUtil;

/**
 * ViewHandler implementation for Facelets
 *
 * @author Jacob Hookom
 * @version $Id: FaceletViewHandler.java,v 1.49.2.6 2006/03/20 07:22:00 jhook
 *          Exp $
 */
public class NXThemesFaceletViewHandler extends FaceletViewHandler {

    private static final Log log = LogFactory.getLog(NXThemesFaceletViewHandler.class);

    // Seam

    private static final String SEAM_EXPRESSION_FACTORY = "org.jboss.seam.el.SeamExpressionFactoryImpl";

    // Facelets
    @SuppressWarnings("hiding")
    private static final long DEFAULT_REFRESH_PERIOD = 2;

    private static final String PARAM_REFRESH_PERIOD = "facelets.REFRESH_PERIOD";

    @SuppressWarnings("hiding")
    private static final String PARAM_RESOURCE_RESOLVER = "facelets.RESOURCE_RESOLVER";

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
        String nuxRefreshPeriod = Framework.getProperty(PARAM_REFRESH_PERIOD);
        if (nuxRefreshPeriod != null && nuxRefreshPeriod.length() > 0) {
            refreshPeriod = Long.parseLong(nuxRefreshPeriod);
        } else {
            String userPeriod = ctx.getExternalContext().getInitParameter(
                    PARAM_REFRESH_PERIOD);
            if (userPeriod != null && userPeriod.length() > 0) {
                refreshPeriod = Long.parseLong(userPeriod);
            }
        }
        // resource resolver
        ResourceResolver resolver = new DefaultResourceResolver();
        String resolverName = ctx.getExternalContext().getInitParameter(
                PARAM_RESOURCE_RESOLVER);
        if (resolverName != null && resolverName.length() > 0) {
            try {
                resolver = (ResourceResolver) ReflectionUtil.forName(
                        resolverName).newInstance();
            } catch (Exception e) {
                throw new FacesException("Error Initializing ResourceResolver["
                        + resolverName + "]", e);
            }
        }

        return new NXThemesFaceletFactory(c, resolver, refreshPeriod);
    }

}
