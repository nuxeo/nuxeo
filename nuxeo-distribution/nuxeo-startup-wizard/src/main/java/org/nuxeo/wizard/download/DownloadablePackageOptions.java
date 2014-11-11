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

public class DownloadablePackageOptions {

    protected List<DownloadablePackageOption> pkgOptions = new ArrayList<DownloadablePackageOption>();

    protected List<DownloadPackage> pkg4Download = new ArrayList<DownloadPackage>();

    public List<DownloadablePackageOption> getOptions() {
        return pkgOptions;
    }

    public void addOptions(DownloadablePackageOption pkgOption) {
        pkgOptions.add(pkgOption);
    }

    public int size() {
        return getOptions().size();
    }

    public DownloadablePackageOption get(int idx) {
        return getOptions().get(idx);
    }

    public void resetSelection() {
        pkg4Download = new ArrayList<DownloadPackage>();
        for (DownloadablePackageOption option : pkgOptions) {
            resetSelection(option);
        }
    }

    protected void resetSelection(DownloadablePackageOption option) {
        option.setSelected(false);
        for (DownloadablePackageOption child : option.getChildrenPackages()) {
            resetSelection(child);
        }
    }

    public void select(List<String> ids) {
        resetSelection();
        for (String id : ids) {
            DownloadablePackageOption option = findById(id, pkgOptions);
            option.setSelected(true);
            if (!pkg4Download.contains(option.getPackage())) {
                pkg4Download.add(option.getPackage());
            }
        }
    }

    protected DownloadablePackageOption findById(String id, List<DownloadablePackageOption> options) {
        for (DownloadablePackageOption option : options) {
            if (option.getId().equals(id)) {
                return option;
            }
            DownloadablePackageOption childOption = findById(id, option.getChildrenPackages());
            if (childOption!=null) {
                return childOption;
            }
        }
        return null;
    }

    public List<DownloadPackage> getPkg4Download() {
        return pkg4Download;
    }


}

