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
 * $Id: FilterRule.java 30476 2008-02-22 09:13:23Z bstefanescu $
 */

package org.nuxeo.ecm.platform.actions;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
@XObject("rule")
public class FilterRule {

    // These instance variables are package-private because there are no accessors (for now?).

    @XNode(value = "@grant")
    boolean grant = false; // DENY

    @XNodeList(value = "permission", type = String[].class, componentType = String.class)
    String[] permissions;

    @XNodeList(value = "facet", type = String[].class, componentType = String.class)
    String[] facets;

    @XNodeList(value = "type", type = String[].class, componentType = String.class)
    String[] types;

    @XNodeList(value = "schema", type = String[].class, componentType = String.class)
    String[] schemas;

    @XNodeList(value = "group", type = String[].class, componentType = String.class)
    String[] groups;

    String[] conditions;

    protected String cacheKey;

    public FilterRule() {
    }

    public FilterRule(boolean grant, String[] permissions, String[] facets,
            String[] conditions, String[] types, String[] schemas) {
        this.grant = grant;
        this.permissions = permissions;
        this.facets = facets;
        this.conditions = conditions;
        this.types = types;
        this.schemas = schemas;
    }

    @XNodeList(value = "condition", type = String[].class, componentType = String.class)
    public void setConditions(String[] conditions) {
        // add some preprocessing
        preprocessConditions(conditions);
        this.conditions = conditions;
    }

    /**
     * Preprocess conditions to add the necessary EL to have variables resolved via SeamContext.
     */
    private static void preprocessConditions(String[] conditions) {
        for (int i = 0; i < conditions.length; i++) {
            String condition = conditions[i];
            condition = condition.trim();
            if ((condition.startsWith("${") || condition.startsWith("#{"))
                    && condition.endsWith("}")) {
                String parsedCondition = condition.substring(2, condition
                        .length() - 1);
                int idx = parsedCondition.indexOf('.');
                if (idx == -1) {
                    // simple context variable lookup (may be Factory call)
                    conditions[i] = "SeamContext.get(\"" + parsedCondition
                            + "\")";
                } else {
                    // Seam component call
                    String seamComponentName = parsedCondition
                            .substring(0, idx);
                    String resolutionAccessor = "SeamContext.get(\""
                            + seamComponentName + "\")";
                    conditions[i] = resolutionAccessor
                            + parsedCondition.substring(idx);
                }
            }
        }
    }

    public String getCacheKey() {
        if (cacheKey==null) {
            StringBuffer sb = new StringBuffer();
            sb.append("grant:");
            sb.append(grant);
            if (permissions!=null && permissions.length>0) {
                sb.append(":permissions:");
                for (String perm : permissions){
                    sb.append(perm);
                    sb.append(",");
                }
            }
            if (facets!=null && facets.length>0 ) {
                sb.append(":facets:");
                for (String facet : facets){
                    sb.append(facet);
                    sb.append(",");
                }
            }
            if (conditions!=null && conditions.length>0) {
                sb.append(":conditions:");
                for (String cond : conditions){
                    sb.append(cond);
                    sb.append(",");
                }
            }
            if (types!=null && types.length>0) {
                sb.append(":types:");
                for (String typ : types){
                    sb.append(typ);
                    sb.append(",");
                }
            }
            if (schemas!=null && schemas.length>0) {
                sb.append(":schemas:");
                for (String schem : schemas){
                    sb.append(schem);
                    sb.append(",");
                }
            }

            if (groups != null && groups.length > 0) {
                sb.append(":groups:");
                for (String group : groups) {
                    sb.append(group);
                    sb.append(",");
                }
            }
            cacheKey = sb.toString();
        }
        return cacheKey;
    }

    @Override
    public String toString() {
        return getCacheKey();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FilterRule)) {
            return false;
        }
        return getCacheKey().equals(((FilterRule) obj).getCacheKey());
    }

    @Override
    public int hashCode() {
        return getCacheKey().hashCode();
    }

}
