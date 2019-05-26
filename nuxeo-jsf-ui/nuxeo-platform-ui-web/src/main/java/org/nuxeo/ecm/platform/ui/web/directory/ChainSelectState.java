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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectState {

    private String directoryNames;

    private String[] directories;

    private String keySeparator;

    private boolean qualifiedParentKeys;

    private boolean recursive;

    private int depth;

    private String display;

    private boolean translate;

    private boolean showObsolete;

    private String style;

    private String styleClass;

    private int size;

    private boolean allowBranchSelection;

    private String reRender;

    private boolean displayValueOnly;

    protected String defaultRootKey;

    public boolean getQualifiedParentKeys() {
        return qualifiedParentKeys;
    }

    public void setQualifiedParentKeys(boolean qualifiedParentKeys) {
        this.qualifiedParentKeys = qualifiedParentKeys;
    }

    public String getKeySeparator() {
        return keySeparator;
    }

    public void setKeySeparator(String keySeparator) {
        this.keySeparator = keySeparator;
    }

    public String getDefaultRootKey() {
        return defaultRootKey;
    }

    public void setDefaultRootKey(String defaultRootKey) {
        this.defaultRootKey = defaultRootKey;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public boolean getTranslate() {
        return translate;
    }

    public void setTranslate(boolean translate) {
        this.translate = translate;
    }

    public boolean getShowObsolete() {
        return showObsolete;
    }

    public void setShowObsolete(boolean showObsolete) {
        this.showObsolete = showObsolete;
    }

    public String getDirectoryNames() {
        return directoryNames;
    }

    public void setDirectoryNames(String directoryNames) {
        this.directoryNames = directoryNames;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String[] getDirectories() {
        return directories;
    }

    public void setDirectories(String[] directories) {
        this.directories = directories;
    }

    public int getListboxSize() {
        return size;
    }

    public void setListboxSize(int size) {
        this.size = size;
    }

    public boolean getAllowBranchSelection() {
        return allowBranchSelection;
    }

    public void setAllowBranchSelection(boolean allowBranchSelection) {
        this.allowBranchSelection = allowBranchSelection;
    }

    public String getReRender() {
        return reRender;
    }

    public void setReRender(String reRender) {
        this.reRender = reRender;
    }

    public boolean getDisplayValueOnly() {
        return displayValueOnly;
    }

    public void setDisplayValueOnly(boolean displayValueOnly) {
        this.displayValueOnly = displayValueOnly;
    }

}
