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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class DownloadablePackageOptions {

    protected List<DownloadablePackageOption> pkgOptions = new ArrayList<DownloadablePackageOption>();

    protected List<DownloadPackage> pkg4Download = new ArrayList<DownloadPackage>();

    protected List<DownloadPackage> commonPackages = new ArrayList<DownloadPackage>();

    protected List<DownloadPackage> allPackages = new ArrayList<DownloadPackage>();

    protected List<Preset> presets = new ArrayList<Preset>();

    protected static final Log log = LogFactory.getLog(DownloadablePackageOptions.class);

    public List<DownloadablePackageOption> getOptions() {
        return pkgOptions;
    }

    public void addOptions(DownloadablePackageOption pkgOption) {
        pkgOptions.add(pkgOption);
    }

    public void addCommonPackage(DownloadPackage pkg) {
        commonPackages.add(pkg);
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

    public List<String> checkSelectionValid(List<String> ids) {
        for (String id : ids) {
            DownloadablePackageOption option = findById(id, pkgOptions);
            if (option==null) {
                List<String> newIds = new ArrayList<String>();
                newIds.addAll(ids);
                newIds.remove(id);
                return checkSelectionValid(newIds);
            }
            // force selection of parents
            if (option.getParent() != null
                    && !ids.contains(option.getParent().getId())) {
                List<String> newIds = new ArrayList<String>();
                newIds.addAll(ids);
                newIds.add(option.getParent().getId());
                return checkSelectionValid(newIds);
            }
            // check constraints
            if (option.isExclusive()) {
                for (DownloadablePackageOption sib : option.getSiblingPackages()) {
                    if (ids.contains(sib.getId())) {
                        ids.remove(option.getId());
                        log.warn("Unsatisfied constraints in selection ... fixing");
                        return checkSelectionValid(ids);
                    }
                }
            } else {
                for (DownloadablePackageOption sib : option.getSiblingPackages()) {
                    if (ids.contains(sib.getId()) && sib.isExclusive()) {
                        ids.remove(sib.getId());
                        log.warn("Unsatisfied constraints in selection ... fixing");
                        return checkSelectionValid(ids);
                    }
                }
            }
        }
        return ids;
    }

    protected void markForDownload(String pkgId) {
        for (DownloadPackage pkg : allPackages) {
            if (pkg.getId().equals(pkgId)) {
                markForDownload(pkg);
                break;
            }
        }
    }
    protected void markForDownload(DownloadPackage pkg) {
        if (!pkg4Download.contains(pkg) && pkg.getFilename() != null
                && !"".equals(pkg.getFilename())) {
            pkg4Download.add(pkg);
            for (String dep : pkg.getImpliedDeps()) {
                markForDownload(dep);
            }
        }
    }

    public void select(List<String> ids) {
        resetSelection();
        ids = checkSelectionValid(ids);
        for (String id : ids) {
            DownloadablePackageOption option = findById(id, pkgOptions);
            option.setSelected(true);
            DownloadPackage pkg = option.getPackage();
            if (pkg != null) {
                markForDownload(pkg);
            }
        }
    }

    protected DownloadablePackageOption findById(String id,
            List<DownloadablePackageOption> options) {
        for (DownloadablePackageOption option : options) {
            if (option.getId().equals(id)) {
                return option;
            }
            DownloadablePackageOption childOption = findById(id,
                    option.getChildrenPackages());
            if (childOption != null) {
                return childOption;
            }
        }
        return null;
    }

    public List<DownloadPackage> getPkg4Download() {
        List<DownloadPackage> pkgs = new ArrayList<DownloadPackage>(
                commonPackages);
        pkgs.addAll(pkg4Download);
        return pkgs;
    }

    protected void asJson(DownloadablePackageOption option, StringBuffer sb) {
        sb.append("{");
        sb.append("\"id\":\"" + option.id + "\",");
        sb.append("\"package\":\"" + option.getPackage().getId() + "\",");
        sb.append("\"color\":\"" + option.getColor() + "\",");
        sb.append("\"textcolor\":\"" + option.getTextColor() + "\",");
        sb.append("\"label\":\"" + option.getLabel() + "\",");
        sb.append("\"shortlabel\":\"" + option.getShortLabel() + "\",");
        sb.append("\"selected\":\"" + option.selected + "\",");
        sb.append("\"exclusive\":\"" + option.exclusive + "\",");
        sb.append("\"children\": [");
        List<DownloadablePackageOption> children = option.getChildrenPackages();
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            asJson(children.get(i), sb);
        }
        sb.append("] }");
    }

    protected DownloadablePackageOption getSelectedRoot() {
        for (DownloadablePackageOption option : pkgOptions) {
            if (option.isSelected()) {
                return option;
            }
        }
        return pkgOptions.get(0);
    }

    public String asJson() {
        StringBuffer sb = new StringBuffer();
        asJson(getSelectedRoot(), sb);
        return sb.toString();
    }

    void addPreset(String id, String label, String[] pkgIds) {
        presets.add(new Preset(id, label, pkgIds));
    }

    public List<Preset> getPresets() {
        return presets;
    }

    public List<DownloadPackage> getAllPackages() {
        return allPackages;
    }

    public void setAllPackages(List<DownloadPackage> allPackages) {
        this.allPackages = allPackages;
    }


}
