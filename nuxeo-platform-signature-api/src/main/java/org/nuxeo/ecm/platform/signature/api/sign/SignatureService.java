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

package org.nuxeo.ecm.platform.signature.api.sign;

import java.io.File;
import java.io.InputStream;

import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;

/**
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
public interface SignatureService{
    /**
     *
     * Signs a provided PDF document
     *
     * @param certInfo
     * @param origPdfStream
     * @return
     * @throws Exception
     */
    public File signPDF(CertInfo certInfo, InputStream origPdfStream) throws Exception;
}
