/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.binding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ValueExpressionLiteral;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;

/**
 * Alternative to {@link AliasVariableMapper} optimized behavior.
 * <p>
 * Keeps variables in the current context, but without aliasing variables for efficiency. Compared to the standard
 * variable mappers, adds blocking features, given patterns, to allow compartmenting variables contexts (inside layout
 * widget trees for instance).
 *
 * @since 8.2
 */
public class BlockingVariableMapper extends VariableMapper {

    private static final Log log = LogFactory.getLog(BlockingVariableMapper.class);

    protected final VariableMapper orig;

    protected Map<String, ValueExpression> vars;

    protected List<String> blockedPatterns;

    public BlockingVariableMapper(VariableMapper orig) {
        super();
        this.orig = orig;
    }

    @Override
    public ValueExpression resolveVariable(String variable) {
        ValueExpression ve = null;
        try {
            if (hasVariable(variable)) {
                ve = (ValueExpression) vars.get(variable);
            } else {
                // resolve to a value expression resolving to null if variable
                // is supposed to be blocked
                if (variable != null && blockedPatterns != null) {
                    for (String blockedPattern : blockedPatterns) {
                        if (StringUtils.isBlank(blockedPattern)) {
                            continue;
                        }
                        boolean doBlock = false;
                        if (blockedPattern.endsWith("*")) {
                            String pattern = blockedPattern.substring(0, blockedPattern.length() - 1);
                            if (variable.startsWith(pattern)) {
                                doBlock = true;
                            }
                        } else if (blockedPattern.equals(variable)) {
                            doBlock = true;
                        }
                        if (doBlock) {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Blocked expression var='%s'", variable));
                            }
                            return getNullValueExpression();
                        }
                    }
                }
                return orig.resolveVariable(variable);
            }
            return ve;
        } catch (StackOverflowError e) {
            throw new ELException("Could not Resolve Variable [Overflow]: " + variable, e);
        }
    }

    @Override
    public ValueExpression setVariable(String variable, ValueExpression expression) {
        if (vars == null) {
            vars = new LinkedHashMap<String, ValueExpression>();
        }
        return vars.put(variable, expression);
    }

    public boolean hasVariable(String variable) {
        return vars != null && vars.containsKey(variable);
    }

    protected ValueExpression getNullValueExpression() {
        return new ValueExpressionLiteral(null, Object.class);
    }

    public Map<String, ValueExpression> getVariables() {
        return vars;
    }

    public List<String> getBlockedPatterns() {
        return blockedPatterns;
    }

    public void setBlockedPatterns(List<String> blockedPatterns) {
        if (blockedPatterns != null) {
            this.blockedPatterns = new ArrayList<String>();
            this.blockedPatterns.addAll(blockedPatterns);
        } else {
            this.blockedPatterns = null;
        }
    }

    public void addBlockedPattern(String blockedPattern) {
        if (StringUtils.isBlank(blockedPattern)) {
            return;
        }
        if (this.blockedPatterns == null) {
            this.blockedPatterns = new ArrayList<String>();
        }
        this.blockedPatterns.add(blockedPattern);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(getClass().getSimpleName());
        buf.append(" {");
        buf.append("vars=");
        buf.append(vars);
        buf.append('}');

        return buf.toString();
    }

}