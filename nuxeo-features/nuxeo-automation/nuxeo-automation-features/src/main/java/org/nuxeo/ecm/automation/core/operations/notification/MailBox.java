/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON<bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/*
 * MailBox expose resolver given a String, a List of String or an Array of String.
 * String can be a username, a groupname or a direct email. See javadoc.
 * A MailBox object represent a MailBox address.
 * @since 5.9.1
 */
public class MailBox {

    private static final Log log = LogFactory.getLog(MailBox.class);

    private static final String USER_PREFIX = "user:";

    public String firstname = "";

    public String lastname = "";

    public String address;

    /**
     * Fetch for each string given the mailbox target associated see
     */
    public static List<MailBox> fetchPersonsFromList(List<String> values, boolean isStrict) {
        if (values == null) {
            return new ArrayList<MailBox>();
        }

        List<MailBox> result = new ArrayList<MailBox>();
        for (String info : values) {
            result.addAll(fetchPersonsFromString(info, isStrict));
        }

        return result;
    }

    /**
     * Resolve value to find the mailbox associated. if strict is true and if value is prefixed by "user:" then find the
     * email address in his profile, otherwise the given string is considered as the email address. if strict is false,
     * and there is comma. The value is considered as a list. For each substring the resolution is as explained below :
     * if the substring startswith by "user:" then try to resolve the user email, otherwise try to fetch the user
     * without prefix if not found considered the string as an email address.
     */
    public static List<MailBox> fetchPersonsFromString(String value, boolean isStrict) {
        List<MailBox> result = new ArrayList<MailBox>();

        // if strict waiting simply the user account or direct email address
        if (isStrict) {
            result.add(new MailBox(value, isStrict));
            return result;
        }

        String[] valuesToResolve = value.split(",");
        UserManager umgr = Framework.getService(UserManager.class);
        for (String info : valuesToResolve) {

            if (info.startsWith("user:")) {
                result.add(new MailBox(info, isStrict));
                continue;
            }

            if (info.startsWith("group:")) {
                List<String> usernames = umgr.getUsersInGroupAndSubGroups(value.substring("group:".length()));
                for (String username : usernames) {
                    result.add(new MailBox("user:" + username, isStrict));
                }
                continue;
            }

            // Suppose that a username ?
            DocumentModel user = umgr.getUserModel(info);
            if (user != null) {
                String address = (String) user.getPropertyValue("email");
                String firstname = (String) user.getPropertyValue("firstName");
                String lastname = (String) user.getPropertyValue("lastName");
                result.add(new MailBox(address, firstname, lastname));
                continue;
            }

            // Suppose that a groupname ?
            DocumentModel group = umgr.getGroupModel(info);
            if (group != null) {
                @SuppressWarnings("unchecked")
                List<String> usernames = (List<String>) group.getPropertyValue(umgr.getGroupMembersField());
                if (usernames != null) {
                    for (String username : usernames) {
                        result.add(new MailBox("user:" + username, isStrict));
                    }
                    continue;
                }
            }
            if (!info.contains("@")) {
                log.warn("Can't really resolve the mailbox defined, anyway added. Check if something bad configured: "
                        + info);
            }
            result.add(new MailBox(info, null, null));

        }

        return result;
    }

    public MailBox(String address, String firstname, String lastname) {
        this.address = address;
        this.firstname = firstname == null ? "" : firstname;
        this.lastname = lastname == null ? "" : lastname;
    }

    public MailBox(DocumentModel user, boolean isStrict) {
        initFromDocumentModel(user);
    }

    public MailBox(String info, boolean isStrict) {
        if (info.startsWith(USER_PREFIX)) {
            String userId = info.substring(USER_PREFIX.length());
            DocumentModel user = getUmgr().getUserModel(userId);
            if (user != null) {
                initFromDocumentModel(user);
                return;
            }
        }

        if (!isStrict) {
            // Try to fetch it from usermanager without prefix
            DocumentModel user = getUmgr().getUserModel(info);
            initFromDocumentModel(user);
            return;
        }

        if (!info.contains("@")) {
            log.warn("Info given seems not well formed, please check (sent anyway): " + info);
        }
        // String is directly the email address
        address = info;

    }

    private void initFromDocumentModel(DocumentModel user) {
        if (user != null && user.getPropertyValue("email") != null
                && ((String) user.getPropertyValue("email")).contains("@")) {
            address = (String) user.getPropertyValue("email");
        }
        if (user != null && user.getPropertyValue("firstName") != null
                && !((String) user.getPropertyValue("firstName")).isEmpty()) {
            firstname = (String) user.getPropertyValue("firstName");
        }
        if (user != null && user.getPropertyValue("lastName") != null
                && !((String) user.getPropertyValue("lastName")).isEmpty()) {
            firstname = (String) user.getPropertyValue("lastName");
        }

    }

    public UserManager getUmgr() {
        return Framework.getService(UserManager.class);
    }

    /**
     * returning the mailbox address as String. If firstname and lastname is set add it into the returned string.
     */
    @Override
    public String toString() {
        if (!firstname.isEmpty() && !lastname.isEmpty()) {
            return firstname + " " + lastname + "<" + address + ">";
        }
        return address;
    }
}
