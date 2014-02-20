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
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;

import com.sun.faces.facelets.TemplateClient;
import com.sun.faces.facelets.el.DefaultVariableMapper;

/**
 * Default FaceletContext implementation.
 * <p>
 * Copied from facelets-1.1.15.B1 by Jean-Marc Orliaguet <jmo@chalmers.se> -
 * 2010/10/24.
 * <p>
 * A single FaceletContext is used for all Facelets involved in an invocation
 * of {@link com.sun.facelets.Facelet#apply(FacesContext, UIComponent)
 * Facelet#apply(FacesContext, UIComponent)}. This means that included Facelets
 * are treated the same as the JSP include directive.
 *
 * @author Jacob Hookom
 * @version $Id: DefaultFaceletContext.java,v 1.4.4.3 2006/03/25 01:01:53 jhook
 *          Exp $
 */
final class DefaultFaceletContext extends FaceletContext {

    private final FacesContext faces;

    private final ELContext ctx;

    private final DefaultFacelet facelet;

    private final List faceletHierarchy;

    private VariableMapper varMapper;

    private FunctionMapper fnMapper;

    private final Map ids;

    private final Map prefixes;

    private String prefix;

    private final StringBuilder uniqueIdBuilder = new StringBuilder(30);

    public DefaultFaceletContext(DefaultFaceletContext ctx,
            DefaultFacelet facelet) {
        this.ctx = ctx.ctx;
        this.clients = ctx.clients;
        this.faces = ctx.faces;
        this.fnMapper = ctx.fnMapper;
        this.ids = ctx.ids;
        this.prefixes = ctx.prefixes;
        this.varMapper = ctx.varMapper;
        this.faceletHierarchy = new ArrayList(ctx.faceletHierarchy.size() + 1);
        this.faceletHierarchy.addAll(ctx.faceletHierarchy);
        this.faceletHierarchy.add(facelet);
        this.facelet = facelet;
    }

    public DefaultFaceletContext(FacesContext faces, DefaultFacelet facelet) {
        this.ctx = ELAdaptor.getELContext(faces);
        this.ids = new HashMap();
        this.prefixes = new HashMap();
        this.clients = new ArrayList(5);
        this.faces = faces;
        this.faceletHierarchy = new ArrayList(1);
        this.faceletHierarchy.add(facelet);
        this.facelet = facelet;
        this.varMapper = this.ctx.getVariableMapper();
        if (this.varMapper == null) {
            this.varMapper = new DefaultVariableMapper();
        }
        this.fnMapper = this.ctx.getFunctionMapper();
    }

    /*
     * (non-Javadoc)
     * @see com.sun.facelets.FaceletContext#getFacesContext()
     */
    @Override
    public FacesContext getFacesContext() {
        return this.faces;
    }

    /*
     * (non-Javadoc)
     * @see com.sun.facelets.FaceletContext#getExpressionFactory()
     */
    @Override
    public ExpressionFactory getExpressionFactory() {
        return this.facelet.getExpressionFactory();
    }

    /*
     * (non-Javadoc)
     * @see
     * com.sun.facelets.FaceletContext#setVariableMapper(javax.el.VariableMapper
     * )
     */
    @Override
    public void setVariableMapper(VariableMapper varMapper) {
        // Assert.param("varMapper", varMapper);
        this.varMapper = varMapper;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.sun.facelets.FaceletContext#setFunctionMapper(javax.el.FunctionMapper
     * )
     */
    @Override
    public void setFunctionMapper(FunctionMapper fnMapper) {
        // Assert.param("fnMapper", fnMapper);
        this.fnMapper = fnMapper;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.sun.facelets.FaceletContext#includeFacelet(javax.faces.component.
     * UIComponent, java.lang.String)
     */
    @Override
    public void includeFacelet(UIComponent parent, String relativePath)
            throws IOException, FacesException, ELException {
        this.facelet.include(this, parent, relativePath);
    }

    /*
     * (non-Javadoc)
     * @see javax.el.ELContext#getFunctionMapper()
     */
    @Override
    public FunctionMapper getFunctionMapper() {
        return this.fnMapper;
    }

