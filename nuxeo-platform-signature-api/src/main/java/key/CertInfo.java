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
 *     ws@nuxeo.com
 */

package key;

/**
 * Holds Parameters for certificate configuration
 *
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
public class CertInfo {

    private String userID;

    private String userDN;

    private String certSubject;

    private int validMillisBefore;

    private int validMillisAfter;

    private String keyAlgorithm;

    private String certSignatureAlgorithm;

    private String securityProviderName;

    private String signingReason;

    private int numBits;

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

    public String getSecurityProviderName() {
        return securityProviderName;
    }

    public void setSecurityProviderName(String securityProviderName) {
        this.securityProviderName = securityProviderName;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public String getCertSignatureAlgorithm() {
        return certSignatureAlgorithm;
    }

    public void setCertSignatureAlgorithm(String certSignatureAlgorithm) {
        this.certSignatureAlgorithm = certSignatureAlgorithm;
    }

    public int getValidMillisBefore() {
        return validMillisBefore;
    }

    public void setValidMillisBefore(int validMillisBefore) {
        this.validMillisBefore = validMillisBefore;
    }

    public int getValidMillisAfter() {
        return validMillisAfter;
    }

    public void setValidMillisAfter(int validMillisAfter) {
        this.validMillisAfter = validMillisAfter;
    }

    public String getCertSubject() {
        return certSubject;
    }

    public void setCertSubject(String certSubject) {
        this.certSubject = certSubject;
    }

    public String getUserDN() {
        return userDN;
    }

    public void setUserDN(String userDN) {
        this.userDN = userDN;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

}
