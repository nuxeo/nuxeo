/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ValueExpressionLiteral;

/**
 * @since 5.6
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class AliasVariableMapperWrapper extends VariableMapper {

    private static final Log log = LogFactory.getLog(AliasVariableMapperWrapper.class);

    protected final VariableMapper orig;

    protected final List<String> blockedPatterns;

    protected Map vars;

    public AliasVariableMapperWrapper(VariableMapper orig, List<String> blockedPatterns) {
        super();
        this.orig = orig;
        this.blockedPatterns = blockedPatterns;
    }

    /**
     * First tries to resolve against the inner Map, then the wrapped ValueExpression, unless target is an
     * {@link AliasVariableMapper} that blocks this variable pattern.
     */
    @Override
    public ValueExpression resolveVariable(String variable) {
        ValueExpression ve = null;
        try {
            if (vars != null) {
                ve = (ValueExpression) vars.get(variable);
            }
            if (ve == null) {
                // resolve to a value expression resolving to null if variable
                // is supposed to be blocked
                if (variable != null && blockedPatterns != null) {
                    for (String blockedPattern : blockedPatterns) {
                        if (blockedPattern == null) {
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
                                log.debug("Blocked expression var='" + variable + "'");
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

    protected ValueExpression getNullValueExpression() {
        return new ValueExpressionLiteral(null, Object.class);
    }

    @Override
    public ValueExpression setVariable(String variable, ValueExpression expression) {
        if (vars == null) {
            vars = new HashMap();
        }
        return (ValueExpression) vars.put(variable, expression);
    }

}
