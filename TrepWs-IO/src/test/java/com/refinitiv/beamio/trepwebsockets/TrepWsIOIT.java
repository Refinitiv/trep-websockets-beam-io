package com.refinitiv.beamio.trepwebsockets;

import java.util.Collections;

import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.Lists;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO.InstrumentTuple;

@RunWith(JUnit4.class)
public class TrepWsIOIT {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    private static final long loop = 50L;

    @Test
    public void testReadMessages() {

        PCollection<MarketPriceMessage> output =
            pipeline.apply(
                TrepWsIO.read()
                .withHostname("ads2")
                .withPort(5900)
                .withUsername("radmin")
                .withInstrumentTuples(
                    Lists.newArrayList(
                        InstrumentTuple.of(null, Lists.newArrayList("EUR="),Lists.newArrayList("PROD_PERM", "BID", "ASK")),
                        InstrumentTuple.of("IDN_SELECTFEED", Lists.newArrayList("0#ED:","0#.FTSE","0#.INDEX"), null)))
                .withCachedFields(Collections.singleton("PROD_PERM"))
                .withTimeout(60000)
                .withMaxNumRecords(loop));

          PAssert.thatSingleton(output.apply("Count", Count.globally())).isEqualTo(loop);
          pipeline.run();
      }
}
