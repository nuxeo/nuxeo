/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */

package org.nuxeo.wizard.download;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class DownloadablePackageOption {

    protected final DownloadPackage pkg;

    protected boolean exclusive;

    protected boolean selected = false;

    protected String label;

    protected List<DownloadablePackageOption> childrenPackages = new ArrayList<DownloadablePackageOption>();

    protected final String id;

    public DownloadablePackageOption(DownloadPackage pkg) {
        this.pkg = pkg;
        this.id = UUID.randomUUID().toString();
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public void setExclusive(String exclusive) {
        if (exclusive!=null) {
            if ("true".equalsIgnoreCase(exclusive)) {
                this.exclusive=true;
            } else {
                this.exclusive=false;
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
    }

    public String getLabel() {
        if (label==null) {
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
    
    
}
