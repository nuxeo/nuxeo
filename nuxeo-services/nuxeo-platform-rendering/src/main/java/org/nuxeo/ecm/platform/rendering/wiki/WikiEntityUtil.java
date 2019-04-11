/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Cognium Systems SA - initial API and implementation
 *******************************************************************************/
package org.nuxeo.ecm.platform.rendering.wiki;

import java.util.HashMap;
import java.util.Map;

/**
 * Copied from last version of wikimodel
 *
 * @author kotelnikov
 */
public class WikiEntityUtil {

    // Utility class.
    private WikiEntityUtil() {
    }

    private static class Entity {

        public final int fHtmlCode;

        public final String fHtmlSymbol;

        public final String fWikiSymbol;

        private Entity(String wikiSymbol, String htmlSymbol, int htmlCode) {
            fWikiSymbol = wikiSymbol;
            fHtmlSymbol = htmlSymbol;
            fHtmlCode = htmlCode;
        }

    }

    private static final Map<String, Entity> fHtmlToWiki = new HashMap<>();

    private static final Entity[] fIdToWiki = new Entity[65535];

    private static final Map<String, Entity> fWikiToHtml = new HashMap<>();

    static {
        add("<", "lt", 8249);
        add(">", "gt", 8250);
        add("&", "amp", 38); // ???

        add("\'", "rsquo", 8217);
        add("(tm)", "trade", 8482);
        add("(TM)", "trade", 8482);
        add("(No)", "8470", 8470);
        add(" -- ", "ndash", 8211);
        add("---", "mdash", 8212);
        add(" --- ", "mdash", 8212);
        add("...", "hellip", 8230);
        add("(*)", "bull", 8226);
        add("(R)", "reg", 174);
        add("(r)", "reg", 174);
        add("(o)", "deg", 176);
        add("(C)", "copy", 169);
        add("(p)", "para", 182);
        add("(P)", "para", 182);
        add("(s)", "sect", 167);
        add("()", "nbsp", 160);
        add("<<", "laquo", 171);
        add(">>", "raquo", 187);
        // add("<", "lsaquo", 8249);
        // add(">", "rsaquo", 8250);

        // Currency
        add("(c)", "cent", 162);
        add("(E)", "euro", 8364);
        add("(O)", "curren", 164);
        add("(L)", "pound", 163);
        add("(Y)", "yen", 165);
        add("(f)", "fnof", 402);

        // Math
        add("+/-", "plusmn", 177);
        add("(S)", "sum", 8721);
        add("(/)", "divide", 247);
        add("(x)", "times", 215);
        add("(8)", "infin", 8734);
        add("(~)", "sim", 8764);
        add("!=", "ne", 8800);

        add("->", "rarr", 8594);
        add("-->", "rarr", 8594);
        add("--->", "rarr", 8594);

        add("<-", "larr", 8592);
        add("<--", "larr", 8592);
        add("<---", "larr", 8592);

        add("<->", "harr", 8596);
        add("<-->", "harr", 8596);
        add("<--->", "harr", 8596);

        add("=>", "rArr", 8658);
        add("==>", "rArr", 8658);
        add("===>", "rArr", 8658);

        add("<=", "lArr", 8658);
        add("<==", "lArr", 8658);
        add("<===", "lArr", 8658);

        add("<=>", "hArr", 8660);
        add("<==>", "hArr", 8660);
        add("<===>", "hArr", 8660);

        add("<=", "le", 8804);
        add(">=", "ge", 8805);
        add("!=", "ne", 8800);
        add("~=", "asymp", 8776);
    }

    private static void add(String wikiEnity, String htmlEntity, int id) {
        Entity entity = new Entity(wikiEnity, htmlEntity, id);
        fWikiToHtml.put(wikiEnity, entity);
        fHtmlToWiki.put(htmlEntity, entity);
        fIdToWiki[id] = entity;
    }

    /**
     * Returns an HTML code corresponding to the specified HTML entity.
     *
     * @param htmlEntity the HTML entity to transform to the corresponding HTML code
     * @return an HTML code corresponding to the specified HTML entity
     */
    public static int getHtmlCodeByHtmlEntity(String htmlEntity) {
        Entity entity = fHtmlToWiki.get(htmlEntity);
        return entity != null ? entity.fHtmlCode : 0;
    }

    /**
     * Returns an HTML code corresponding to the specified wiki entity.
     *
     * @param wikiEntity the wiki entity to transform to the corresponding HTML entity
     * @return an HTML code corresponding to the specified wiki entity
     */
    public static int getHtmlCodeByWikiSymbol(String wikiEntity) {
        Entity entity = fWikiToHtml.get(wikiEntity);
        return entity != null ? entity.fHtmlCode : 0;
    }

    /**
     * @param ch for this character the corresponding html entity will be returned
     * @return an html entity corresponding to the given character
     */
    public static String getHtmlSymbol(char ch) {
        Entity entity = fIdToWiki[ch];
        return entity != null ? entity.fWikiSymbol : null;
    }

    /**
     * @param wikiEntity for this wiki entity the corresponding html entity will be returned
     * @return an html entity corresponding to the given wiki symbol
     */
    public static String getHtmlSymbol(String wikiEntity) {
        Entity entity = fWikiToHtml.get(wikiEntity);
        return entity != null ? entity.fHtmlSymbol : null;
    }

    /**
     * @param ch for this character the corresponding wiki entity will be returned
     * @return an wiki entity corresponding to the given character
     */
    public static String getWikiSymbol(char ch) {
        Entity entity = fIdToWiki[ch];
        return entity != null ? entity.fWikiSymbol : null;
    }

    /**
     * @param htmlEntity for this html entity the corresponding wiki entity will be returned
     * @return an wiki entity corresponding to the given html symbol
     */
    public static String getWikiSymbol(String htmlEntity) {
        Entity entity = fHtmlToWiki.get(htmlEntity);
        return entity != null ? entity.fHtmlSymbol : null;
    }

}
