/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("handler")
public class EventHandler {

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
    protected String lifeCycle;

    @XNode("filters/pathStartsWith")
    protected String pathStartsWith;

    protected Filter attribute;

    @XNode("filters/attribute")
    public void setAttribute(String attribute) {
        this.attribute = DocumentAttributeFilterFactory.getFilter(attribute);
    }

    /** the principal should be member of at least one of the groups. OR is used */
    @XNodeList(value = "filters/group", type = ArrayList.class, componentType = String.class)
    protected List<String> memberOf;

    @XNode("filters/isAdministrator")
    protected Boolean isAdministrator;

    protected String expression;

    @XNode("filters/expression")
    protected void _setExpression(String expr) {
        expr = expr.replaceAll("&lt;", "<");
        expression = expr.replaceAll("&gt;", ">");
    }

    private Expression expr;

    public EventHandler() {
    }

    public EventHandler(String eventId, String chainId) {
        this(Collections.singleton(eventId), chainId);
    }

    public EventHandler(Set<String> eventId, String chainId) {
        this.events = eventId;
        this.chainId = chainId;
    }

    public Set<String> getEvents() {
        return events;
    }

    public String getChainId() {
        return chainId;
    }

    public void setPostCommit(boolean isPostCommit) {
        this.isPostCommit = isPostCommit;
    }

    public boolean isPostCommit() {
        return isPostCommit;
    }

    public void setAttributeFilter(Filter attribute) {
        this.attribute = attribute;
    }

    public void setIsAdministrator(Boolean isAdministrator) {
        this.isAdministrator = isAdministrator;
    }

    public void setMemberOf(List<String> groups) {
        this.memberOf = groups;
    }

    public void setPathStartsWith(String pathStartsWith) {
        this.pathStartsWith = pathStartsWith;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setDoctypes(Set<String> doctypes) {
        this.doctypes = doctypes;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public void setLifeCycle(String lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getExpression() {
        return expression;
    }

    public String getFacet() {
        return facet;
    }

    public Filter getAttribute() {
        return attribute;
    }

    public String getLifeCycle() {
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

    public Expression getExpr() {
        return expr;
    }

    public boolean isEnabled(OperationContext ctx, EventContext eventCtx)
            throws Exception {
        Object obj = ctx.getInput();
        DocumentModel doc = null;
        if (obj instanceof DocumentModel) {
            doc = (DocumentModel) obj;
        }
        if (doctypes != null) {
            if (doc == null
                    || (!doctypes.isEmpty() && !doctypes.contains(doc.getType()))) {
                return false;
            }
        }
        if (facet != null) {
            if (doc == null || !doc.hasFacet(facet)) {
                return false;
            }
        }
        if (lifeCycle != null) {
            if (doc == null
                    || !lifeCycle.equals(doc.getCurrentLifeCycleState())) {
                return false;
            }
        }
        if (attribute != null) {
            if (doc == null || !attribute.accept(doc)) {
                return false;
            }
        }
        if (pathStartsWith != null) {
            if (doc == null
                    || !doc.getPathAsString().startsWith(pathStartsWith)) {
                return false;
            }
        }
        if (memberOf != null && !memberOf.isEmpty()) {
            NuxeoPrincipal p = (NuxeoPrincipal) eventCtx.getPrincipal();
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
            if (!((NuxeoPrincipal) eventCtx.getPrincipal()).isAdministrator()) {
                return false;
            }
        }
        if (expression != null) {
            if (expr == null) {
                expr = Scripting.newExpression(expression);
            }
            if ((Boolean) expr.eval(ctx)) {
                return false;
            }
        }
        return true;
    }

}
