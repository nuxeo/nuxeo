/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tools.command;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * Output information of Work in a StreamWorkManager Queue.
 *
 * @since 11.5
 */
public class WorkCatCommand extends Command {
    private static final Log log = LogFactory.getLog(WorkCatCommand.class);

    protected static final String NAME = "workCat";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("n").desc("Limit to the first N Works").hasArg().argName("N").build());
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Work Queue Log name")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("codec")
                                .desc("Codec used to read record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        int limit = Integer.parseInt(cmd.getOptionValue("n", "-1"));
        String name = cmd.getOptionValue("log-name");
        String group = cmd.getOptionValue("group", "tools");
        String codec = cmd.getOptionValue("codec");
        workStat(manager, name, group, limit, codec);
        return true;
    }

    protected void workStat(LogManager manager, String name, String group, int limit, String codec)
            throws InterruptedException {

        try (LogTailer<Record> tailer = manager.createTailer(group, name, getRecordCodec(codec))) {
            int count = 0;
            log.info("pos,class,fullname,category,name");
            do {
                LogRecord<Record> record = tailer.read(Duration.ofMillis(1000));
                if (record == null) {
                    break;
                }
                count++;
                log.info(record.offset().toString() + "," + deserialize(record.message().getData()));
            } while (limit < 0 || (count < limit));
        }

    }

    public static String deserialize(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = null;
        String ret = "";
        try {
            in = new ObjectInputStream(bis);
            Object work = in.readObject();
            String clazz = work.getClass().getSimpleName();
            ret += clazz;
            ret += "," + work.getClass().getCanonicalName();
            Method getCategory = work.getClass().getMethod("getCategory");
            String category = (String) getCategory.invoke(work);
            ret += "," + category;
            ret += "," + work.toString().replace(",", ".");
            return ret;
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | IllegalStateException e) {
            return e.getClass().getSimpleName() + "," + e.getClass().getCanonicalName() + ",ERROR,NA";
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception so we cannot use a try-with-resources squid:S2093
            }
        }
    }

}
