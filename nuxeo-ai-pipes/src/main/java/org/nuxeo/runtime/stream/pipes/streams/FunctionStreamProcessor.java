/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.runtime.stream.pipes.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.metrics.NuxeoMetricSet;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * A stream processor using a Function.
 */
public class FunctionStreamProcessor {

    public static final String STREAM_IN = "source";

    public static final String STREAM_OUT = "sink";

    private static final Log log = LogFactory.getLog(FunctionStreamProcessor.class);

    /**
     * A list of streams for this stream processor
     */
    public static List<String> getStreamsList(String streamIn, String streamOut) {
        List<String> streams = new ArrayList<>(1);
        if (StringUtils.isBlank(streamIn)) {
            throw new IllegalArgumentException("You must define a streamIn stream");
        }
        streams.add("i1:" + streamIn);
        if (StringUtils.isNotBlank(streamOut)) {
            String[] outStreams = streamOut.split(",");
            int i = 1;
            for (String outStream : outStreams) {
                streams.add("o" + i++ + ":" + outStream);
            }
        }
        return streams;
    }

    public static String buildName(String simpleName, String streamIn, String streamOut) {
        String in = streamIn != null ? streamIn + "$" : "";
        String out = streamOut != null ? "$" + streamOut : "";
        return in + simpleName + out;
    }

    public Topology getTopology(Function<Record, Optional<Record>> function, Map<String, String> options) {
        String streamIn = options.get(STREAM_IN);
        String streamOut = options.get(STREAM_OUT);
        List<String> streams = getStreamsList(streamIn, streamOut);
        String computationName = buildName(function.getClass().getSimpleName(), streamIn, streamOut);
        FunctionMetrics metrics = registerMetrics(new FunctionMetrics(computationName), computationName);
        return Topology.builder()
                       .addComputation(
                               () -> new FunctionComputation(streams.size() - 1,
                                                             computationName, metrics, function), streams)
                       .build();
    }

    /**
     * Register metrics for this computation
     */
    public static <T extends NuxeoMetricSet> T registerMetrics(T metrics, String name) {
        try {
            MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
            registry.registerAll(metrics);
        } catch (IllegalArgumentException e) {
            log.warn(
                    String.format("Metrics are already registered for %s. They will only be recorded again after a full restart.",
                                  name));
        }
        return metrics;
    }

    /**
     * A Computation that uses a Java Function to transform the Record.
     */
    public static class FunctionComputation extends AbstractComputation {

        protected final FunctionMetrics metrics;

        protected final Function<Record, Optional<Record>> function;

        public FunctionComputation(int outputStreams, String name,
                                   FunctionMetrics metrics, Function<Record, Optional<Record>> function) {

            super(name, 1, outputStreams);
            this.metrics = metrics;
            this.function = function;
        }

        @Override
        public void init(ComputationContext context) {
            log.debug(String.format("Starting computation for %s", metadata.name()));
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            metrics.called();
            if (log.isDebugEnabled()) {
                log.debug("Processing record " + record);
            }
            try {
                Optional<Record> applied = function.apply(record);
                applied.ifPresent(rec -> writeToStreams(context, rec));
                context.askForCheckpoint();
            } catch (NuxeoException e) {
                log.error("Discard invalid record: " + record, e);
                metrics.error();
            }
        }

        @Override
        public void destroy() {
            log.debug(String.format("Destroy computation: %s", metadata.name()));
        }

        /**
         * Writes to the output streams.  Performs no action if no Record or output streams.
         */
        protected void writeToStreams(ComputationContext context, Record record) {
            if (record != null && !metadata.outputStreams().isEmpty()) {
                metrics.produced();
                metadata.outputStreams().forEach(o -> context.produceRecord(o, record));
            }
        }
    }

    /**
     * Metrics about the function
     */
    public static class FunctionMetrics extends NuxeoMetricSet {

        protected long called = 0;

        protected long errors = 0;

        protected long produced = 0;

        public FunctionMetrics(String name) {
            super("nuxeo", "streams", "func", name);
            this.putGauge(() -> called, "called");
            this.putGauge(() -> errors, "errors");
            this.putGauge(() -> produced, "produced");
        }

        /**
         * Increment called
         */
        public void called() {
            called++;
        }

        /**
         * Increment errors
         */
        public void error() {
            errors++;
        }

        /**
         * Increment produced
         */
        public void produced() {
            produced++;
        }
    }

}