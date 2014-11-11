/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     madarche
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands.user;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;
import org.nuxeo.runtime.api.Framework;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author M.-A. Darche
 */
public class GroupModCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(GroupModCommand.class);

    public static final String COMMAND_NAME = "groupmod";

    public static final int CSV_COLUMN_COUNT = 1;

    public static final String GROUP_DIRECTORY_NAME = "groupDirectory";

    public void printUsage() {
        log.error("Usage:\n" + COMMAND_NAME + " --file USERS_FILE.csv\n"
                + COMMAND_NAME + " --user username groupname");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        // Parsing the command line
        String[] elements = cmdLine.getParameters();
        if (elements.length != 1) {
            printUsage();
            return;
        }

        // Let's parse all the command line before opening any connection to the
        // repository
        String groupName = elements[0];
        boolean importThroughCsv = false;
        File csvFile = null;
        String userName = null;
        String optionName = "file";
        if (cmdLine.isOptionSet(optionName)) {
            String csvFileName = cmdLine.getOption(optionName);
            log.debug("Reading user definitions of CSV file " + csvFileName);
            csvFile = new File(csvFileName);
            importThroughCsv = true;
        } else {
            optionName = "user";
            if (cmdLine.isOptionSet(optionName)) {
                userName = cmdLine.getOption(optionName);
                log.debug("Adding user " + userName + " to group " + groupName
                        + " ...");
            } else {
                printUsage();
                return;
            }
        }

        optionName = "set";
        boolean appendUsers = true;
        if (cmdLine.isOptionSet(optionName)) {
            appendUsers = false;
        }

        // Opening a connection to the repository
        RepositoryInstance repository = null;
        Session directorySession = null;
        try {
            repository = context.getRepositoryInstance();
            log.debug("Repository connection: " + repository);
            DirectoryService dirService = Framework.getService(DirectoryService.class);
            log.debug("Directory names: " + dirService.getDirectoryNames());
            directorySession = dirService.open(GROUP_DIRECTORY_NAME);
            if (importThroughCsv) {
                importCsvFile(csvFile, appendUsers, groupName, directorySession);
            } else {
                modifyGroup(Collections.singletonList(userName), appendUsers,
                        groupName, directorySession);
            }
        } finally {
            if (directorySession != null) {
                directorySession.close();
            }
            if (repository != null) {
                repository.close();
            }
        }
    }

    private void importCsvFile(File file, boolean appendUsers,
            String groupName, Session directorySession) throws Exception {
        CSVReader csvReader = new CSVReader(new FileReader(file));
        List<String> userNames = new ArrayList<String>();

        String[] nextLine;
        boolean firstLine = true;
        while ((nextLine = csvReader.readNext()) != null) {
            // nextLine is an array of strings from the line
            if (!firstLine) {
                if (nextLine.length == 0
                        || (nextLine.length == 1 && (nextLine[0] == null || "".equals(nextLine[0])))) {
                    // Don't process empty lines
                    continue;
                }
                if (nextLine.length == CSV_COLUMN_COUNT) {
                    userNames.add(nextLine[0]);
                } else {
                    log.error("The following CSV line will not be imported because it hasn't the requiread "
                            + CSV_COLUMN_COUNT
                            + " field "
                            + "\n"
                            + Arrays.asList(nextLine));
                }
            }
            firstLine = false;
        }
        csvReader.close();
        modifyGroup(userNames, appendUsers, groupName, directorySession);
    }

    @SuppressWarnings("unchecked")
    private void modifyGroup(List<String> userNames, boolean appendUsers,
            String groupName, Session directorySession) throws Exception {
        DocumentModel entry = directorySession.getEntry(groupName);
        log.debug("Entry = " + entry);
        if (entry == null) {
            log.error("The group denoted by \"" + groupName
                    + "\" doesn't exist in the directory.");
            return;
        }
        if (appendUsers) {
            List<String> previousUserNames = (List<String>) entry.getProperty(
                    "group", "members");
            userNames.addAll(previousUserNames);
        }
        entry.setProperty("group", "members", userNames);
        directorySession.updateEntry(entry);
        log.info("Group " + groupName + " updated successfully.");
    }

}
