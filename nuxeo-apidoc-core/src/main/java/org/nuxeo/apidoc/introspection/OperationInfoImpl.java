/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.introspection;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;

/**
 * DTO for an {@link OperationInfo}, used for the runtime implementation.
 */
public class OperationInfoImpl extends BaseNuxeoArtifact implements
        OperationInfo {

    public final OperationDocumentation op;

    public final String version;

    protected final String operationClass;

    protected final String contributingComponent;

    public OperationInfoImpl(OperationDocumentation op, String version, String operationClass, String contributingComponent) {
        this.op = op;
        this.version = version;
        this.operationClass=operationClass;
        if (contributingComponent==null || contributingComponent.isEmpty()) {
            this.contributingComponent = OperationInfo.BUILT_IN;
        } else {
            String[] parts = contributingComponent.split(":");
            if (parts.length>1) {
                this.contributingComponent=parts[1];
            } else {
                this.contributingComponent=contributingComponent;
            }
        }
    }

    @Override
    public String getName() {
        return op.getId();
    }

    @Override
    public String getId() {
        return ARTIFACT_PREFIX + op.getId();
    }

    @Override
    public String getDescription() {
        return op.getDescription();
    }

    @Override
    public String[] getSignature() {
        return op.getSignature();
    }

    @Override
    public String getCategory() {
        return op.getCategory();
    }

    @Override
    public String getUrl() {
        return op.getUrl();
    }

    @Override
    public String getLabel() {
        return op.getLabel();
    }

    @Override
    public String getRequires() {
        return op.getRequires();
    }

    @Override
    public String getSince() {
        return op.since;
    }

    @Override
    public List<Param> getParams() {
        return Arrays.asList(op.getParams());
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return "/";
    }

    @Override
    public int compareTo(OperationInfo o) {
        String s1 = getLabel() == null ? getId() : getLabel();
        String s2 = o.getLabel() == null ? o.getId() : o.getLabel();
        return s1.compareTo(s2);
    }

    public String getOperationClass() {
        return operationClass;
    }

    public String getContributingComponent() {
        return contributingComponent;
    }

}
