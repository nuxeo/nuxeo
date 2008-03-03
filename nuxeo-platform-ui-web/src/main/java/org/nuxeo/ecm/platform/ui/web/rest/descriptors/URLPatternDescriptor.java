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
