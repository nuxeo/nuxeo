/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Variable mapper that holds value expressions. It can be exposed to the request context after the JSF component tree
 * build, so that {@link AliasValueExpression} instances can be resolved.
 * <p>
 * It also builds the {@link VariableMapper} to use when building the component tree, so that
 * {@link AliasValueExpression} instances are kept in component attributes, even on ajax rerender.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class AliasVariableMapper extends VariableMapper {

    private static final Log log = LogFactory.getLog(AliasVariableMapper.class);

    public static final String REQUEST_MARKER = AliasVariableMapper.class.getName() + "_MARKER";

    protected String id;

    protected Map<String, ValueExpression> vars;

    protected List<String> blockedPatterns;

    public AliasVariableMapper() {
        super();
    }

    public AliasVariableMapper(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ValueExpression resolveVariable(String variable) {
        ValueExpression ve = null;
        if (vars != null) {
            ve = vars.get(variable);
        }
        return ve;
    }

    @Override
    public ValueExpression setVariable(String variable, ValueExpression expression) {
        if (vars == null) {
            vars = new LinkedHashMap<>();
        }
        return vars.put(variable, expression);
    }

    public boolean hasVariables(String variable) {
        return vars != null && vars.containsKey(variable);
    }

    public VariableMapper getVariableMapperForBuild(VariableMapper orig) {
        AliasVariableMapperWrapper vm = new AliasVariableMapperWrapper(orig, getBlockedPatterns());
        Map<String, ValueExpression> vars = getVariables();
        if (vars != null) {
            String id = getId();
            for (Map.Entry<String, ValueExpression> var : vars.entrySet()) {
                vm.setVariable(var.getKey(), new AliasValueExpression(id, var.getKey()));
            }
        }
        return vm;
    }

    public Map<String, ValueExpression> getVariables() {
        return vars;
    }

    public List<String> getBlockedPatterns() {
        return blockedPatterns;
    }

    public void setBlockedPatterns(List<String> blockedPatterns) {
        this.blockedPatterns = blockedPatterns;
    }

    public static AliasVariableMapper getVariableMapper(FacesContext ctx, String id) {
        NuxeoAliasBean a = lookupBean(ctx);
        if (a != null) {
            return a.get(id);
        }
        return null;
    }

    public static void exposeAliasesToRequest(FacesContext ctx, AliasVariableMapper vm) {
        NuxeoAliasBean a = lookupBean(ctx);
        if (a != null) {
            a.add(vm);
        }
    }

    public static void removeAliasesExposedToRequest(FacesContext ctx, String id) {
        // do not remove aliases from bean anymore: bean is kept in request
        // scope and will be emptied at the end of the request
        // NuxeoAliasBean a = lookupBean(ctx);
        // if (a != null) {
        // a.remove(id);
        // }
        return;
    }

    protected static NuxeoAliasBean lookupBean(FacesContext ctx) {
        String expr = "#{" + NuxeoAliasBean.NAME + "}";
        NuxeoAliasBean bean = (NuxeoAliasBean) ctx.getApplication().evaluateExpressionGet(ctx, expr, Object.class);
        if (bean == null) {
            log.error("Managed bean not found: " + expr);
            return null;
        }
        return bean;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(getClass().getSimpleName());
        buf.append(" {");
        buf.append(" id=");
        buf.append(id);
        buf.append(", vars=");
        buf.append(vars);
        buf.append('}');

        return buf.toString();
    }

}
