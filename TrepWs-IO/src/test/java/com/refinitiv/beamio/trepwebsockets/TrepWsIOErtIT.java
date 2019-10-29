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
public class TrepWsIOErtIT {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    private static final long loop = Long.MAX_VALUE-1;

    @Test
    public void testReadMessages() {

        PCollection<MarketPriceMessage> output =
            pipeline.apply(
                TrepWsIO.read()
                .withRegion("eu")
                .withTokenAuth(true)
                .withTokenStore("gs://tr-solutions-playground-temp")
                .withServiceDiscovery(true)
                .withHostname("amer-3.pricing.streaming.edp.thomsonreuters.com")
                .withPort(443)
                .withUsername("GE-A-01103867-3-532")
                .withPassword("FEfEMWT{$DvM6MUavwgBwATxhfgQhA")
                .withMaxMounts(5)
                .withInstrumentTuples(
                    Lists.newArrayList(
                        InstrumentTuple.of("ELEKTRON_DD",
                        Lists.newArrayList("EUR=","JPY=","NOK=",".FTSE","LCOc1","LCOc2","LCOc3","LCOc4","LCOc5","LCOc6","LCOc7"),
                        Lists.newArrayList("PROD_PERM", "BID", "ASK")) ))
                .withCachedFields(Collections.singleton("PROD_PERM"))
                .withTimeout(60000)
                .withMaxNumRecords(loop));

          PAssert.thatSingleton(output.apply("Count", Count.globally())).isEqualTo(loop);
          pipeline.run();
      }
}
