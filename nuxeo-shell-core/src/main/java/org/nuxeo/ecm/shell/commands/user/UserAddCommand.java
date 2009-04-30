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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class UserAddCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "useradd";

    public static final int USERS_CSV_COLUMN_COUNT = 5;

    public static final String[] USERS_CSV_FIELD_NAMES = { "username",
            "firstName", "lastName", "email", "company" };

    public static final String USER_DIRECTORY_NAME = "userDirectory";

    private static final Log log = LogFactory.getLog(UserAddCommand.class);

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        // Parsing the command line
        String[] elements = cmdLine.getParameters();
        if (elements.length > 1) {
            log.error("Usage:\n" + COMMAND_NAME + " --file USERS_FILE.csv\n"
                    + COMMAND_NAME + " username");
            return;
        }

        // Let's parse all the command line before opening any connection to the
        // repository
        boolean importThroughCsv = false;
        File csvFile = null;
        Map<String, Object> userFields = new HashMap<String, Object>();
        String optionName = null;
        optionName = "file";
        if (cmdLine.isOptionSet(optionName)) {
            String csvFileName = cmdLine.getOption(optionName);
            log.debug("Reading user definitions of CSV file " + csvFileName);
            csvFile = new File(csvFileName);
            importThroughCsv = true;
        } else {
            String userName = elements[0];
            log.debug("Creating user " + userName + " ...");
            userFields.put("username", userName);
            optionName = "firstname";
            if (cmdLine.isOptionSet(optionName)) {
                userFields.put("firstName", cmdLine.getOption(optionName));
            }
            optionName = "lastname";
            if (cmdLine.isOptionSet(optionName)) {
                userFields.put("lastName", cmdLine.getOption(optionName));
            }
            optionName = "email";
            if (cmdLine.isOptionSet(optionName)) {
                userFields.put("email", cmdLine.getOption(optionName));
            }
            optionName = "company";
            if (cmdLine.isOptionSet(optionName)) {
                userFields.put("company", cmdLine.getOption(optionName));
            }
        }

        // Opening a connection to the repository
        RepositoryInstance repository = null;
        Session directorySession = null;
        try {
            repository = context.getRepositoryInstance();
            log.debug("Repository connection: " + repository);
            DirectoryService dirService = Framework.getService(DirectoryService.class);
            log.debug("Directory names: " + dirService.getDirectoryNames());
            directorySession = dirService.open(USER_DIRECTORY_NAME);
            if (importThroughCsv) {
                importCsvFile(csvFile, directorySession);
            } else {
                createUser(userFields, directorySession);
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

    private void importCsvFile(File file, Session directorySession)
            throws Exception {
        CSVReader csvReader = null;
        csvReader = new CSVReader(new FileReader(file));
        List<String[]> users = new ArrayList<String[]>();

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
                if (nextLine.length == USERS_CSV_COLUMN_COUNT) {
                    users.add(nextLine);
                } else {
                    log.error("The following CSV line will not be imported because it hasn't the requiread "
                            + USERS_CSV_COLUMN_COUNT
                            + " fields "
                            + Arrays.asList(USERS_CSV_FIELD_NAMES)
                            + "\n"
                            + Arrays.asList(nextLine));
                }
            }
            firstLine = false;
        }
        csvReader.close();
        for (String[] user : users) {
            Map<String, Object> userFields = new HashMap<String, Object>();
            userFields.put("username", user[0]);
            userFields.put("firstName", user[1]);
            userFields.put("lastName", user[2]);
            userFields.put("email", user[3]);
            userFields.put("company", user[4]);
            createUser(userFields, directorySession);
        }
    }

    private void createUser(Map<String, Object> userFields,
            Session directorySession) throws Exception {
        directorySession.createEntry(userFields);
        log.info("User " + userFields.get("username")
                + " created successfully.");
    }

}
