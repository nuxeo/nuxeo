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

/**
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class Preset {

    protected final String id;

    protected final String label;

    protected final String[] pkgs;

    public Preset(String id, String label, String[] pkgs) {
        this.id = id;
        this.label = label;
        this.pkgs = pkgs;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String[] getPkgs() {
        return pkgs;
    }

    public String getPkgsAsJsonArray() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < pkgs.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("'" + pkgs[i] + "'");
        }
        sb.append("]");
        return sb.toString();
    }

}
