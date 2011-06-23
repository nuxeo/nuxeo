/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_BIRTHDATE_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_PHONENUMBER_FIELD;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@code UserProfile}.
 *
 * @see UserProfile
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.4.3
 */
public class UserProfileAdapter extends AbstractLocalConfiguration<UserProfile>
        implements UserProfile {

    protected DocumentModel doc;

    public UserProfileAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public DocumentRef getDocumentRef() {
        return doc.getRef();
    }

    @Override
    public Blob getAvatar() throws ClientException {
        return (Blob) doc.getPropertyValue(USER_PROFILE_AVATAR_FIELD);
    }

    @Override
    public Calendar getBirthDate() throws ClientException {
        return (Calendar) doc.getPropertyValue(USER_PROFILE_BIRTHDATE_FIELD);
    }

    @Override
    public String getPhoneNumber() throws ClientException {
        return (String) doc.getPropertyValue(USER_PROFILE_PHONENUMBER_FIELD);
    }

}
