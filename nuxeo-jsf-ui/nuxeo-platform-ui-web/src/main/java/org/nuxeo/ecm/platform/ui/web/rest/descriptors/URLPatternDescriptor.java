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
 * $Id: URLPatternDescriptor.java 22097 2007-07-06 12:38:20Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.rest.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "urlPattern")
public class URLPatternDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("defaultURLPolicy")
    private boolean defaultURLPolicy = false;

    @XNode("needBaseURL")
    private boolean needBaseURL = false;

    @XNode("needFilterPreprocessing")
    private boolean needFilterPreprocessing = false;

    @XNode("needRedirectFilter")
    private boolean needRedirectFilter = false;

    @XNode("actionBinding")
    private String actionBinding;

    /**
     * EL expression resolving to a boolean value, and determines if {@link #documentViewBinding} and
     * {@link #newDocumentViewBinding} will attempt to be resolved using this descriptor
     */
    @XNode("documentViewBindingApplies")
    private String documentViewBindingApplies;

    @XNode("documentViewBinding")
    private String documentViewBinding;

    @XNode("newDocumentViewBinding")
    private String newDocumentViewBinding;

    @XNodeList(value = "bindings/binding", type = ValueBindingDescriptor[].class, componentType = ValueBindingDescriptor.class)
    private ValueBindingDescriptor[] valueBindings;

    @XNode("codecName")
    private String documentViewCodecName;

    public String getActionBinding() {
        return actionBinding;
    }

    public void setActionBinding(String actionBinding) {
        this.actionBinding = actionBinding;
    }

    public boolean getDefaultURLPolicy() {
        return defaultURLPolicy;
    }

    public void setDefaultURLPolicy(boolean defaultURLPolicy) {
        this.defaultURLPolicy = defaultURLPolicy;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getNeedBaseURL() {
        return needBaseURL;
    }

    public void setNeedBaseURL(boolean needBaseURL) {
        this.needBaseURL = needBaseURL;
    }

    public boolean getNeedFilterPreprocessing() {
        return needFilterPreprocessing;
    }

    public void setNeedFilterPreprocessing(boolean needFilterPreprocessing) {
        this.needFilterPreprocessing = needFilterPreprocessing;
    }

    public boolean getNeedRedirectFilter() {
        return needRedirectFilter;
    }

    public void setNeedRedirectFilter(boolean needRedirectFilter) {
        this.needRedirectFilter = needRedirectFilter;
    }

    public String getDocumentViewCodecName() {
        return documentViewCodecName;
    }

    public void setDocumentViewCodecName(String documentViewCodecName) {
        this.documentViewCodecName = documentViewCodecName;
    }

    public ValueBindingDescriptor[] getValueBindings() {
        return valueBindings;
    }

    public void setValueBindings(ValueBindingDescriptor[] valueBindings) {
        this.valueBindings = valueBindings;
    }

    public String getDocumentViewBindingApplies() {
        return documentViewBindingApplies;
    }

    public void setDocumentViewApplies(String documentViewBindingApplies) {
        this.documentViewBindingApplies = documentViewBindingApplies;
    }

    public String getDocumentViewBinding() {
        return documentViewBinding;
    }

    public void setDocumentViewBinding(String documentViewBinding) {
        this.documentViewBinding = documentViewBinding;
    }

    public String getNewDocumentViewBinding() {
        return newDocumentViewBinding;
    }

    public void setNewDocumentViewBinding(String newDocumentViewBinding) {
        this.newDocumentViewBinding = newDocumentViewBinding;
    }

}
