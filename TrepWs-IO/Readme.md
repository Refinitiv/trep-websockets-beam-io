
# Reading from a websocket TREP/ADS

TrepWsIO source returns unbounded collection of
`<MarketPriceMessage>` messages. A MarketPriceMessage
includes:
* a `long` **id** (the request ID)
* a `String` **type**(Refresh, Update, etc.)
* a `String` **updateType** (Trade, Quote, etc.)
* a `long` **seqNumber** (the backbone sequence number of each message)
* a `String` **name** (the RIC name)
* a `String` **service** (the ADS service name)
* a `Map<String, String>` of **fields** (e.g. ASK=1.1512,BID=1.151, etc.)
* an `Instant`> **timestamp** (the message receive time)
* the `String` raw **jsonString** received from the ADS

WebSocket is a standard communication protocol built on top of TCP connection. The WebSocket protocol enables streams of messages between the ADS and this Beam Source.

The wire protocol used over the WebSocket connection is Thomson Reuters Simplified JSON, which is also referred as WebSocket API. The WebSocket API is an abstract interface between ADS and WebSocket clients communicating with the simplified JSON messages.

Currently, the ADS supports the size of the WebSocket frame up to 60k bytes. If the ADS receives any JSON message larger than that, the WebSocket channel will be disconnected.

This source will split into *n* parts which is the minimum of desiredNumSplits and the getMaxMounts parameter. Note the default getMaxMounts is 1. The watchlist sent to each ADS mount is derived by extracting each RIC from the InstrumentTuples and distributing them in round robin order.

This source does not process checkpoint marks, but does record sequence numbers and timestamps for watermarking.

The TrepWsIO now supports connections to both an ADS and the Elektron Real-Time Service both using the Websocket API.

To configure a TREP Websocket source, you must specify at the minimum hostname, username and a list of instrument tuples.

If connecting to ERT then withTokenAuth(true) and a password must also be specified. Additionally if withServiceDiscovery(true) and .withRegion("eu") are set the TrepWsIO will discover and connect to services in that region.



For example:

    // Firstly create any number instrument tuples of Service, RIC list and Field list
    //   Service can be null, in which case the default ADS Service will be used
    //   RIC list cannot be null
    //   Fields can be null, in which case field filtering (a View) will not apply

    InstrumentTuple instrument1 = InstrumentTuple.of(
      "IDN_RDF",
      Lists.newArrayList("EUR=","JPY="),
      Lists.newArrayList("BID","ASK"));

    InstrumentTuple instrument2 = InstrumentTuple.of(
      null,
      Lists.newArrayList("MSFT.O","IBM.N"),
      null);

    PCollection<MarketPriceMessage> messages =
      pipeline.apply(TrepWsIO.read()
       .withHostname("websocket-ads") // hostname
       .withUsername("user")          // username

       // For ERT
       .withTokenAuth(true)
       .withPassword("ert-password")
       .withServiceDiscovery(true)
       .withRegion("eu")

       // Token store location used to support multiple ERT mounts. This can be the Dataflow Temp location.
       .withTokenStore("gs://gcs-token-store")

       .withInstrumentTuples(Lists.newArrayList(instrument1, instrument2))

       // Rest of the settings are optional:

       // Cache these fields values and populate in every update
       .withCachedFields(Sets.newHashSet("PROD_PERM","CONTR_MNTH"))

       // ADS websocket port, if unset 15000 is used
       .withPort(15000)

       // The maximum number of ADS mounts (overridden if the number of
       // desired splits is smaller). If unset then 1 is used.
       // NOTE: for ERT maxMounts is forced to 1 to avoid clashes then performing token authentication.
       .withMaxMounts(1)

       // The DACS position, if unset the IP address of the local host is used
       .withPosition("192.168.1.104")

       // The DACS application ID, if unset 256 is used
       .withAppId("123")

       // The wesocket MaxSessionIdleTimeout in milliSeconds.
       // Note: this must be greater that 6000 (twice the ping timeout)
       .withTimeout(60000)

       // For ERT to override the defaults for authentication server/port/path and discovery path
      .withAuthServer("api.edp.thomsonreuters.com")
      .withAuthPort(443)
      .withTokenAuthPath("/auth/oauth2/beta1/token")
      .withDiscoveryPath("/streaming/pricing/v1/")

      );

      messages.apply(....) // PCollection<MarketPriceMessage>
    }
