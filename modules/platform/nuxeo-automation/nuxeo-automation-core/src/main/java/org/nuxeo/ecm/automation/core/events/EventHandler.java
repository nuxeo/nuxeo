/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mvel2.CompileException;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;

/**
 * Descriptor for event handlers.
 */
@XObject("handler")
@XRegistry(merge = false, remove = false, enable = false)
public class EventHandler {

    private static final Log log = LogFactory.getLog(EventHandler.class);

    @XNode("@chainId")
    protected String chainId;

    @XNode("@postCommit")
    protected boolean isPostCommit;

    @XNodeList(value = "event", type = HashSet.class, componentType = String.class)
    protected Set<String> events;

    @XNodeList(value = "filters/doctype", type = HashSet.class, componentType = String.class, nullByDefault = true)
    protected Set<String> doctypes;

    @XNode("filters/facet")
    protected String facet;

    @XNode("filters/lifeCycle")
    protected void setLifeCycleExpr(String lifeCycles) {
        lifeCycle = StringUtils.split(lifeCycles, ',', true);
    }

    protected String[] lifeCycle;

    @XNode("filters/pathStartsWith")
    protected String pathStartsWith;

    protected Filter attribute;

    @XNode("filters/attribute")
    public void setAttribute(String attribute) {
        this.attribute = DocumentAttributeFilterFactory.getFilter(attribute);
    }

    /**
     * the principal should be member of at least one of the groups. OR is used
     */
    @XNodeList(value = "filters/group", type = ArrayList.class, componentType = String.class)
    protected List<String> memberOf;

    @XNode("filters/isAdministrator")
    protected Boolean isAdministrator;

    /**
     * @since 5.7: added to replace the 'expression' element as its evaluation is inverted
     */
    protected String condition;

    @XNode("filters/condition")
    protected void _setCondition(String expr) {
        condition = convertExpr(expr);
    }

    protected String convertExpr(String expr) {
        String res = expr.replace("&lt;", "<");
        res = res.replace("&gt;", ">");
        res = res.replace("&amp;", "&");
        return res;
    }

    public Set<String> getEvents() {
        return events;
    }

    public String getChainId() {
        return chainId;
    }

    public boolean isPostCommit() {
        return isPostCommit;
    }

    /**
     * Condition to define on event handler
     *
     * @since 5.7
     */
    public String getCondition() {
        return condition;
    }

    public String getFacet() {
        return facet;
    }

    public Filter getAttribute() {
        return attribute;
    }

    public String[] getLifeCycle() {
        return lifeCycle;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    public Boolean getIsAdministrator() {
        return isAdministrator;
    }

    public String getPathStartsWith() {
        return pathStartsWith;
    }

    public Set<String> getDoctypes() {
        return doctypes;
    }

    /**
     * Checks if this handler should run for the event and operation context.
     *
     * @param quick If {@code true}, then this method may not check all filter parameters like {@code filter/expression}
     *            and just return {@code true} to avoid costly evaluations on {@link ShallowDocumentModel} instances
     */
    public boolean isEnabled(OperationContext ctx, EventContext eventCtx, boolean quick) {
        Object obj = ctx.getInput();
        DocumentModel doc = null;
        if (obj instanceof DocumentModel) {
            doc = (DocumentModel) obj;
        }
        if (doctypes != null) {
            if (doc == null || (!doctypes.isEmpty() && !doctypes.contains(doc.getType()))) {
                return false;
            }
        }
        if (facet != null) {
            if (doc == null || !doc.hasFacet(facet)) {
                return false;
            }
        }
        if (lifeCycle != null && lifeCycle.length > 0) {
            if (doc == null) {
                return false;
            }
            boolean match = false;
            String currentLc = doc.getCurrentLifeCycleState();
            for (String lc : lifeCycle) {
                if (lc.equals(currentLc)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        if (attribute != null) {
            if (doc == null || !attribute.accept(doc)) {
                return false;
            }
        }
        if (pathStartsWith != null) {
            if (doc == null || !doc.getPathAsString().startsWith(pathStartsWith)) {
                return false;
            }
        }
        if (memberOf != null && !memberOf.isEmpty()) {
            NuxeoPrincipal p = eventCtx.getPrincipal();
            boolean granted = false;
            for (String group : memberOf) {
                if (p.isMemberOf(group)) {
                    granted = true;
                    break;
                }
            }
            if (!granted) {
                return false;
            }
        }
        if (isAdministrator != null) {
            if (!eventCtx.getPrincipal().isAdministrator()) {
                return false;
            }
        }
        if (quick) {
            return true;
        }
        /*
         * The following are not evaluated in quick mode, as we need a full DocumentModelImpl to evaluate most
         * expressions.
         */
        if (!org.apache.commons.lang3.StringUtils.isBlank(condition)) {
            Expression expr = Scripting.newExpression(condition);
            try {
                if (!Boolean.TRUE.equals(expr.eval(ctx))) {
                    return false;
                }
            } catch (CompileException e) {
                // happens for expressions evaluated over a DeletedDocumentModel for instance
                log.debug("Failed to execute expression: " + e, e);
                return false;
            }
        }
        return true;
    }
}
