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

package org.nuxeo.template.fm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.template.api.TemplateInput;

/**
 * Helper class used to extract variable names from a FreeMarker template. This
 * is used to initialize the {@link TemplateInput} parameters.
 * 
 * Extraction is for now simple and system may not detect all the cases, but
 * user is able to add parameters from the UI.
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
 */
public class FreeMarkerVariableExtractor {

    protected final static Pattern simpleVariableMatcher = Pattern.compile("\\$\\{([^\\}]*)\\}");

    protected final static String[] spliters = new String[] { ".", "?", "=",
            ">", "<", "!", " ", "[" };

    protected final static Pattern[] directiveMatchers = new Pattern[] {
            Pattern.compile("\\[\\#if\\s([^\\]]*)\\]"),
            Pattern.compile("\\[\\#list\\s(.+)\\sas\\s([^\\]]*)\\]") };

    protected final static Pattern[] assignMatchers = new Pattern[] { Pattern.compile("\\[\\#assign\\s(.+)=.*\\]") };

    protected static String extractVariableName(String match) {

        String varName = match.trim();

        if (varName.startsWith("!")) {
            varName = varName.substring(1);
        }

        int idx = varName.indexOf(".");
        if (idx > 1) {
            varName = varName.substring(0, idx);
        }

        for (String spliter : spliters) {
            idx = varName.indexOf(spliter);
            if (idx > 1) {
                varName = varName.substring(0, idx);
            }
        }

        return varName;

    }

    public static List<String> extractVariables(String content) {

        List<String> variables = new ArrayList<String>();

        List<String> blackListedVariables = new ArrayList<String>();

        Matcher matcher = simpleVariableMatcher.matcher(content);

        matcher.matches();
        while (matcher.find()) {
            if (matcher.groupCount() > 0) {
                String v = extractVariableName(matcher.group(1));
                if (!variables.contains(v)) {
                    variables.add(v);
                }
            }
        }

        for (Pattern dPattern : directiveMatchers) {
            Matcher dmatcher = dPattern.matcher(content);
            dmatcher.matches();
            while (dmatcher.find()) {
                if (dmatcher.groupCount() > 0) {
                    String v = extractVariableName(dmatcher.group(1));
                    if (!variables.contains(v)) {
                        variables.add(v);
                    }
                    if (dmatcher.groupCount() > 1) {
                        blackListedVariables.add(extractVariableName(dmatcher.group(2)));
                    }
                }
            }
        }

        for (Pattern dPattern : assignMatchers) {
            Matcher dmatcher = dPattern.matcher(content);
            dmatcher.matches();
            while (dmatcher.find()) {
                if (dmatcher.groupCount() > 0) {
                    String v = extractVariableName(dmatcher.group(1));
                    blackListedVariables.add(extractVariableName(v));
                }
            }
        }

        // remove internal variables
        for (String bVar : blackListedVariables) {
            variables.remove(bVar);
        }

        // remove reserved variables that don't need specific bindings
        for (String bVar : FMContextBuilder.RESERVED_VAR_NAMES) {
            variables.remove(bVar);
        }

        // remove any non valid variable names
        Iterator<String> varIter = variables.iterator();
        while (varIter.hasNext()) {
            String var = varIter.next();
            if (var.contains("<") || var.contains(">")) {
                varIter.remove();
            }
        }

        return variables;
    }

}