    /*
     * (non-Javadoc)
     * @see javax.el.ELContext#getVariableMapper()
     */
    @Override
    public VariableMapper getVariableMapper() {
        return this.varMapper;
    }

    /*
     * (non-Javadoc)
     * @see javax.el.ELContext#getContext(java.lang.Class)
     */
    @Override
    public Object getContext(Class key) {
        return this.ctx.getContext(key);
    }

    /*
     * (non-Javadoc)
     * @see javax.el.ELContext#putContext(java.lang.Class, java.lang.Object)
     */
    @Override
    public void putContext(Class key, Object contextObject) {
        this.ctx.putContext(key, contextObject);
    }

    /*
     * (non-Javadoc)
     * @see com.sun.facelets.FaceletContext#generateUniqueId(java.lang.String)
     */
    @Override
    public String generateUniqueId(String base) {

        if (prefix == null) {
            StringBuilder builder = new StringBuilder(
                    faceletHierarchy.size() * 30);
            for (int i = 0; i < faceletHierarchy.size(); i++) {
                DefaultFacelet facelet = (DefaultFacelet) faceletHierarchy.get(i);
                builder.append(facelet.getAlias());
            }
            Integer prefixInt = new Integer(builder.toString().hashCode());

            Integer cnt = (Integer) prefixes.get(prefixInt);
            if (cnt == null) {
                this.prefixes.put(prefixInt, new Integer(0));
                prefix = prefixInt.toString();
            } else {
                int i = cnt.intValue() + 1;
                this.prefixes.put(prefixInt, new Integer(i));
                prefix = prefixInt + "_" + i;
            }
        }

        Integer cnt = (Integer) this.ids.get(base);
        if (cnt == null) {
            this.ids.put(base, new Integer(0));
            uniqueIdBuilder.delete(0, uniqueIdBuilder.length());
            uniqueIdBuilder.append(prefix);
            uniqueIdBuilder.append("_");
            uniqueIdBuilder.append(base);
            return uniqueIdBuilder.toString();
        } else {
            int i = cnt.intValue() + 1;
            this.ids.put(base, new Integer(i));
            uniqueIdBuilder.delete(0, uniqueIdBuilder.length());
            uniqueIdBuilder.append(prefix);
            uniqueIdBuilder.append("_");
            uniqueIdBuilder.append(base);
            uniqueIdBuilder.append("_");
            uniqueIdBuilder.append(i);
            return uniqueIdBuilder.toString();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.sun.facelets.FaceletContext#getAttribute(java.lang.String)
     */
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
     * @see com.sun.facelets.FaceletContext#setAttribute(java.lang.String,
     * java.lang.Object)
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
     * @see
     * com.sun.facelets.FaceletContext#includeFacelet(javax.faces.component.
     * UIComponent, java.net.URL)
     */
    @Override
    public void includeFacelet(UIComponent parent, URL absolutePath)
            throws IOException, FacesException, ELException {
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
        this.clients.add(0, new TemplateManager(this.facelet, client, true));
    }

    @Override
    public void extendClient(final TemplateClient client) {
        this.clients.add(new TemplateManager(this.facelet, client, false));
    }

    @Override
    public boolean includeDefinition(UIComponent parent, String name)
            throws IOException, FaceletException, FacesException, ELException {
        boolean found = false;
        TemplateManager client;

        for (int i = 0, size = this.clients.size(); i < size && !found; i++) {
            client = ((TemplateManager) this.clients.get(i));
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

        private final boolean root;

        private final Set names = new HashSet();

        public TemplateManager(DefaultFacelet owner, TemplateClient target,
                boolean root) {
            this.owner = owner;
            this.target = target;
            this.root = root;
        }

        @Override
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

        public boolean isRoot() {
            return this.root;
        }
    }

    @Override
    public boolean isPropertyResolved() {
        return this.ctx.isPropertyResolved();
    }

    @Override
    public void setPropertyResolved(boolean resolved) {
        this.ctx.setPropertyResolved(resolved);
    }
}
