/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.runtime.api;


/**
 * Helper class to detect transaction type needed, cloned from DataSourceHelper
 *
 *
 * @author Stephane Lacoin
 */
public class TransactionTypeHelper {

    public static final String RESOURCE_LOCAL = "RESOURCE_LOCAL";
    public static final String JTA = "JTA";

    protected static final String PROPERTY_NAME = "org.nuxeo.runtime.txType";

    protected static String txType;


    public static void autodetect() {
        J2EEContainerDescriptor selectedContainer = J2EEContainerDescriptor.getSelected();
        if (selectedContainer == null) {
            txType = null;
        } else {
            txType = selectedContainer.txFactory;
        }
    }

    /**
     * Sets the prefix to be used (mainly for tests).
     */
    public static void setTxType(String txType) {
        TransactionTypeHelper.txType = txType;
    }

    /**
     * Get the JNDI prefix used for DataSource lookups.
     */
    public static String getTxType() {
        if (txType == null) {
            if (Framework.isInitialized()) {
                String configuredPrefix = Framework.getProperty(PROPERTY_NAME);
                if (configuredPrefix != null) {
                    txType = Framework.getProperty(PROPERTY_NAME);
                }
                if (txType == null) {
                    autodetect();
                }
            }
            if (txType==null) { // manage default
                txType = RESOURCE_LOCAL;
            }
        }
        return txType;
    }

}
