/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.management.statuses;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Instance identifier (mainly imported from connect client : TechnicalInstanceIdentifier)
 * 
 * @author matic
 *
 */
public class NuxeoInstanceIdentifierHelper {

    private static final String HASH_METHOD = "MD5";

    protected static final Log log = LogFactory.getLog(NuxeoInstanceIdentifierHelper.class);

    protected static String serverInstanceName;

    public static String generateHardwareUID() throws Exception {
        String hwUID = "";

        String javaVersion = System.getProperty("java.version");

        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();

        while (ifs.hasMoreElements()) {
            NetworkInterface ni = ifs.nextElement();

            if (javaVersion.contains("1.6")) {
                // ni.getHardwareAddress() only in jdk 1.6
                Method[] methods = ni.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equalsIgnoreCase("getHardwareAddress")) {
                        byte[] hwAddr = (byte[]) method.invoke(ni);
                        if (hwAddr != null) {
                            hwUID = hwUID + "-" + Base64.encodeBytes(hwAddr);
                        }
                        break;
                    }
                }
            } else {
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    hwUID = hwUID
                            + "-"
                            + Base64.encodeBytes(addrs.nextElement().getAddress());
                }
            }
        }
        return hwUID;
    }

    public static String summarize(String value) throws NoSuchAlgorithmException {
            byte[] digest;
                digest = MessageDigest.getInstance(HASH_METHOD).digest(
                        value.getBytes());
            BigInteger sum = new BigInteger(digest);
            return sum.toString(16);
    }
    
    public static String newServerInstanceName() {

        String osName = System.getProperty("os.name"); 
        
        String hwInfo;
        try {
            hwInfo = generateHardwareUID();
            hwInfo = summarize(hwInfo);
        } catch (Exception e1) {
            hwInfo = "***";
        }

        String instancePath;
        try {
            instancePath = Framework.getRuntime().getHome().toString();
            instancePath = summarize(instancePath);
        } catch (NoSuchAlgorithmException e) {
            instancePath = "***";
        }

        return osName + "-" + instancePath + "-" + hwInfo;

    }

    public static String getServerInstanceName() {
        if (serverInstanceName == null) {
            serverInstanceName = Framework.getProperty(AdministrativeStatusManager.ADMINISTRATIVE_INSTANCE_ID);
            if (StringUtils.isEmpty(serverInstanceName)) {
                serverInstanceName = newServerInstanceName();
            }
        }

        return serverInstanceName;
    }
}
