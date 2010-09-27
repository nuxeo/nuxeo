/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.api.pki;

import java.util.Date;

/**
 * Holds certificate configuration parameters
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class CertInfo {

    // algorithm info
    private String keyAlgorithm;

    private String certSignatureAlgorithm;

    private int numBits;

    // user info

    private String userID;

    private String userDN;

    private String userName;

    private String userEmail;

    // cert info
    private Date validFrom;

    private Date validTo;

    private String signingReason;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public int getNumBits() {
        return numBits;
    }

    public void setNumBits(int numBits) {
        this.numBits = numBits;
    }

    public String getSigningReason() {
        return signingReason;
    }

    public void setSigningReason(String signingReason) {
        this.signingReason = signingReason;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getCertSignatureAlgorithm() {
        return certSignatureAlgorithm;
    }

    public void setCertSignatureAlgorithm(String certSignatureAlgorithm) {
        this.certSignatureAlgorithm = certSignatureAlgorithm;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userProvidedName) {
        this.userName = "CN=" + userProvidedName;
    }

    public String getUserDN() {
        return userDN;
    }

    public void setUserDN(String userDN) {
        this.userDN = userDN;
    }

}