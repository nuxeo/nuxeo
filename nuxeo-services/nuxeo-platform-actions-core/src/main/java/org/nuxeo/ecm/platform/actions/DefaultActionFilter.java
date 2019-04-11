/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DefaultActionFilter.java 30476 2008-02-22 09:13:23Z bstefanescu $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.HashMap;
import java.util.Map;

import javax.el.ELException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("filter")
public class DefaultActionFilter implements ActionFilter, Cloneable {

    private static final long serialVersionUID = 8885038533939001747L;

    private static final Log log = LogFactory.getLog(DefaultActionFilter.class);

    @XNode("@id")
    protected String id;

    @XNode("@append")
    protected boolean append;

    @XNodeList(value = "rule", type = String[].class, componentType = FilterRule.class)
    protected FilterRule[] rules;

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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public FilterRule[] getRules() {
        return rules;
    }

    public void setRules(FilterRule[] rules) {
        this.rules = rules;
    }

    // FIXME: the parameter 'action' is not used!
    @Override
    public boolean accept(Action action, ActionContext context) {
        if (log.isDebugEnabled()) {
            if (action == null) {
                log.debug(String.format("#accept: checking filter '%s'", getId()));
            } else {
                log.debug(String.format("#accept: checking filter '%s' for action '%s'", getId(), action.getId()));
            }
        }
        // no context: reject
        if (context == null) {
            if (log.isDebugEnabled()) {
                log.debug("#accept: no context available: action filtered");
            }
            return false;
        }
        // no rule: accept
        if (rules == null || rules.length == 0) {
            return true;
        }
        boolean existsGrantRule = false;
        boolean grantApply = false;
        for (FilterRule rule : rules) {
            boolean ruleApplies = checkRule(rule, context);
            if (!rule.grant) {
                if (ruleApplies) {
                    if (log.isDebugEnabled()) {
                        log.debug("#accept: denying rule applies => action filtered");
                    }
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
            if (log.isDebugEnabled()) {
                if (grantApply) {
                    log.debug("#accept: granting rule applies, action not filtered");
                } else {
                    log.debug("#accept: granting rule applies, action filtered");
                }
            }
            return grantApply;
        }
        // there is no allow rule, and none of the deny rules applies
        return true;
    }

    public static final String PRECOMPUTED_KEY = "PrecomputedFilters";

    /**
     * Returns true if all conditions defined in the rule are true.
     * <p>
     * Since 5.7.3, does not put computed value in context in a cache if the action context does not allow it.
     *
     * @see ActionContext#disableGlobalCaching()
     */
    @SuppressWarnings("unchecked")
    protected final boolean checkRule(FilterRule rule, ActionContext context) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("#checkRule: checking rule '%s'", rule));
        }
        boolean disableCache = context.disableGlobalCaching();
        if (!disableCache) {
            // check cache
            Map<FilterRule, Boolean> precomputed = (Map<FilterRule, Boolean>) context.getLocalVariable(PRECOMPUTED_KEY);
            if (precomputed != null && precomputed.containsKey(rule)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("#checkRule: return precomputed result for rule '%s'", rule));
                }
                return Boolean.TRUE.equals(precomputed.get(rule));
            }
        }
        // compute filter result
        boolean result = (rule.facets == null || rule.facets.length == 0 || checkFacets(context, rule.facets))
                && (rule.types == null || rule.types.length == 0 || checkTypes(context, rule.types))
                && (rule.schemas == null || rule.schemas.length == 0 || checkSchemas(context, rule.schemas))
                && (rule.permissions == null || rule.permissions.length == 0 || checkPermissions(context,
                        rule.permissions))
                && (rule.groups == null || rule.groups.length == 0 || checkGroups(context, rule.groups))
                && (rule.conditions == null || rule.conditions.length == 0 || checkConditions(context, rule.conditions));
        if (!disableCache) {
            // put in cache
            Map<FilterRule, Boolean> precomputed = (Map<FilterRule, Boolean>) context.getLocalVariable(PRECOMPUTED_KEY);
            if (precomputed == null) {
                precomputed = new HashMap<>();
                context.putLocalVariable(PRECOMPUTED_KEY, precomputed);
            }
            precomputed.put(rule, Boolean.valueOf(result));
        }
        return result;
    }

    /**
     * Returns true if document has one of the given facets, else false.
     *
     * @return true if document has one of the given facets, else false.
     */
    protected final boolean checkFacets(ActionContext context, String[] facets) {
        DocumentModel doc = context.getCurrentDocument();
        if (doc == null) {
            return false;
        }
        for (String facet : facets) {
            if (doc.hasFacet(facet)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("#checkFacets: return true for facet '%s'", facet));
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("#checkFacets: return false");
        }
        return false;
    }

    /**
     * Returns true if given document has one of the permissions, else false.
     * <p>
     * If no document is found, return true only if principal is a manager.
     *
     * @return true if given document has one of the given permissions, else false
     */
    protected final boolean checkPermissions(ActionContext context, String[] permissions) {
        DocumentModel doc = context.getCurrentDocument();
        if (doc == null) {
            NuxeoPrincipal principal = context.getCurrentPrincipal();
            // default check when there is not context yet
            if (principal != null) {
                if (principal.isAdministrator()) {
                    if (log.isDebugEnabled()) {
                        log.debug("#checkPermissions: doc is null but user is admin => return true");
                    }
                    return true;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("#checkPermissions: doc and user are null => return false");
            }
            return false;
        }
        // check rights on doc
        CoreSession docMgr = context.getDocumentManager();
        if (docMgr == null) {
            if (log.isDebugEnabled()) {
                log.debug("#checkPermissions: no core session => return false");
            }
            return false;
        }
        for (String permission : permissions) {
            if (docMgr.hasPermission(doc.getRef(), permission)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("#checkPermissions: return true for permission '%s'", permission));
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("#checkPermissions: return false");
        }
        return false;
    }

    protected final boolean checkGroups(ActionContext context, String[] groups) {
        NuxeoPrincipal principal = context.getCurrentPrincipal();
        if (principal == null) {
            if (log.isDebugEnabled()) {
                log.debug("#checkGroups: no user => return false");
            }
            return false;
        }
        for (String group : groups) {
            if (principal.isMemberOf(group)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("#checkGroups: return true for group '%s'", group));
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("#checkGroups: return false");
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
    protected final boolean checkConditions(ActionContext context, String[] conditions) {
        for (String condition : conditions) {
            try {
                if (context.checkCondition(condition)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("#checkCondition: return true for condition '%s'", condition));
                    }
                    return true;
                }
            } catch (ELException e) {
                log.error("evaluation of condition " + condition + " failed: returning false", e);
                return false;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("#checkConditions: return false");
        }
        return false;
    }

    /**
     * Returns true if document type is one of the given types, else false.
     * <p>
     * If document is null, consider context is the server and return true if 'Server' is in the list.
     *
     * @return true if document type is one of the given types, else false.
     */
    protected final boolean checkTypes(ActionContext context, String[] types) {
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
                if (log.isDebugEnabled()) {
                    log.debug(String.format("#checkTypes: return true for type '%s'", docType));
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("#checkTypes: return false");
        }
        return false;
    }

    /**
     * Returns true if document has one of the given schemas, else false.
     *
     * @return true if document has one of the given schemas, else false
     */
    protected final boolean checkSchemas(ActionContext context, String[] schemas) {
        DocumentModel doc = context.getCurrentDocument();
        if (doc == null) {
            if (log.isDebugEnabled()) {
                log.debug("#checkSchemas: no doc => return false");
            }
            return false;
        }
        for (String schema : schemas) {
            if (doc.hasSchema(schema)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("#checkSchemas: return true for schema '%s'", schema));
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("#checkSchemas: return false");
        }
        return false;
    }

    public boolean getAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    @Override
    public DefaultActionFilter clone() {
        DefaultActionFilter clone = new DefaultActionFilter();
        clone.id = id;
        clone.append = append;
        if (rules != null) {
            clone.rules = new FilterRule[rules.length];
            for (int i = 0; i < rules.length; i++) {
                clone.rules[i] = rules[i].clone();
            }
        }
        return clone;
    }

    /**
     * Equals method added to handle hot reload of inner filters, see NXP-9677
     *
     * @since 5.6
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultActionFilter)) {
            return false;
        }
        final DefaultActionFilter o = (DefaultActionFilter) obj;
        String objId = o.getId();
        if (objId == null && !(this.id == null)) {
            return false;
        }
        if (this.id == null && !(objId == null)) {
            return false;
        }
        if (objId != null && !objId.equals(this.id)) {
            return false;
        }
        boolean append = o.getAppend();
        if (!append == this.append) {
            return false;
        }
        FilterRule[] objRules = o.getRules();
        if (objRules == null && !(this.rules == null)) {
            return false;
        }
        if (this.rules == null && !(objRules == null)) {
            return false;
        }
        if (objRules != null) {
            if (objRules.length != this.rules.length) {
                return false;
            }
            for (int i = 0; i < objRules.length; i++) {
                if (objRules[i] == null && (!(this.rules[i] == null))) {
                    return false;
                }
                if (this.rules[i] == null && (!(objRules[i] == null))) {
                    return false;
                }
                if (!objRules[i].equals(this.rules[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
