/*
 * Copyright Refinitiv 2018
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
 */
package com.refinitiv.beamio.trepwebsockets;

import org.apache.commons.lang3.RandomStringUtils;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO.InstrumentTuple;

@RunWith(JUnit4.class)
public class TrepWsIOTest {

	@Rule
	public final transient TestPipeline pipeline = TestPipeline.create();

	private void runPipelineExpectingException(String innerMessage) {
	    try {
	        pipeline.run();
	        fail();
	    } catch (Exception e) {
	        assertThat(Throwables.getRootCause(e).getMessage(), containsString(innerMessage));
	    }
	}

    @Test
	public void testConnectionRefused() {

        pipeline.apply(TrepWsIO.read()
                .withHostname("localhost")
                .withPort(1)
                .withUsername("nobody")
                .withInstrumentTuples(Lists.newArrayList(
                        InstrumentTuple.of(null, Lists.newArrayList("RIC"), null))));

        runPipelineExpectingException("Connection refused");
	}

    @Test(expected=IllegalArgumentException.class)
    public void testRequiredParameters() {

        pipeline.apply(TrepWsIO.read().withInstrumentTuples(null));
        pipeline.run();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSplit() throws Exception {

        List<InstrumentTuple> tuples = Lists.newArrayList();
        for (int i = 0; i < 2; i++) {
            List<String> rics = Lists.newArrayList();
            for (int ii = 0; ii < 3; ii++) {
                rics.add(RandomStringUtils.randomAlphabetic(10));
            }
            tuples.add(InstrumentTuple.of(null,rics,null));
        }

        TrepWsIO.Read read = TrepWsIO.read()
                .withHostname("localhost")
                .withMaxMounts(10)
                .withInstrumentTuples(tuples);
        PipelineOptions pipelineOptions = PipelineOptionsFactory.create();
        int desiredNumSplits = 12;
        AtomicReference<String> ref = new AtomicReference<String>();
        ref.set("hello");
        TrepWsIO.UnboundedTrepWsSource initialSource = new TrepWsIO.UnboundedTrepWsSource(read, 1);
        List<TrepWsIO.UnboundedTrepWsSource> splits = initialSource.split(desiredNumSplits, pipelineOptions);

        assertEquals(6, splits.size());
    }

}
