/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.fm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;

/**
 * Helper class used to extract variable names from a FreeMarker template. This is used to initialize the
 * {@link TemplateInput} parameters. Extraction is for now simple and system may not detect all the cases, but user is
 * able to add parameters from the UI.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class FreeMarkerVariableExtractor {

    protected final static Pattern simpleVariableMatcher = Pattern.compile("\\$\\{([^\\}]*)\\}");

    protected final static String[] spliters = new String[] { ".", "?", "=", ">", "<", "!", " ", "[" };

    protected final static Pattern[] directiveMatchers = new Pattern[] { Pattern.compile("\\[\\#if\\s([^\\]]*)\\]"),
            Pattern.compile("\\[\\#list\\s[\\d\\.\\.]*(.+)\\sas\\s([^\\]]*)\\]") };

    protected final static Pattern[] assignMatchers = new Pattern[] { Pattern.compile("\\[\\#assign\\s(.+)=.*\\]") };

    protected static final List<String> reservedContextKeywords = new ArrayList<String>();

    protected static final String[] freeMarkerVariableSuffix = { "_index", "_has_next" };

    protected static String extractVariableName(String match) {

        String varName = match.trim();

        if (varName.startsWith("!")) {
            varName = varName.substring(1);
        }

        while (varName.startsWith("(")) {
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

    public static void resetReservedContextKeywords() {
        synchronized (reservedContextKeywords) {
            reservedContextKeywords.clear();
        }
    }

    protected static List<String> getreservedContextKeywords() {
        synchronized (reservedContextKeywords) {
            if (reservedContextKeywords.size() == 0) {
                TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);
                if (tps != null) {
                    reservedContextKeywords.addAll(tps.getReservedContextKeywords());
                }
            }
        }
        return reservedContextKeywords;
    }

    public static List<String> extractVariables(String content) {

        List<String> variables = new ArrayList<String>();

        List<String> blackListedVariables = new ArrayList<String>();

        if (content.length() > 10000) {
            // split content in multilines
            // otherwise some regexp won't capture everything
            content = content.replaceAll("</", "\n</");
        }

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
                        String localVariable = extractVariableName(dmatcher.group(2));
                        blackListedVariables.add(localVariable);
                        for (String suffix : freeMarkerVariableSuffix) {
                            blackListedVariables.add(localVariable + suffix);
                        }
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
        for (String bVar : getreservedContextKeywords()) {
            variables.remove(bVar);
        }        

        // remove any non valid variable names
        ListIterator<String> varIter = variables.listIterator();
        while (varIter.hasNext()) {
            String var = varIter.next();
            if (var.contains("<") || var.contains(">")) {
                varIter.remove();
            } else if (var.contains("\n")) {
                varIter.set(var.replaceAll("\n", "").trim());
            } else if (var.startsWith(".")) {
                // remove FM "Special Variables"
                varIter.remove();
            }
        }

        return variables;
    }

}
