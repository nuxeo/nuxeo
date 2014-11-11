/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: Functions.java 28572 2008-01-08 14:40:44Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.tag.fn;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Util functions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public final class Functions {

    public enum BytePrefix {

        SI(1000, new String[] { "", "k", "M", "G", "T", "P", "E", "Z", "Y" },
                new String[] { "", "kilo", "mega", "giga", "tera", "exa",
                        "zetta", "yotta" }), IEC(1024, new String[] { "", "Ki",
                "Mi", "Gi", "Ti", "Pi", "Ei", "Zi", "Yi" }, new String[] { "",
                "kibi", "mebi", "tebi", "pebi", "exbi", "zebi", "yobi" }), JEDEC(
                1024, new String[] { "", "K", "M", "G" }, new String[] { "",
                        "kilo", "mega", "giga" });

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
     * Key in the session holding a map caching user full names.
     */
    private static final String FULLNAMES_MAP_KEY = Functions.class.getName()
            + ".FULLNAMES_MAP";

    static final Map<String, String> mapOfDateLength = new HashMap<String, String>() {
        {
            put("short", String.valueOf(DateFormat.SHORT));
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

    public static String joinCollection(Collection<Object> collection,
            String separator) {
        if (collection == null) {
            return null;
        }
        return StringUtils.join(collection.iterator(), separator);
    }

    /**
     * Can be used in order to produce something like that
     * "Julien, Alain , Thierry et Marc-Aur?le" where ' , ' and ' et ' is the
     * final one.
     *
     * @param collection
     * @param separator
     * @param finalDelimiter
     * @return
     */
    public static String joinCollectionWithFinalDelimiter(
            Collection<Object> collection, String separator,
            String finalSeparator) {
        return joinArrayWithFinalDelimiter(collection.toArray(), separator,
                finalSeparator);
    }

    public static String joinArrayWithFinalDelimiter(Object[] collection,
            String separator, String finalSeparator) {
        if (collection == null) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        int i = 0;
        for (Object object : collection) {
            result.append(object);
            if (++i == collection.length - 1)
                separator = finalSeparator;
            if (i != collection.length)
                result.append(separator);
        }

        return result.toString();
    }

    public static String formatDateUsingBasicFormatter(Date date) {
        return formatDate(date, basicDateFormater());
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

    private static UserManager getUserManager() throws ClientException {
        if (userManager == null) {
            try {
                // XXX this should not use a static variable to do the caching
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return userManager;
    }

    /**
     * Returns the full name of a user.
     *
     * @param username the user id, or null or empty for the current user.
     * @return the full user name.
     */
    @SuppressWarnings("unchecked")
    public static String userFullName(String username) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        // empty user name is current user
        if (username == null || username.length() == 0) {
            username = externalContext.getUserPrincipal().getName();
        }
        // check cache
        Map<String, Object> session = externalContext.getSessionMap();
        Map<String, String> fullNames = (Map<String, String>) session.get(FULLNAMES_MAP_KEY);
        if (fullNames != null && fullNames.containsKey(username)) {
            return fullNames.get(username);
        }
        // compute full name
        String fullName;
        try {
            NuxeoPrincipal principal = getUserManager().getPrincipal(username);
            if (principal != null) {
                fullName = principalFullName(principal);
            } else {
                fullName = username;
            }
        } catch (ClientException e) {
            fullName = username;
        }
        // put in cache
        if (fullNames == null) {
            fullNames = new HashMap<String, String>();
            session.put(FULLNAMES_MAP_KEY, fullNames);
        }
        fullNames.put(username, fullName);
        return fullName;
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

    public static String dateFormater(String formatLength) {

        // A map to store temporary available date format

        FacesContext context = FacesContext.getCurrentInstance();
        Locale locale = context.getViewRoot().getLocale();

        DateFormat aDateFormat = DateFormat.getDateInstance(
                Integer.parseInt(mapOfDateLength.get(formatLength.toLowerCase())),
                locale);

        // Cast to SimpleDateFormat to make "toPattern" method available
        SimpleDateFormat format = (SimpleDateFormat) aDateFormat;

        // return the date pattern
        return format.toPattern();
    }

    // method to format date in the standard short format
    public static String basicDateFormater() {
        return dateFormater("short");
    }

    // method to format date and time considering user's local
    public static String dateAndTimeFormater(String formatLength) {

        // A map to store temporary available date format

        FacesContext context = FacesContext.getCurrentInstance();
        Locale locale = context.getViewRoot().getLocale();

        DateFormat aDateFormat = DateFormat.getDateTimeInstance(
                Integer.parseInt(mapOfDateLength.get(formatLength.toLowerCase())),
                Integer.parseInt(mapOfDateLength.get(formatLength.toLowerCase())),
                locale);

        // Cast to SimpleDateFormat to make "toPattern" method available
        SimpleDateFormat format = (SimpleDateFormat) aDateFormat;

        // return the date pattern
        return format.toPattern();
    }

    // method to format date and time in the standard short format
    public static String basicDateAndTimeFormater() {
        return dateAndTimeFormater("short");
    }

    public static String printFileSize(String size) {
        return printFormatedFileSize(size, "SI", true);
    }

    public static String printFormatedFileSize(String sizeS, String format,
            Boolean isShort) {
        Integer size = (sizeS == null || "".equals(sizeS)) ? 0
                : Integer.parseInt(sizeS);
        BytePrefix prefix = Enum.valueOf(BytePrefix.class, format);
        int base = prefix.getBase();
        String[] suffix = isShort ? prefix.getShortSuffixes()
                : prefix.getLongSuffixes();
        int ex = 0;
        while (size > base - 1 || ex > suffix.length) {
            ex++;
            size /= base;
        }
        return "" + size + " " + suffix[ex] + "B";
    }


    public static Integer integerDivision(Integer x, Integer y) {
        return x / y;
    }

}
