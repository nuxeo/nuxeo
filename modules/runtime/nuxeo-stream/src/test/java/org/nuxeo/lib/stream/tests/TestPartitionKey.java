/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.tests;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.dropwizard.metrics5.ExponentiallyDecayingReservoir;
import io.dropwizard.metrics5.Histogram;

/**
 * Test partitioning key strategy and simulate the distribution and duration of a processing. The goal is to have an
 * even distribution in order to limit the long tail processing, see NXP-33355 for more information.
 **/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPartitionKey {

    private static final Logger log = LogManager.getLogger(TestPartitionKey.class);

    // If you know the exact key use it
    protected String commandId = UUID.randomUUID().toString();

    // Choose your partition key implementation
    PartitionKeyMethod partitionKeyMethod = this::getBestPartitionKey;

    // Monitoring provides record latency metric, that can be turned to document processing duration
    protected double durationPerDocument = 15.5 / 250;

    protected boolean forceOutput = false;

    protected final Random random = new Random(new Date().getTime());

    // Same hashing used by LogAppender to select the partition
    private int getPartition(String key, int numberOfPartitions) {
        return (key.hashCode() & 0x7fffffff) % numberOfPartitions;
    }

    // Below different partition keys that could be used by the scroller

    // Legacy partition key until 2023.37 with poor distribution on high partition number
    private String getLegacyPartitionKey(int i) {
        return commandId + ":" + i;
    }

    // Best shard key proposal
    private String getBestPartitionKey(int i) {
        return i + ":" + commandId.substring(0, 8);
    }

    // With fixed size, much slower to compute x10
    private String getFixedSizePartitionKey(int i) {
        return "%08d:%s".formatted(i, commandId.substring(0, 8));
    }

    // A random long, very bad distribution
    private String getRandomPartitionKey(int i) {
        return String.valueOf(random.nextLong());
    }

    @FunctionalInterface
    interface PartitionKeyMethod {
        String execute(int a);
    }

    // Test different number of partitions, note that we choose only highly composite number i.e. 2, 4, 6, 12, 24,
    // 36, 48, 60, 120, 180, 240, 360
    @Test
    public void testDistribution002() {
        testDistribution(2);
    }

    @Test
    public void testDistribution004() {
        testDistribution(4);
    }

    @Test
    public void testDistribution006() {
        testDistribution(6);
    }

    @Test
    public void testDistribution012() {
        testDistribution(12);
    }

    @Test
    public void testDistribution024() {
        testDistribution(12);
    }

    @Test
    public void testDistribution048() {
        testDistribution(48);
    }

    @Test
    public void testDistribution060() {
        testDistribution(60);
    }

    @Test
    public void testDistribution120() {
        testDistribution(120);
    }

    @Test
    public void testDistribution180() {
        testDistribution(180);
    }

    @Test
    public void testDistribution240() {
        testDistribution(240);
    }

    @Test
    public void testDistribution360() {
        testDistribution(360);
    }

    @Test
    public void testDistributionCustom() {
        var oldKey = commandId;
        try {
            forceOutput = true;
            // simulate a true processing on 2023.36 distribution 1.8g docs on 240 partitions
            commandId = "b4579dd5-24e3-4078-9f31-c423377cdea0";
            partitionKeyMethod = this::getLegacyPartitionKey; // duration = 129.17h + 35.44h (tail 27.44%)
            testDistribution(1_800_000_000, 250, 240);

            partitionKeyMethod = this::getBestPartitionKey; // duration = 129.17h + 2.07h (tail 1.60%)
            testDistribution(1_800_000_000, 250, 240);
        } finally {
            forceOutput = false;
            commandId = oldKey;
        }

    }

    public void testDistribution(int partitions) {
        testDistribution(10_000, 100, partitions);
        testDistribution(100_000, 100, partitions);
        testDistribution(1_000_000, 100, partitions);
        testDistribution(10_000_000, 100, partitions);
        testDistribution(100_000_000, 100, partitions);
    }

    public void testDistribution(int numberOfDocuments, int numberOfDocumentPerRecord, int numberOfPartitions) {
        int numberOfRecords = numberOfDocuments / numberOfDocumentPerRecord;
        double durationPerRecord = durationPerDocument * numberOfDocumentPerRecord;

        Histogram histogram = new Histogram(new ExponentiallyDecayingReservoir());

        int[] partitions = new int[numberOfPartitions];
        for (int i = 0; i < numberOfRecords; i++) {
            partitions[getPartition(partitionKeyMethod.execute(i), numberOfPartitions)]++;
        }
        for (int i = 0; i < numberOfPartitions; i++) {
            histogram.update(partitions[i]);
        }
        long min = histogram.getSnapshot().getMin();
        long max = histogram.getSnapshot().getMax();
        double mean = histogram.getSnapshot().getMean();
        double median = histogram.getSnapshot().getMedian();
        double stdev = histogram.getSnapshot().getStdDev();

        double bestDuration = durationPerRecord * numberOfRecords / numberOfPartitions;
        double currentDuration = durationPerRecord * max;
        double diff = currentDuration - bestDuration;
        double tailPercent = diff * 100.0 / bestDuration;
        // filter output to only tail processing that takes more than 5% and more than 5min
        if (forceOutput || (diff > 310 && tailPercent > 5.0)) {
            log.warn(
                    "%d distributed in %d records among %d partitions: min: %d, p50: %.2f, max: %d, mean: %.2f +/- %.2f duration = %.2fh + %.2fh (tail %.2f%%)".formatted(
                            numberOfDocuments, numberOfRecords, numberOfPartitions, min, median, max, mean, stdev,
                            bestDuration / 3600, diff / 3600.0, tailPercent));
            log.warn(
                    "Processing duration for one doc: %.2fs, one record of %d docs: %.2fs, all records: %.2fh".formatted(
                            durationPerDocument, numberOfDocumentPerRecord, durationPerRecord,
                            durationPerRecord * numberOfRecords / 3600.0));

            log.warn("Best duration with even distribution: %.2fh %.2frec/s %.2fdocs/s".formatted(bestDuration / 3600.0,
                    numberOfRecords / bestDuration, numberOfDocuments / bestDuration));
            log.warn("Current duration: %.2fh %.2frec/s %.2fdocs/s".formatted(currentDuration / 3600.0,
                    numberOfRecords / currentDuration, numberOfDocuments / currentDuration));
            log.warn("First consumer starving after: %.2fh".formatted(durationPerRecord * min / 3600.0));
        }
    }
}
