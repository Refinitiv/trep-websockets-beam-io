package com.refinitiv.beamio.trepwebsockets;

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
    
    private static final long loop = 6L;

    @Test
    public void testReadMessages() {
       
        PCollection<MarketPriceMessage> output = 
            pipeline.apply(
                TrepWsIO.read()
                .withHostname("ads1")
                .withPort(5900)
                .withUsername("radmin")
                .withInstrumentTuples(
                    Lists.newArrayList(
                        InstrumentTuple.of("IDN_SELECTFEED", Lists.newArrayList("JUNK", "EUR=", "JPY="),Lists.newArrayList("PROD_PERM", "BID", "ASK")),
                        InstrumentTuple.of("IDN_SELECTFEED", Lists.newArrayList("LGOc1","CMCU3","VOD.L",".TASI"), null)))
                .withTimeout(60000)
                .withMaxNumRecords(loop));

          PAssert.thatSingleton(output.apply("Count", Count.globally())).isEqualTo(loop);
          pipeline.run();
      }
}
