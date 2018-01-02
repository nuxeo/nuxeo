/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class DownloadablePackageOptions {

    protected List<DownloadablePackageOption> pkgOptions = new ArrayList<>();

    protected List<DownloadPackage> pkg4Install = new ArrayList<>();

    protected List<DownloadPackage> commonPackages = new ArrayList<>();

    protected List<DownloadPackage> allPackages = new ArrayList<>();

    protected List<Preset> presets = new ArrayList<>();

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
        pkg4Install = new ArrayList<>();
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
            if (option == null) {
                List<String> newIds = new ArrayList<>();
                newIds.addAll(ids);
                newIds.remove(id);
                return checkSelectionValid(newIds);
            }
            // force selection of parents
            if (option.getParent() != null && !ids.contains(option.getParent().getId())) {
                List<String> newIds = new ArrayList<>();
                newIds.addAll(ids);
                newIds.add(0, option.getParent().getId());
                return checkSelectionValid(newIds);
            }
            // force selection of implies
            if (option.getPackage() != null && option.getPackage().getImpliedDeps() != null) {
                List<String> newIds = new ArrayList<>();
                newIds.addAll(ids);
                boolean needRecheck = false;
                for (String implied : option.getPackage().getImpliedDeps()) {
                    if (!ids.contains(implied)) {
                        if (findById(id, pkgOptions) != null) {
                            if (option.isExclusive() && option.getSiblingPackages().stream().anyMatch(
                                    sib -> sib.getId().equals(implied))) {
                                log.error(String.format(
                                        "Option %s cannot be exclusive and imply one of its sibling packages", id));
                                continue;
                            }
                            newIds.add(implied);
                            needRecheck = true;
                        }
                    }
                }
                if (needRecheck) {
                    return checkSelectionValid(newIds);
                }
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
        if (!pkg4Install.contains(pkg)) {
            if (pkg.getFilename() != null && !"".equals(pkg.getFilename())) {
                for (String dep : pkg.getImpliedDeps()) {
                    markForDownload(dep);
                }
            }
            pkg4Install.add(pkg);
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

    protected DownloadablePackageOption findById(String id, List<DownloadablePackageOption> options) {
        for (DownloadablePackageOption option : options) {
            if (option.getId().equals(id)) {
                return option;
            }
            DownloadablePackageOption childOption = findById(id, option.getChildrenPackages());
            if (childOption != null) {
                return childOption;
            }
        }
        return null;
    }

    public List<DownloadPackage> getPkg4Install() {
        List<DownloadPackage> pkgs = new ArrayList<>(commonPackages);
        pkgs.addAll(pkg4Install);
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
        sb.append("\"description\":\"" + option.getDescription() + "\",");
        sb.append("\"virtual\":\"" + option.isVirtual() + "\",");
        sb.append("\"implies\": [");
        for (int i = 0; i < option.getPackage().getImpliedDeps().size(); i++) {
            sb.append("\"" + option.getPackage().getImpliedDeps().get(i).trim() + "\"");
            if (i < option.getPackage().getImpliedDeps().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("],");
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
