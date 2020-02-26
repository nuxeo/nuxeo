/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Thierry Martins
 *     Florent Munch
 */
package org.nuxeo.ecm.core.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Schedule service implementation. Since the cleanup of the quartz job is done when service is activated, ( see see
 * https://jira.nuxeo.com/browse/NXP-7303 ) in cluster mode, the schedules contributions MUST be the same on all nodes.
 * Due the fact that all jobs are removed when service starts on a node it may be a short period with no schedules in
 * quartz table even other node is running.
 */
public class SchedulerServiceImpl extends DefaultComponent implements SchedulerService {

    private static final Log log = LogFactory.getLog(SchedulerServiceImpl.class);

    /** @since 11.1 */
    public static final String CLUSTER_START_DURATION_PROP = "org.nuxeo.scheduler.cluster.start.duration";

    /** @since 11.1 */
    public static final Duration CLUSTER_START_DURATION_DEFAULT = Duration.ofMinutes(1);

    protected RuntimeContext context;

    protected Scheduler scheduler;

    protected final ScheduleExtensionRegistry registry = new ScheduleExtensionRegistry();

    /**
     * @since 7.10
     */
    private Map<String, JobKey> jobKeys = new HashMap<>();

    @Override
    public void activate(ComponentContext context) {
        log.debug("Activate");
        this.context = context.getRuntimeContext();
    }

    protected void setupScheduler() throws IOException, SchedulerException {
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
        File file = new File(Environment.getDefault().getConfig(), "quartz.properties");
        if (file.exists()) {
            try (InputStream stream = new FileInputStream(file)) {
                schedulerFactory.initialize(stream);
            }
        } else {
            // use default config (unit tests)
            Properties props = new Properties();
            props.put("org.quartz.scheduler.instanceName", "Quartz");
            props.put("org.quartz.scheduler.threadName", "Quartz_Scheduler");
            props.put("org.quartz.scheduler.instanceId", "NON_CLUSTERED");
            props.put("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
            props.put("org.quartz.scheduler.skipUpdateCheck", "true");
            props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            props.put("org.quartz.threadPool.threadCount", "1");
            props.put("org.quartz.threadPool.threadPriority", "4");
            props.put("org.quartz.threadPool.makeThreadsDaemons", "true");
            schedulerFactory.initialize(props);
        }
        scheduler = schedulerFactory.getScheduler();
        scheduler.start();
        // server = MBeanServerFactory.createMBeanServer();
        // server.createMBean("org.quartz.ee.jmx.jboss.QuartzService",
        // quartzObjectName);

        // clean up all nuxeo jobs
        // https://jira.nuxeo.com/browse/NXP-7303
        GroupMatcher<JobKey> matcher = GroupMatcher.jobGroupEquals("nuxeo");
        Set<JobKey> jobs = scheduler.getJobKeys(matcher);
        try {
            scheduler.deleteJobs(new ArrayList<>(jobs)); // raise a lock error in case of concurrencies
            for (Schedule each : registry.getSchedules()) {
                registerSchedule(each);
            }
        } catch (LockException cause) {
            log.warn("scheduler already re-initializing, another cluster node concurrent startup ?", cause);
        }
        log.info("scheduler started");
    }

    protected void shutdownScheduler() {
        if (scheduler == null) {
            return;
        }
        try {
            scheduler.shutdown();
        } catch (SchedulerException cause) {
            log.error("Cannot shutdown scheduler", cause);
        } finally {
            scheduler = null;
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.debug("Deactivate");
        shutdownScheduler();
    }

    @Override
    public void start(ComponentContext context) {
        startScheduler();
    }

    protected void startScheduler() {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        String prop = Framework.getProperty(CLUSTER_START_DURATION_PROP);
        Duration duration = DurationUtils.parsePositive(prop, CLUSTER_START_DURATION_DEFAULT);
        Duration pollDelay = Duration.ofSeconds(1);
        clusterService.runAtomically("start-scheduler", duration, pollDelay, () -> {
            try {
                setupScheduler();
            } catch (IOException | SchedulerException e) {
                throw new NuxeoException(e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) {
        try {
            scheduler.standby();
        } catch (SchedulerException cause) {
            log.error("Cannot put scheduler in stand by mode", cause);
        }
    }

    @Override
    public boolean hasApplicationStarted() {
        return scheduler != null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            registerSchedule((Schedule) contrib);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        // do nothing to do ;
        // clean up will be done when service is activated
        // see https://jira.nuxeo.com/browse/NXP-7303
    }

    public RuntimeContext getContext() {
        return context;
    }

    @Override
    public void registerSchedule(Schedule schedule) {
        registerSchedule(schedule, null);
    }

    @Override
    public void registerSchedule(Schedule schedule, Map<String, Serializable> parameters) {
        registry.addContribution(schedule);
        if (scheduler == null) {
            return;
        }
        Schedule contributed = registry.getSchedule(schedule);
        if (contributed != null) {
            schedule(contributed, parameters);
        } else {
            unschedule(schedule.getId());
        }
    }

    protected void schedule(Schedule schedule, Map<String, Serializable> parameters) {
        log.info("Registering " + schedule);

        EventJobFactory jobFactory = schedule.getJobFactory();
        JobDetail job = jobFactory.buildJob(schedule, parameters).build();
        Trigger trigger = jobFactory.buildTrigger(schedule).build();

        // This is useful when testing to avoid multiple threads:
        // trigger = new SimpleTrigger(schedule.getId(), "nuxeo");

        try {
            try {
                schedule(schedule.getId(), job, trigger);
            } catch (ObjectAlreadyExistsException e) {
                // when jobs are persisted in a database, the job should already be there
                // remove existing job and re-schedule
                log.trace("Overriding scheduler with id: " + schedule.getId());
                boolean unregistered = unschedule(schedule.getId(), job.getKey());
                if (unregistered) {
                    schedule(schedule.getId(), job, trigger);
                }
            }
        } catch (SchedulerException e) {
            log.error(String.format("failed to schedule job with id '%s': %s", schedule.getId(), e.getMessage()), e);
        }
    }

    @Override
    public boolean unregisterSchedule(String id) {
        log.info("Unregistering schedule with id" + id);
        Schedule schedule = registry.getSchedule(id);
        if (schedule == null) {
            return false;
        }
        registry.removeContribution(schedule, true);
        return unschedule(id);
    }

    protected void schedule(String id, JobDetail job, Trigger trigger) throws SchedulerException {
        scheduler.scheduleJob(job, trigger);
        // record the jobKey only if scheduleJob didn't throw
        jobKeys.put(id, job.getKey());
    }

    protected boolean unschedule(String id) {
        return unschedule(id, jobKeys.remove(id));
    }

    protected boolean unschedule(String id, JobKey jobKey) {
        if (jobKey == null) {
            // shouldn't happen but let's be robust
            log.warn("Unscheduling null jobKey for id: " + id);
            return false;
        }
        try {
            return scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error(String.format("failed to unschedule job with '%s': %s", id, e.getMessage()), e);
            return true; // didn't exist so consider it removed (maybe concurrency)
        }
    }

    @Override
    public boolean unregisterSchedule(Schedule schedule) {
        return unregisterSchedule(schedule.getId());
    }

}
