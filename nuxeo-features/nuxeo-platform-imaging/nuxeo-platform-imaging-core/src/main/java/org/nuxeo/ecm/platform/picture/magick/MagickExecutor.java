/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.picture.magick;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to execute an ImageMagic command. Depends on the
 * {@link CommandLineExecutorService} to run external processes.
 *
 * @author tiry
 */
public class MagickExecutor {

    private static final Log log = LogFactory.getLog(MagickExecutor.class);

    protected static ExecResult execCommand(String commandName,
            CmdParameters params) throws CommandNotAvailable {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        return cles.execCommand(commandName, params);
    }

    /**
     * @deprecated since 5.6. Quoting file paths is done by {@link CmdParameters}.
     */
    @Deprecated
    protected static String formatFilePath(String filePath) {
        return String.format("\"%s\"", filePath);
    }

}
