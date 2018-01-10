/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 *
 */

package org.nuxeo.wizard.download;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class DownloadablePackageOption {

    protected final DownloadPackage pkg;

    protected boolean exclusive;

    protected boolean selected = false;

    protected String label;

    protected List<DownloadablePackageOption> childrenPackages = new ArrayList<>();

    protected final String id;

    protected DownloadablePackageOption parent;

    public DownloadablePackageOption(DownloadPackage pkg, int idx) {
        this.pkg = pkg;
        // this.id = UUID.randomUUID().toString();
        id = "o" + idx;
    }

    public DownloadablePackageOption(DownloadPackage pkg, String id) {
        this.pkg = pkg;
        this.id = id;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public void setExclusive(String exclusive) {
        if (exclusive != null) {
            if ("true".equalsIgnoreCase(exclusive)) {
                this.exclusive = true;
            } else {
                this.exclusive = false;
            }
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<DownloadablePackageOption> getChildrenPackages() {
        return childrenPackages;
    }

    public void addChildPackage(DownloadablePackageOption child) {
        childrenPackages.add(child);
        child.setParent(this);
    }

    protected void setParent(DownloadablePackageOption parent) {
        this.parent = parent;
    }

    public List<DownloadablePackageOption> getSiblingPackages() {

        List<DownloadablePackageOption> siblings = new ArrayList<>();
        if (parent != null) {
            for (DownloadablePackageOption sibling : parent.getChildrenPackages()) {
                if (!sibling.getId().equals(getId())) {
                    siblings.add(sibling);
                }
            }
        }
        return siblings;
    }

    public String getLabel() {
        if (label == null && pkg != null) {
            return pkg.getLabel();
        }
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DownloadPackage getPackage() {
        return pkg;
    }

    public String getId() {
        return id;
    }

    public String getColor() {
        if (pkg != null) {
            return pkg.getColor();
        }
        return "";
    }

    /**
     * @since 8.3
     */
    public String getDescription() {
        if (pkg != null) {
            return pkg.getDescription();
        }
        return "";
    }

    /**
     * @since 8.3
     */
    public boolean isVirtual() {
        if (pkg != null) {
            return pkg.isVirtual();
        }
        return false;
    }

    public String getTextColor() {
        if (pkg != null) {
            return pkg.getTextColor();
        }
        return "";
    }

    public DownloadablePackageOption getParent() {
        return parent;
    }

    public String getShortLabel() {
        if (pkg != null) {
            return pkg.getShortLabel();
        }
        return null;
    }
}
