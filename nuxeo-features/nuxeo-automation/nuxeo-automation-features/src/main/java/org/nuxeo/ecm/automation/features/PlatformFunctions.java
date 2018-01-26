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
 *     Bogdan Stefanescu
 *     Ricardo Dias
 *     Estelle Giuly
 */
package org.nuxeo.ecm.automation.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PlatformFunctions extends CoreFunctions {

    private volatile DirectoryService dirService;

    private static final Log log = LogFactory.getLog(PlatformFunctions.class);

    private volatile UserManager userMgr;

    public static final String HIBERNATE_SEQUENCER_PROPERTY = "org.nuxeo.ecm.core.uidgen.sequencer.hibernate";

    public UserManager getUserManager() {
        if (userMgr == null) {
            userMgr = Framework.getService(UserManager.class);
        }
        return userMgr;
    }

    public DirectoryService getDirService() {
        if (dirService == null) {
            dirService = Framework.getService(DirectoryService.class);
        }
        return dirService;
    }

    public String getVocabularyLabel(String voc, String key) {
        try (Session session = getDirService().open(voc)) {
            if (!session.hasEntry(key)) {
                log.debug("Unable to find the key '" + key + "' in the vocabulary '" + voc + "'.");
                return key;
            }
            DocumentModel doc = session.getEntry(key);
            return (String) doc.getPropertyValue("label"); // no schema prefix for vocabularies
        }
    }

    public NuxeoPrincipal getPrincipal(String username) {
        return getUserManager().getPrincipal(username);
    }

    protected String getEmail(NuxeoPrincipal principal, String userSchemaName, String userEmailFieldName) {
        if (principal == null) {
            return null;
        }
        return (String) principal.getModel().getProperty(userSchemaName, userEmailFieldName);
    }

    public String getEmail(String username) {
        UserManager userManager = getUserManager();
        return getEmail(userManager.getPrincipal(username), userManager.getUserSchemaName(),
                userManager.getUserEmailField());
    }

    public Set<NuxeoPrincipal> getPrincipalsFromGroup(String group) {
        return getPrincipalsFromGroup(group, false);
    }

    public Set<NuxeoPrincipal> getPrincipalsFromGroup(String group, boolean ignoreGroups) {
        PrincipalHelper ph = new PrincipalHelper(getUserManager(), null);
        return ph.getPrincipalsFromGroup(group, !ignoreGroups);
    }

    public StringList getEmailsFromGroup(String group) {
        return getEmailsFromGroup(group, false);
    }

    public StringList getEmailsFromGroup(String group, boolean ignoreGroups) {
        PrincipalHelper ph = new PrincipalHelper(getUserManager(), null);
        Set<String> emails = ph.getEmailsFromGroup(group, !ignoreGroups);
        return new StringList(emails);
    }

    public StringList getPrincipalEmails(List<NuxeoPrincipal> principals) {
        StringList result = new StringList(principals.size());
        String schemaName = getUserManager().getUserSchemaName();
        String fieldName = getUserManager().getUserEmailField();
        for (NuxeoPrincipal principal : principals) {
            String email = getEmail(principal, schemaName, fieldName);
            if (!StringUtils.isEmpty(email)) {
                result.add(email);
            }
        }
        return result;
    }

    public StringList getEmails(List<String> usernames) {
        return getEmails(usernames, false);
    }

    /**
     * Returns user emails
     *
     * @param usernames list of user names
     * @param usePrefix indicates if user resolution takes into account nuxeo prefix <b>user:</b>
     * @since 5.5
     */
    public StringList getEmails(List<String> usernames, boolean usePrefix) {
        if (usernames == null) {
            return new StringList(0);
        }
        UserManager userManager = getUserManager();
        StringList result = new StringList(usernames.size());
        String schemaName = getUserManager().getUserSchemaName();
        String fieldName = getUserManager().getUserEmailField();
        for (String username : usernames) {
            NuxeoPrincipal principal = null;
            if (usePrefix) {
                if (username.startsWith(NuxeoPrincipal.PREFIX)) {
                    principal = userManager.getPrincipal(username.replace(NuxeoPrincipal.PREFIX, ""));
                }
            } else {
                principal = userManager.getPrincipal(username);
            }
            if (principal != null) {
                String email = getEmail(principal, schemaName, fieldName);
                if (!StringUtils.isEmpty(email)) {
                    result.add(email);
                }
            }
        }
        return result;
    }

    public String getNextId(final String key) {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        boolean useHibernate = configurationService.isBooleanPropertyTrue(HIBERNATE_SEQUENCER_PROPERTY);
        return getNextId(key, useHibernate ? "hibernateSequencer" : null);
    }

    public String getNextId(final String key, final String sequencerName) {
        UIDGeneratorService uidGeneratorService = Framework.getService(UIDGeneratorService.class);
        UIDSequencer seq = uidGeneratorService.getSequencer(sequencerName);
        return Long.toString(seq.getNextLong(key));
    }

    public static String htmlEscape(String str) {
        return StringEscapeUtils.escapeHtml(str);
    }

    /**
     * Escapes a string according to NXQL escaping rules.
     * <p>
     * The resulting string is safe to include between single quotes in a NXQL expression.
     *
     * @param str the input string
     * @return the escaped string
     * @since 6.0-HF21, 7.10
     */
    public static String nxqlEscape(String str) {
        return NXQL.escapeStringInner(str);
    }

    /**
     * Concatenate into the list given as first argument other arguments. Other arguments will be explosed if it is a
     * list of object. ex: concatenateInto(myList, a, anotherList) with a is scalar object and anotherList is a list of
     * object will produced myList.add(a) and the same for each object contained into the anotherList list.
     *
     * @param <T>
     * @param list List of values of type A
     * @param values Value can be instance of java.util.Collection<Object> or an array of Objects or simply a scalar
     *            Object. If Null, the parameter is ignored
     * @return the list that contains the list contain and value (see value description)
     * @exception xxxxx if in values there is at least one object type not compatible with the collection list
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> concatenateIntoList(List<T> list, Object... values) {

        if (list == null) {
            throw new IllegalArgumentException("First parameter must not be null");
        }

        for (Object value : values) {
            if (value == null) {
                continue;
            }

            if (value instanceof Object[]) {
                for (Object subValue : (Object[]) value) {
                    if (subValue != null) {
                        list.add((T) subValue);
                    }
                }
                continue;
            }

            if (value instanceof Collection) {
                for (Object subValue : (Collection<Object>) value) {
                    if (subValue != null) {
                        list.add((T) subValue);
                    }
                }
                continue;
            }

            list.add((T) value);

        }
        return list;
    }

    /**
     * Idem than concatenateInto except that a new list is created.
     */
    public <T> List<T> concatenateValuesAsNewList(Object... values) {

        List<T> result = new ArrayList<T>();
        return concatenateIntoList(result, values);
    }

    /**
     * Checks if a document with the supplied id (or path) exists.
     * @param session The CoreSession to obtain the document
     * @param idOrPath The document Id or path
     * @return true if the document exists, or false otherwise
     */
    public boolean documentExists(CoreSession session, String idOrPath) {
        DocumentRef documentRef = idOrPath.startsWith("/") ? new PathRef(idOrPath) : new IdRef(idOrPath);
        return session.exists(documentRef);
    }

}
