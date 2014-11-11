/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DefaultActionFilter.java 30476 2008-02-22 09:13:23Z bstefanescu $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.elcache.CachedJEXLManager;
import org.nuxeo.runtime.expression.Context;
import org.nuxeo.runtime.expression.JexlExpression;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("filter")
public class DefaultActionFilter implements ActionFilter {

    private static final long serialVersionUID = 8885038533939001747L;

    private static final Log log = LogFactory.getLog(DefaultActionFilter.class);

    @XNode("@id")
    private String id;

    @XNode("@append")
    private boolean append;

    @XNodeList(value = "rule", type = String[].class, componentType = FilterRule.class)
    private FilterRule[] rules;

    public DefaultActionFilter() {
        this(null, null, false);
    }

    public DefaultActionFilter(String id, FilterRule[] rules) {
        this(id, rules, false);
    }

    public DefaultActionFilter(String id, FilterRule[] rules, boolean append) {
        this.id = id;
        this.rules = rules;
        this.append = append;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FilterRule[] getRules() {
        return rules;
    }

    public void setRules(FilterRule[] rules) {
        this.rules = rules;
    }

    /**
     * Checks whether this action is valid in the given context.
     * <p>
     * The action is considered valid if no denying rule is found and at least
     * one granting rule is found. If no rule is found at all, it is valid.
     * <p>
     * In other words: OR between granting rules, AND between denying rules,
     * denial is favored (also if exceptions occur), AND inside of rules, OR
     * inside or rule items (type, facet,...).
     *
     * @param action
     * @return
     */
    public boolean accept(Action action, ActionContext context) {
        // no context: reject
        if (context == null) {
            return false;
        }
        // no rule: accept
        if (rules == null || rules.length == 0) {
            return true;
        }
        boolean existsGrantRule = false;
        boolean grantApply = false;
        for (FilterRule rule : rules) {
            boolean ruleApplies = checkRule(rule, action, context);
            if (!rule.grant) {
                if (ruleApplies) {
                    return false;
                }
            } else {
                existsGrantRule = true;
                if (ruleApplies) {
                    grantApply = true;
                }
            }
        }
        if (existsGrantRule) {
            return grantApply;
        }
        // there is no allow rule, and none of the deny rules applies
        return true;
    }

    protected static final String PRECOMPUTED_KEY = "PrecomputedFilters";

    /**
     * Returns true if all conditions defined in the rule are true.
     */
    @SuppressWarnings("unchecked")
    protected final boolean checkRule(FilterRule rule, Action action,
            ActionContext context) {
        // check cache
        Map<FilterRule, Boolean> precomputed = (Map<FilterRule, Boolean>) context.get(PRECOMPUTED_KEY);
        if (precomputed == null) {
            precomputed = new HashMap<FilterRule, Boolean>();
            context.put(PRECOMPUTED_KEY, precomputed);
        } else if (precomputed.containsKey(rule)) {
            return precomputed.get(rule).booleanValue();
        }
        // compute filter result
        boolean result = (rule.facets == null || rule.facets.length == 0 || checkFacets(
                action, context, rule.facets))
                && (rule.types == null || rule.types.length == 0 || checkTypes(
                        action, context, rule.types))
                && (rule.schemas == null || rule.schemas.length == 0 || checkSchemas(
                        action, context, rule.schemas))
                && (rule.permissions == null || rule.permissions.length == 0 || checkPermissions(
                        action, context, rule.permissions))
                && (rule.groups == null || rule.groups.length == 0 || checkGroups(
                        action, context, rule.groups))
                && (rule.conditions == null || rule.conditions.length == 0 || checkConditions(
                        action, context, rule.conditions));
        // put in cache
        precomputed.put(rule, Boolean.valueOf(result));
        return result;
    }

    /**
     * Returns true if document has one of the given facets, else false.
     *
     * @return true if document has one of the given facets, else false.
     */
    protected final boolean checkFacets(Action action, ActionContext context,
            String[] facets) {
        DocumentModel doc = context.getCurrentDocument();
        if (doc == null) {
            return false;
        }
        for (String facet : facets) {
            if (doc.hasFacet(facet)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if given document has one of the permissions, else false.
     * <p>
     * If no document is found, return true only if principal is a manager.
     *
     * @return true if given document has one of the given permissions, else
     *         false
     */
    protected final boolean checkPermissions(Action action,
            ActionContext context, String[] permissions) {
        DocumentModel doc = context.getCurrentDocument();
        if (doc == null) {
            NuxeoPrincipal principal = context.getCurrentPrincipal();
            // default check when there is not context yet
            if (principal != null) {
                if (principal.isAdministrator()) {
                    return true;
                }
            }
            return false;
        }
        // check rights on doc
        CoreSession docMgr = context.getDocumentManager();
        if (docMgr == null) {
            return false;
        }
        for (String permission : permissions) {
            try {
                if (docMgr.hasPermission(doc.getRef(), permission)) {
                    return true;
                }
            } catch (Exception e) {
                log.error(e, e);
            }
        }
        return false;
    }

    protected final boolean checkGroups(Action action, ActionContext context,
            String[] groups) {
        NuxeoPrincipal principal = context.getCurrentPrincipal();
        if (principal == null) {
            return false;
        }
        List<String> principalGroups = principal.getGroups();
        for (String group : groups) {
            if (principalGroups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if one of the conditions is verified, else false.
     * <p>
     * If one evaluation fails, return false.
     *
     * @return true if one of the conditions is verified, else false.
     */
    protected final boolean checkConditions(Action action,
            ActionContext context, String[] conditions) {
        DocumentModel doc = context.getCurrentDocument();
        NuxeoPrincipal currentPrincipal = context.getCurrentPrincipal();

        for (String condition : conditions) {
            try {
                JexlExpression exp = CachedJEXLManager.getExpression(condition);
                Context ctx = new Context();
                ctx.put("document", doc);
                ctx.put("principal", currentPrincipal);
                // get custom context from ActionContext
                for (String k : context.keySet()) {
                    ctx.put(k, context.get(k));
                }
                ctx.put("SeamContext", context.get("SeamContext"));

                Object eval = exp.eval(ctx);
                if (eval == null) {
                    log.error("evaluation of condition " + condition
                            + " failed: returning false");
                }
                return Boolean.TRUE.equals(eval);
            } catch (Exception e) {
                log.error("evaluation of condition " + condition
                        + " failed: returning false", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Returns true if document type is one of the given types, else false.
     * <p>
     * If document is null, consider context is the server and return true if
     * 'Server' is in the list.
     *
     * @return true if document type is one of the given types, else false.
     */
    protected final boolean checkTypes(Action action, ActionContext context,
            String[] types) {
        DocumentModel doc = context.getCurrentDocument();
        String docType;
        if (doc == null) {
            // consider we're on the Server root
            docType = "Root";
        } else {
            docType = doc.getType();
        }

        for (String type : types) {
            if (type.equals(docType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if document has one of the given schemas, else false.
     *
     * @return true if document has one of the given schemas, else false
     */
    protected final boolean checkSchemas(Action action, ActionContext context,
            String[] schemas) {
        DocumentModel doc = context.getCurrentDocument();
        if (doc == null) {
            return false;
        }
        for (String schema : schemas) {
            if (doc.hasSchema(schema)) {
                return true;
            }
        }
        return false;
    }

    public boolean getAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

}
