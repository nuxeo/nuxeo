/*
 * (C) Copyright 2007-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: Functions.java 28572 2008-01-08 14:40:44Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.tag.fn;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentRenderUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Util functions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public final class Functions {

    public static final String I18N_DURATION_PREFIX = "label.duration.unit.";

    public static final String BIG_FILE_SIZE_LIMIT_PROPERTY = "org.nuxeo.big.file.size.limit";

    public static final long DEFAULT_BIG_FILE_SIZE_LIMIT = 5 * 1024 * 1024;

    /**
     * @since 7.4
     */
    public static final String BYTE_PREFIX_FORMAT_PROPERTY = "nuxeo.jsf.defaultBytePrefixFormat";

    /**
     * @since 7.4
     */
    public static final String DEFAULT_BYTE_PREFIX_FORMAT = "SI";

    public static final Pattern YEAR_PATTERN = Pattern.compile("y+");

    public enum BytePrefix {

        SI(1000, new String[] { "", "k", "M", "G", "T", "P", "E", "Z", "Y" }, new String[] { "", "kilo", "mega",
                "giga", "tera", "peta", "exa", "zetta", "yotta" }), IEC(1024, new String[] { "", "Ki", "Mi", "Gi",
                "Ti", "Pi", "Ei", "Zi", "Yi" }, new String[] { "", "kibi", "mebi", "gibi", "tebi", "pebi", "exbi",
                "zebi", "yobi" }), JEDEC(1024, new String[] { "", "K", "M", "G" }, new String[] { "", "kilo", "mega",
                "giga" });

        private final int base;

        private final String[] shortSuffixes;

        private final String[] longSuffixes;

        BytePrefix(int base, String[] shortSuffixes, String[] longSuffixes) {
            this.base = base;
            this.shortSuffixes = shortSuffixes;
            this.longSuffixes = longSuffixes;
        }

        public int getBase() {
            return base;
        }

        public String[] getShortSuffixes() {
            return shortSuffixes;
        }

        public String[] getLongSuffixes() {
            return longSuffixes;
        }

    }

    // XXX we should not use a static variable for this cache, but use a cache
    // at a higher level in the Framework or in a facade.
    private static UserManager userManager;

    /**
     * @since 7.2
     */
    private static UserNameResolverHelper userNameResolver = new UserNameResolverHelper();

    static final Map<String, String> mapOfDateLength = new HashMap<String, String>() {
        {
            put("short", String.valueOf(DateFormat.SHORT));
            put("shortWithCentury".toLowerCase(), "shortWithCentury".toLowerCase());
            put("medium", String.valueOf(DateFormat.MEDIUM));
            put("long", String.valueOf(DateFormat.LONG));
            put("full", String.valueOf(DateFormat.FULL));
        }

        private static final long serialVersionUID = 8465772256977862352L;
    };

    // Utility class.
    private Functions() {
    }

    public static Object test(Boolean test, Object onSuccess, Object onFailure) {
        return test ? onSuccess : onFailure;
    }

    public static String join(String[] list, String separator) {
        return StringUtils.join(list, separator);
    }

    public static String joinCollection(Collection<Object> collection, String separator) {
        if (collection == null) {
            return null;
        }
        return StringUtils.join(collection.iterator(), separator);
    }

    public static String htmlEscape(String data) {
        return StringEscapeUtils.escapeHtml(data);
    }

    /**
     * Escapes a given string to be used in a JavaScript function (escaping single quote characters for instance).
     *
     * @since 5.4.2
     */
    public static String javaScriptEscape(String data) {
        if (data != null) {
            data = StringEscapeUtils.escapeJavaScript(data);
        }
        return data;
    }

    /**
     * Can be used in order to produce something like that "Julien, Alain , Thierry et Marc-Aurele" where ' , ' and ' et
     * ' is the final one.
     */
    public static String joinCollectionWithFinalDelimiter(Collection<Object> collection, String separator,
            String finalSeparator) {
        return joinArrayWithFinalDelimiter(collection.toArray(), separator, finalSeparator);
    }

    public static String joinArrayWithFinalDelimiter(Object[] collection, String separator, String finalSeparator) {
        if (collection == null) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        result.append(StringUtils.join(collection, separator));
        if (collection != null && collection.length > 0 && !StringUtils.isBlank(finalSeparator)) {
            result.append(finalSeparator);
        }
        return result.toString();
    }

    public static String formatDateUsingBasicFormatter(Date date) {
        return formatDate(date, basicDateFormatter());
    }

    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public static String concat(String s1, String s2) {
        return s1 + s2;
    }

    public static String indentString(Integer level, String text) {
        StringBuilder label = new StringBuilder("");
        for (int i = 0; i < level; i++) {
            label.append(text);
        }
        return label.toString();
    }

    public static boolean userIsMemberOf(String groupName) {
        FacesContext context = FacesContext.getCurrentInstance();
        NuxeoPrincipal principal = (NuxeoPrincipal) context.getExternalContext().getUserPrincipal();
        return principal.isMemberOf(groupName);
    }

    private static UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getService(UserManager.class);
        }
        return userManager;
    }

    /**
     * Returns the full name of a user, or its username if user if not found.
     * <p>
     * Since 5.5, returns null if given username is null (instead of returning the current user full name).
     */
    public static String userFullName(String username) {
        if (SecurityConstants.SYSTEM_USERNAME.equals(username)) {
            // avoid costly and useless calls to the user directory
            return username;
        }

        // empty user name is current user
        if (StringUtils.isBlank(username)) {
            return null;
        }
        return userNameResolver.getUserFullName(username);
    }

    /**
     * Returns the full name of a group from his id
     *
     * @see #groupDisplayName(String, String)
     * @param groupId the group id
     * @return the group full name
     * @since 5.5
     */
    public static String groupFullName(String groupId) {
        NuxeoGroup group = getUserManager().getGroup(groupId);
        String groupLabel = group.getLabel();
        String groupName = group.getName();
        return groupDisplayName(groupName, groupLabel);
    }

    // this should be a method of the principal itself
    public static String principalFullName(NuxeoPrincipal principal) {
        String first = principal.getFirstName();
        String last = principal.getLastName();
        return userDisplayName(principal.getName(), first, last);
    }

    public static String userDisplayName(String id, String first, String last) {
        if (first == null || first.length() == 0) {
            if (last == null || last.length() == 0) {
                return id;
            } else {
                return last;
            }
        } else {
            if (last == null || last.length() == 0) {
                return first;
            } else {
                return first + ' ' + last;
            }
        }
    }

    /**
     * Return, from the id, the id its-self if neither last name nor name are found or the full name plus the email if
     * this one exists
     *
     * @param id id of the user
     * @param first first name of the user
     * @param last last name of the user
     * @param email email of the user
     * @return id or full name with email if exists
     * @since 5.5
     */
    public static String userDisplayNameAndEmail(String id, String first, String last, String email) {
        String userDisplayedName = userDisplayName(id, first, last);
        if (userDisplayedName.equals(id)) {
            return userDisplayedName;
        }
        if (email == null || email.length() == 0) {
            return userDisplayedName;
        }
        return userDisplayedName + " " + email;
    }

    /**
     * Choose between label or name the best string to display a group
     *
     * @param name the group name
     * @param label the group name
     * @return label if not empty or null, otherwise group name
     * @since 5.5
     */
    public static String groupDisplayName(String name, String label) {
        return StringUtils.isBlank(label) ? name : label;
    }

    /**
     * Return the date format to handle date taking the user's locale into account.
     *
     * @since 5.9.1
     */
    public static String dateFormatter(String formatLength) {
        // A map to store temporary available date format
        FacesContext context = FacesContext.getCurrentInstance();
        Locale locale = context.getViewRoot().getLocale();

        int style = DateFormat.SHORT;
        String styleString = mapOfDateLength.get(formatLength.toLowerCase());
        boolean addCentury = false;
        if ("shortWithCentury".toLowerCase().equals(styleString)) {
            addCentury = true;
        } else {
            style = Integer.parseInt(styleString);
        }

        DateFormat aDateFormat = DateFormat.getDateInstance(style, locale);

        // Cast to SimpleDateFormat to make "toPattern" method available
        SimpleDateFormat format = (SimpleDateFormat) aDateFormat;

        // return the date pattern
        String pattern = format.toPattern();

        if (style == DateFormat.SHORT && addCentury) {
            // hack to add century on generated pattern
            pattern = YEAR_PATTERN.matcher(pattern).replaceAll("yyyy");
        }
        return pattern;
    }

    /**
     * Return the date format to handle date taking the user's locale into account. Uses the pseudo "shortWithCentury"
     * format.
     *
     * @since 5.9.1
     */
    public static String basicDateFormatter() {
        return dateFormatter("shortWithCentury");
    }

    /**
     * Return the date format to handle date and time taking the user's locale into account.
     *
     * @since 5.9.1
     */
    public static String dateAndTimeFormatter(String formatLength) {

        // A map to store temporary available date format

        FacesContext context = FacesContext.getCurrentInstance();
        Locale locale = context.getViewRoot().getLocale();

        int style = DateFormat.SHORT;
        String styleString = mapOfDateLength.get(formatLength.toLowerCase());
        boolean addCentury = false;
        if ("shortWithCentury".toLowerCase().equals(styleString)) {
            addCentury = true;
        } else {
            style = Integer.parseInt(styleString);
        }

        DateFormat aDateFormat = DateFormat.getDateTimeInstance(style, style, locale);

        // Cast to SimpleDateFormat to make "toPattern" method available
        SimpleDateFormat format = (SimpleDateFormat) aDateFormat;

        // return the date pattern
        String pattern = format.toPattern();

        if (style == DateFormat.SHORT && addCentury) {
            // hack to add century on generated pattern
            pattern = YEAR_PATTERN.matcher(pattern).replaceAll("yyyy");
        }
        return pattern;
    }

    /**
     * Return the date format to handle date and time taking the user's locale into account. Uses the pseudo
     * "shortWithCentury" format.
     *
     * @since 5.9.1
     */
    public static String basicDateAndTimeFormatter() {
        return dateAndTimeFormatter("shortWithCentury");
    }

    /**
     * Returns the default byte prefix.
     *
     * @since 7.4
     */
    public static BytePrefix getDefaultBytePrefix() {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        return BytePrefix.valueOf(
                configurationService.getProperty(BYTE_PREFIX_FORMAT_PROPERTY, DEFAULT_BYTE_PREFIX_FORMAT));
    }

    public static String printFileSize(String size) {
        return printFormatedFileSize(size, getDefaultBytePrefix().name(), true);
    }

    public static String printFormatedFileSize(String sizeS, String format, Boolean isShort) {
        long size = (sizeS == null || "".equals(sizeS)) ? 0 : Long.parseLong(sizeS);
        BytePrefix prefix = Enum.valueOf(BytePrefix.class, format);
        int base = prefix.getBase();
        String[] suffix = isShort ? prefix.getShortSuffixes() : prefix.getLongSuffixes();
        int ex = 0;
        while (size > base - 1 || ex > suffix.length) {
            ex++;
            size /= base;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        String msg;
        if (context != null) {
            String bundleName = context.getApplication().getMessageBundle();
            Locale locale = context.getViewRoot().getLocale();
            msg = I18NUtils.getMessageString(bundleName, "label.bytes.suffix", null, locale);
            if ("label.bytes.suffix".equals(msg)) {
                // Set default value if no message entry found
                msg = "B";
            }
        } else {
            // No faces context, set default value
            msg = "B";
        }

        return "" + size + " " + suffix[ex] + msg;
    }

    /**
     * Format the duration of a media in a string of two consecutive units to best express the duration of a media,
     * e.g.:
     * <ul>
     * <li>1 hr 42 min</li>
     * <li>2 min 25 sec</li>
     * <li>10 sec</li>
     * <li>0 sec</li>
     * </ul>
     *
     * @param durationObj a Float, Double, Integer, Long or String instance representing a duration in seconds
     * @param i18nLabels a map to translate the days, hours, minutes and seconds labels
     * @return the formatted string
     */
    public static String printFormattedDuration(Object durationObj, Map<String, String> i18nLabels) {

        if (i18nLabels == null) {
            i18nLabels = new HashMap<String, String>();
        }
        double duration = 0.0;
        if (durationObj instanceof Float) {
            duration = ((Float) durationObj).doubleValue();
        } else if (durationObj instanceof Double) {
            duration = ((Double) durationObj).doubleValue();
        } else if (durationObj instanceof Integer) {
            duration = ((Integer) durationObj).doubleValue();
        } else if (durationObj instanceof Long) {
            duration = ((Long) durationObj).doubleValue();
        } else if (durationObj instanceof String) {
            duration = Double.parseDouble((String) durationObj);
        }

        int days = (int) Math.floor(duration / (24 * 60 * 60));
        int hours = (int) Math.floor(duration / (60 * 60)) - days * 24;
        int minutes = (int) Math.floor(duration / 60) - days * 24 * 60 - hours * 60;
        int seconds = (int) Math.floor(duration) - days * 24 * 3600 - hours * 3600 - minutes * 60;

        int[] components = { days, hours, minutes, seconds };
        String[] units = { "days", "hours", "minutes", "seconds" };
        String[] defaultLabels = { "d", "hr", "min", "sec" };

        String representation = null;
        for (int i = 0; i < components.length; i++) {
            if (components[i] != 0 || i == components.length - 1) {
                String i18nLabel = i18nLabels.get(I18N_DURATION_PREFIX + units[i]);
                if (i18nLabel == null) {
                    i18nLabel = defaultLabels[i];
                }
                representation = String.format("%d %s", components[i], i18nLabel);
                if (i < components.length - 1) {
                    i18nLabel = i18nLabels.get(I18N_DURATION_PREFIX + units[i + 1]);
                    if (i18nLabel == null) {
                        i18nLabel = defaultLabels[i + 1];
                    }
                    representation += String.format(" %d %s", components[i + 1], i18nLabel);
                }
                break;
            }
        }
        return representation;
    }

    public static String printFormattedDuration(Object durationObj) {
        return printFormattedDuration(durationObj, null);
    }

    public static final String translate(String messageId, Object... params) {
        return ComponentUtils.translate(FacesContext.getCurrentInstance(), messageId, params);
    }

    /**
     * @return the big file size limit defined with the property org.nuxeo.big.file.size.limit
     */
    public static long getBigFileSizeLimit() {
        return getFileSize(Framework.getProperty(BIG_FILE_SIZE_LIMIT_PROPERTY, ""));
    }

    public static long getFileSize(String value) {
        Pattern pattern = Pattern.compile("([1-9][0-9]*)([kmgi]*)", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(value.trim());
        long number;
        String multiplier;
        if (!m.matches()) {
            return DEFAULT_BIG_FILE_SIZE_LIMIT;
        }
        number = Long.valueOf(m.group(1));
        multiplier = m.group(2);
        return getValueFromMultiplier(multiplier) * number;
    }

    /**
     * Transform the parameter in entry according to these unit systems:
     * <ul>
     * <li>SI prefixes: k/M/G for kilo, mega, giga</li>
     * <li>IEC prefixes: Ki/Mi/Gi for kibi, mebi, gibi</li>
     * </ul>
     *
     * @param m : binary prefix multiplier
     * @return the value of the multiplier as a long
     */
    public static long getValueFromMultiplier(String m) {
        if ("k".equalsIgnoreCase(m)) {
            return 1L * 1000;
        } else if ("Ki".equalsIgnoreCase(m)) {
            return 1L << 10;
        } else if ("M".equalsIgnoreCase(m)) {
            return 1L * 1000 * 1000;
        } else if ("Mi".equalsIgnoreCase(m)) {
            return 1L << 20;
        } else if ("G".equalsIgnoreCase(m)) {
            return 1L * 1000 * 1000 * 1000;
        } else if ("Gi".equalsIgnoreCase(m)) {
            return 1L << 30;
        } else {
            return 1L;
        }
    }

    /**
     * Returns true if the faces context holds messages for given JSF component id, usually the form id.
     * <p>
     * Id given id is null, returns true if there is at least one client id with messages.
     * <p>
     * Since the form id might be prefixed with a container id in some cases, the method returns true if one of client
     * ids with messages stats with given id, or if given id is contained in it.
     *
     * @since 5.4.2
     */
    public static boolean hasMessages(String clientId) {
        Iterator<String> it = FacesContext.getCurrentInstance().getClientIdsWithMessages();
        if (clientId == null) {
            return it.hasNext();
        } else {
            while (it.hasNext()) {
                String id = it.next();
                if (id != null
                        && (id.startsWith(clientId + ":") || id.contains(":" + clientId + ":") || id.equals(clientId) || id.endsWith(":"
                                + clientId))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String userUrl(String patternName, String username, String viewId, boolean newConversation) {
        return userUrl(patternName, username, viewId, newConversation, null);
    }

    public static String userUrl(String patternName, String username, String viewId, boolean newConversation,
            HttpServletRequest req) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("username", username);
        DocumentView docView = new DocumentViewImpl(null, viewId, parameters);

        // generate url
        URLPolicyService service = Framework.getService(URLPolicyService.class);
        if (patternName == null || patternName.length() == 0) {
            patternName = service.getDefaultPatternName();
        }

        String baseURL = null;
        if (req == null) {
            baseURL = BaseURL.getBaseURL();
        } else {
            baseURL = BaseURL.getBaseURL(req);
        }

        String url = service.getUrlFromDocumentView(patternName, docView, baseURL);

        // pass conversation info if needed
        if (!newConversation && url != null) {
            url = RestHelper.addCurrentConversationParameters(url);
        }

        return url;
    }

    public static List<Object> combineLists(List<? extends Object>... lists) {
        List<Object> combined = new ArrayList<Object>();
        for (List<? extends Object> list : lists) {
            combined.addAll(list);
        }
        return combined;
    }

    /**
     * Helper that escapes a string used as a JSF tag id: this is useful to replace characters that are not handled
     * correctly in JSF context.
     * <p>
     * This method currently removes ASCII characters from the given string, and replaces "-" characters by "_" because
     * the dash is an issue for forms rendered in ajax (see NXP-10793).
     * <p>
     * Also starting digits are replaced by the "_" character because a tag id cannot start with a digit.
     *
     * @since 5.7
     * @return the escaped string
     */
    public static String jsfTagIdEscape(String base) {
        if (base == null) {
            return null;
        }
        int n = base.length();
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < n; i++) {
            char c = base.charAt(i);
            if (i == 0) {
                if (!Character.isLetter(c) && (c != '_')) {
                    res.append("_");
                } else {
                    res.append(c);
                }
            } else {
                if (!Character.isLetter(c) && !Character.isDigit(c) && (c != '_')) {
                    res.append("_");
                } else {
                    res.append(c);
                }
            }
        }
        return org.nuxeo.common.utils.StringUtils.toAscii(res.toString());
    }

    /**
     * Returns the extension from the given {@code filename}.
     * <p>
     * See {@link FilenameUtils#getExtension(String)}.
     *
     * @since 5.7
     */
    public static String fileExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    /**
     * Returns the base name from the given {@code filename}.
     * <p>
     * See {@link FilenameUtils#getBaseName(String)}.
     *
     * @since 5.7
     */
    public static String fileBaseName(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    /**
     * Joins two strings to get a valid render attribute for ajax components.
     *
     * @since 6.0
     */
    public static String joinRender(String render1, String render2) {
        if (StringUtils.isBlank(render1) && StringUtils.isBlank(render2)) {
            return "";
        }
        String res;
        if (StringUtils.isBlank(render1)) {
            res = render2;
        } else if (StringUtils.isBlank(render2)) {
            res = render1;
        } else {
            res = StringUtils.join(new String[] { render1, render2 }, " ");
            res = res.replaceAll("\\s+", " ");
        }
        res = res.trim();
        return res;
    }

    /**
     * Returns the target component absolute id given an anchor in the tree and a local id.
     * <p>
     * If given targetId parameter contains spaces, consider several ids should be resolved and split them.
     *
     * @since 6.0
     * @param anchor the component anchor, used a localization for the target component in the tree.
     * @param targetId the component to look for locally so as to return its absolute client id.
     */
    public static String componentAbsoluteId(UIComponent anchor, String targetId) {
        // handle case where several target ids could be given as input
        if (targetId == null) {
            return null;
        }
        if (targetId.contains(" ")) {
            String res = "";
            for (String t : targetId.split(" ")) {
                res = joinRender(res, ComponentRenderUtils.getComponentAbsoluteId(anchor, t.trim()));
            }
            return res;
        } else {
            return ComponentRenderUtils.getComponentAbsoluteId(anchor, targetId);
        }
    }
}
