/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.oauth2.openid.auth;

import java.util.Date;

/**
 * UserInfo Claims.
 * OpenID Connect Basic Client Profile 1.0 - draft 24
 * @see http://openid.net/specs/openid-connect-basic-1_0.html
 */
public interface OpenIDUserInfo {

    /** @return Subject - Identifier for the End-User at the Issuer. */
    String getSubject();

    /** @return End-User's full name in displayable form including all name parts, ordered according to End-User's locale and preferences */
    String getName();

    /** @return  Given name or first name of the End-User. */
    String getGivenName();

    /** @return Surname or last name of the End-User. */
    String getFamilyName();

    /** @return Middle name of the End-User. */
    String getMiddleName();

    /** @return Casual name of the End-User that may or may not be the same as the given_name. */
    String getNickname();

    /** @return Shorthand name that the End-User wishes to be referred to. */
    String getPreferredUsername();

    /** @return URL of the End-User's profile page. */
    String getProfile();

    /** @return URL of the End-User's profile picture. */
    String getPicture();

    /** @return URL of the End-User's web page or blog. */
    String getWebsite();

    /** @return End-User's preferred e-mail address. */
    String getEmail();

    /** @return True if the End-User's e-mail address has been verified; otherwise false. */
    boolean isEmailVerified();

    /** @return End-User's gender. (female or male). */
    String getGender();

    /** @return End-User's birthday */
    Date getBirthdate();

    /** @return String from zoneinfo time zone database representing the End-User's time zone. */
    String getZoneInfo();

    /** @return End-User's locale. */
    String getLocale();

    /** @return End-User's preferred telephone number. */
    String getPhoneNumber();

    /** @return End-User's preferred address. */
    String getAddress();

    /** @return Time the End-User's information was last updated. */
    Date getUpdatedTime();

}