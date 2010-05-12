/*
 * (C) Copyright 2006-2010 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.bank;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("import")
public final class BankImport  {

    @XNode("@dest")
    private String bankName;
    
    @XNode("@src")
    private String srcFilePath;

    public String getBankName() {
        return bankName;
    }

    public String getSrcFilePath() {
        return srcFilePath;
    }


}
