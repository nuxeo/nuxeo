/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.demo.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoLayout;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;

/**
 * @author Anahide Tchertchian
 */
@XObject("layout")
public class DemoLayoutDescriptor implements DemoLayout {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@src")
    protected String sourcePath;

    @XNode("@isListing")
    protected boolean listing = false;

    @XNode("@hideViewMode")
    protected boolean hideViewMode = false;

    /**
     * @since 7.2
     */
    @XNode("@hideEditMode")
    protected boolean hideEditMode = false;

    /**
     * @since 7.2
     */
    @XNode("@addForm")
    protected boolean addForm = true;

    /**
     * @since 7.2
     */
    @XNode("@useAjaxForm")
    protected boolean useAjaxForm = true;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSourcePath() {
        return LayoutDemoManager.APPLICATION_PATH + sourcePath;
    }

    @Override
    public boolean isListing() {
        return listing;
    }

    @Override
    public boolean isHideViewMode() {
        return hideViewMode;
    }

    @Override
    public boolean isHideEditMode() {
        return hideEditMode;
    }

    @Override
    public boolean isAddForm() {
        return addForm;
    }

    @Override
    public boolean isUseAjaxForm() {
        return useAjaxForm;
    }

}
