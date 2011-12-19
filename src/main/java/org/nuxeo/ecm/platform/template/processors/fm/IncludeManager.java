/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.template.processors.fm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.template.InputType;
import org.nuxeo.ecm.platform.template.TemplateInput;

/**
 *
 * Helper class to manage the "##Include" directive that allow to integrate in
 * ODT template the formater content of an other Document.
 *
 * (Not working for now)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class IncludeManager {

    protected final static Pattern simpleVariableMatcher = Pattern.compile("\\#\\#Include\\(name=(.*)\\)");

    protected static final String INCLUDE_XML = "<text:section text:style-name=\"@@sectionstyle@@\" text:name=\"@@name@@\" text:protected=\"true\"> "
            + "<text:section-source xlink:href=\"../Content/@@filename@@\" "
            + " text:filter-name=\"HTML (StarWriter)\" xlink:actuate=\"onLoad\"/> "
            + " <text:p text:style-name=\"Standard\">PlaceHolder</text:p> "
            + "</text:section>";

    protected Map<String, String> includeDirectives = new HashMap<String, String>();

    protected Map<String, Blob> includeData = new HashMap<String, Blob>();

    public static List<TemplateInput> getIncludes(String content) {

        List<TemplateInput> includes = new ArrayList<TemplateInput>();

        Matcher matcher = simpleVariableMatcher.matcher(content);

        matcher.matches();
        while (matcher.find()) {
            if (matcher.groupCount() > 0) {
                TemplateInput include = new TemplateInput(matcher.group(1));
                include.setType(InputType.Include);
                includes.add(include);
            }
        }
        return includes;
    }

    public void addInclude(String name, DocumentModel doc, String propXPath) {

        String sectionStyle = "Sect1";
        String fileName = "include" + name + ".html";

        String include = INCLUDE_XML.replace("@@name@@", name);
        include = include.replace("@@sectionstyle@@", sectionStyle);
        include = include.replace("@@filename@@", fileName);

        includeDirectives.put(name, include);

        try {
            Serializable value = doc.getPropertyValue(propXPath);
            if (value != null) {
                Blob blob = null;
                if (Blob.class.isAssignableFrom(value.getClass())) {
                    blob = (Blob) value;
                } else {
                    blob = new StringBlob((String) value);
                }
                if (blob != null) {
                    blob.setFilename(fileName);
                    blob.setMimeType("text/html");
                    includeData.put(name, blob);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<String> getIncludeNames() {
        return includeData.keySet();
    }

    public String getDirective(String name) {
        return includeDirectives.get(name);
    }

    public Blob getDate(String name) {
        return includeData.get(name);
    }

    public List<Blob> getBlobs() {
        List<Blob> blobs = new ArrayList<Blob>();
        blobs.addAll(includeData.values());
        return blobs;
    }
}
