/**
 * Licensed under the Common Development and Distribution License,
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.sun.com/cddl/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.nuxeo.theme.jsf.facelets.vendor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.TemplateClient;
import com.sun.facelets.el.DefaultVariableMapper;
import com.sun.facelets.el.ELAdaptor;

/**
 * Default FaceletContext implementation.
 *
 * Copied from facelets-1.1.11 by Jean-Marc Orliaguet <jmo@chalmers.se> -
 * 2007/05/10.
 *
 * A single FaceletContext is used for all Facelets involved in an invocation of
 * {@link com.sun.facelets.Facelet#apply(FacesContext, UIComponent) Facelet#apply(FacesContext, UIComponent)}.
 * This means that included Facelets are treated the same as the JSP include
 * directive.
 *
 * @author Jacob Hookom
 * @version $Id: DefaultFaceletContext.java,v 1.4.4.3 2006/03/25 01:01:53 jhook
 *          Exp $
 */
@SuppressWarnings({"ALL"})
final class DefaultFaceletContext extends FaceletContext {

    private final FacesContext faces;

    private final ELContext ctx;

    private final DefaultFacelet facelet;

    private VariableMapper varMapper;

    private FunctionMapper fnMapper;

    private final Map ids;

    public DefaultFaceletContext(DefaultFaceletContext ctx,
            DefaultFacelet facelet) {
        this.ctx = ctx.ctx;
        this.facelet = facelet;
        this.clients = ctx.clients;
        this.faces = ctx.faces;
        this.fnMapper = ctx.fnMapper;
        this.ids = ctx.ids;
        this.varMapper = ctx.varMapper;
    }

    public DefaultFaceletContext(FacesContext faces, DefaultFacelet facelet) {
        this.ctx = ELAdaptor.getELContext(faces);
        this.ids = new HashMap();
        this.clients = new ArrayList(5);
        this.faces = faces;
        this.facelet = facelet;
        this.varMapper = this.ctx.getVariableMapper();
        if (this.varMapper == null) {
            this.varMapper = new DefaultVariableMapper();
        }
        this.fnMapper = this.ctx.getFunctionMapper();
    }

    @Override
    public FacesContext getFacesContext() {
        return this.faces;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return this.facelet.getExpressionFactory();
    }

    @Override
    public void setVariableMapper(VariableMapper varMapper) {
        // Assert.param("varMapper", varMapper);
        this.varMapper = varMapper;
    }

    @Override
    public void setFunctionMapper(FunctionMapper fnMapper) {
        // Assert.param("fnMapper", fnMapper);
        this.fnMapper = fnMapper;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.facelets.FaceletContext#includeFacelet(javax.faces.component.UIComponent,
     *      java.lang.String)
     */
    @Override
    public void includeFacelet(UIComponent parent, String relativePath)
            throws IOException, FaceletException, FacesException, ELException {
        this.facelet.include(this, parent, relativePath);
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return this.fnMapper;
    }

    @Override
    public VariableMapper getVariableMapper() {
        return this.varMapper;
    }

    @Override
    public Object getContext(Class key) {
        return this.ctx.getContext(key);
    }

    @Override
    public void putContext(Class key, Object contextObject) {
        this.ctx.putContext(key, contextObject);
    }

    @Override
    public String generateUniqueId(String base) {
        Integer cnt = (Integer) this.ids.get(base);
        if (cnt == null) {
            this.ids.put(base, new Integer(0));
            return base;
        } else {
            int i = cnt.intValue() + 1;
            this.ids.put(base, new Integer(i));
            return base + "_" + i;
        }
    }

    @Override
    public Object getAttribute(String name) {
        if (this.varMapper != null) {
            ValueExpression ve = this.varMapper.resolveVariable(name);
            if (ve != null) {
                return ve.getValue(this);
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.facelets.FaceletContext#setAttribute(java.lang.String,
     *      java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (this.varMapper != null) {
            if (value == null) {
                this.varMapper.setVariable(name, null);
            } else {
                this.varMapper.setVariable(
                        name,
                        this.facelet.getExpressionFactory().createValueExpression(
                                value, Object.class));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.facelets.FaceletContext#includeFacelet(javax.faces.component.UIComponent,
     *      java.net.URL)
     */
    @Override
    public void includeFacelet(UIComponent parent, URL absolutePath)
            throws IOException, FaceletException, FacesException, ELException {
        this.facelet.include(this, parent, absolutePath);
    }

    @Override
    public ELResolver getELResolver() {
        return this.ctx.getELResolver();
    }

    private final List clients;

    @Override
    public void popClient(TemplateClient client) {
        if (!this.clients.isEmpty()) {
            Iterator itr = this.clients.iterator();
            while (itr.hasNext()) {
                if (itr.next().equals(client)) {
                    itr.remove();
                    return;
                }
            }
        }
        throw new IllegalStateException(client + " not found");
    }

    @Override
    public void pushClient(final TemplateClient client) {
        this.clients.add(0, new TemplateManager(this.facelet, client));
    }

    @Override
    public void extendClient(final TemplateClient client) {
        this.clients.add(new TemplateManager(this.facelet, client));
    }

    @Override
    public boolean includeDefinition(UIComponent parent, String name)
            throws IOException, FaceletException, FacesException, ELException {
        boolean found = false;
        TemplateClient client;

        for (int i = 0; i < this.clients.size() && !found; i++) {
            client = ((TemplateClient) this.clients.get(i));
            if (client.equals(this.facelet)) {
                continue;
            }
            found = client.apply(this, parent, name);
        }

        return found;
    }

    private final static class TemplateManager implements TemplateClient {

        private final DefaultFacelet owner;

        private final TemplateClient target;

        private final Set names = new HashSet();

        public TemplateManager(DefaultFacelet owner, TemplateClient target) {
            this.owner = owner;
            this.target = target;
        }

        public boolean apply(FaceletContext ctx, UIComponent parent, String name)
                throws IOException, FacesException, FaceletException,
                ELException {
            String testName = (name != null) ? name : "facelets._NULL_DEF_";
            if (this.names.contains(testName)) {
                return false;
            } else {
                this.names.add(testName);
                boolean found = false;
                found = this.target.apply(new DefaultFaceletContext(
                        (DefaultFaceletContext) ctx, this.owner), parent, name);
                this.names.remove(testName);
                return found;
            }
        }

        @Override
        public boolean equals(Object o) {
            // System.out.println(this.owner.getAlias() + " == " +
            // ((DefaultFacelet) o).getAlias());
            return this.owner == o || this.target == o;
        }
    }
}
