/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.theme.bank;

import java.net.URL;

import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;

public class BankService extends DefaultComponent {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.theme.bank.BankService");

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("banks")) {
            registerBankOperation(extension);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("banks")) {
            unregisterBankOperation(extension);
        }
    }

    private void unregisterBankOperation(Extension extension) {
    }

    private void registerBankOperation(Extension extension) {
        Object[] contribs = extension.getContributions();
        RuntimeContext extensionContext = extension.getContext();
        for (Object contrib : contribs) {
            if (contrib instanceof BankImport) {
                BankImport bankImport = (BankImport) contrib;
                String bankName = bankImport.getBankName();
                String srcFilePath = bankImport.getSrcFilePath();
                URL srcFileUrl = extensionContext.getResource(srcFilePath);
                BankManager.importBankData(bankName, srcFileUrl);
            }
        }
    }
}
