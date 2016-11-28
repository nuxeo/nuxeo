/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.management.statuses;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Instance identifier (mainly imported from connect client : TechnicalInstanceIdentifier)
 *
 * @author matic
 */
public class NuxeoInstanceIdentifierHelper {

    private static final String HASH_METHOD = "MD5";

    protected static final Log log = LogFactory.getLog(NuxeoInstanceIdentifierHelper.class);

    protected static String serverInstanceName;

    public static String generateHardwareUID() throws IOException {
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
                        try {
                            byte[] hwAddr = (byte[]) method.invoke(ni);
                            if (hwAddr != null) {
                                hwUID = hwUID + "-" + Base64.encodeBase64String(hwAddr);
                            }
                            break;
                        } catch (ReflectiveOperationException e) {
                            throw ExceptionUtils.runtimeException(e);
                        }
                    }
                }
            } else {
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    hwUID = hwUID + "-" + Base64.encodeBase64String(addrs.nextElement().getAddress());
                }
            }
        }
        return hwUID;
    }

    public static String summarize(String value) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance(HASH_METHOD).digest(value.getBytes());
        BigInteger sum = new BigInteger(digest);
        return sum.toString(16);
    }

    public static String newServerInstanceName() {

        String osName = System.getProperty("os.name");

        String hwInfo;
        try {
            hwInfo = generateHardwareUID();
            hwInfo = summarize(hwInfo);
        } catch (IOException | NoSuchAlgorithmException e) {
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
