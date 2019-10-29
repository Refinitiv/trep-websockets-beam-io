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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.nullToEmpty;
import static com.refinitiv.beamio.trepwebsockets.TrepWsListener.CLOSE;
import static com.refinitiv.beamio.trepwebsockets.TrepWsListener.sendLoginRequest;
import static com.refinitiv.beamio.trepwebsockets.TrepWsListener.sendRequest;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.CLOSED;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.ERROR;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.LOGIN;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.STATUS;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.right;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.Read.Unbounded;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.io.UnboundedSource.CheckpointMark;
import org.apache.beam.sdk.io.UnboundedSource.UnboundedReader;
import org.apache.beam.sdk.io.fs.MoveOptions;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.apache.beam.sdk.util.MimeTypes;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.joda.time.Instant;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.refinitiv.beamio.trepwebsockets.json.MarketPrice;
import com.refinitiv.beamio.trepwebsockets.json.Services;

@SuppressWarnings("serial")
@Experimental(Experimental.Kind.SOURCE_SINK)
public class TrepWsIO {

    private static final Logger LOG = LoggerFactory.getLogger(TrepWsIO.class);
    private static final String FIELDS = "Fields";

    private static Counter      messages  = Metrics.counter(TrepWsIO.class, "Messages");
    private static Counter      records   = Metrics.counter(TrepWsIO.class, "Records");
    private static Counter      status    = Metrics.counter(TrepWsIO.class, "Status Messages");
    private static Distribution queueSize = Metrics.distribution(TrepWsIO.class, "Queue ");

      /**
     * <h3>Reading from a websocket TREP/ADS</h3>
     *
     * <p>
     * TrepWsIO source returns unbounded collection of
     * {@code PCollection<MarketPriceMessage>} messages. A MarketPriceMessage
     * includes:
     * <ul>
     * <li>a <code>long</code> <b>id</b> (the request ID)</li>
     * <li>a <code>String</code> <b>type</b> (Refresh, Update, etc.)</li>
     * <li>a <code>String</code> <b>updateType</b> (Trade, Quote, etc.)</li>
     * <li>a <code>long</code> <b>seqNumber</b> (the backbone sequence number of
     * each message)</li>
     * <li>a <code>String</code> <b>name</b> (the RIC name)</li>
     * <li>a <code>String</code> <b>service</b> (the ADS service name)</li>
     * <li>a {@code Map<String, String>} of <b>fields</b> (e.g. ASK=1.1512,
     * BID=1.151, etc.)</li>
     * <li>an <code>Instant</code> <b>timestamp</b> (the message receive time)</li>
     * <li>the <code>String</code> raw <b>jsonString</b> received from the ADS</li>
     * </ul>
     *
     * <p>
     * WebSocket is a standard communication protocol built on top of TCP
     * connection. The WebSocket protocol enables streams of messages between the
     * ADS and this Beam Source.
     *
     * <p>
     * The wire protocol used over the WebSocket connection is Thomson Reuters
     * Simplified JSON, which is also referred as WebSocket API. The WebSocket API
     * is an abstract interface between ADS and WebSocket clients communicating with
     * the simplified JSON messages.
     *
     * <p>
     * Currently, the ADS supports the size of the WebSocket frame up to 60k bytes.
     * If the ADS receives any JSON message larger than that, the WebSocket channel
     * will be disconnected.
     *
     * <p>
     * This source will split into <i>n</i> parts which is the minimum of
     * desiredNumSplits and the getMaxMounts parameter. The desiredNumSplits is
     * derived by the Dataflow runner from the WorkerMachineType and the NumWorkers
     * pipeline options. Note the default getMaxMounts is 1. The watchlist sent to
     * each ADS mount is derived by extracting each RIC from the InstrumentTuples
     * and distributing them in round robin order.
     * <p>
     * This source does not process checkpoint marks, but does record sequence
     * numbers and timestamps for watermarking.
     *
     * <p>
     * The TrepWsIO now supports connections to both an ADS and the Elektron
     * Real-Time Service both using the Websocket API.
     * <p>
     * To configure a TREP Websocket source, you must specify at the minimum
     * hostname, username and a list of instrument tuples.
     * <p>
     * If connecting to ERT then withTokenAuth(true) and a password must also be
     * specified. Additionally if withServiceDiscovery(true) and .withRegion("eu")
     * are set the TrepWsIO will discover and connect to services in that region.
     *
     * <pre>
     * {@code
     *
     * // Firstly create any number instrument tuples of Service, RIC list and Field list
     * // Service can be null, in which case the default ADS Service will be used
     * // RIC list cannot be null
     * // Fields can be null, in which case field filtering (a View) will not apply
     *
     *     InstrumentTuple instrument1 = InstrumentTuple.of(
     *       "IDN_RDF",
     *       Lists.newArrayList("EUR=","JPY="),
     *       Lists.newArrayList("BID","ASK"));

     *     InstrumentTuple instrument2 = InstrumentTuple.of(
     *       null,
     *       Lists.newArrayList("MSFT.O","IBM.N"),
     *       null);
     *
     *     PCollection<MarketPriceMessage> messages =
     *       pipeline.apply(TrepWsIO.read()
     *        .withHostname("websocket-ads") // hostname
     *        .withUsername("user")          // username
     *
     *        // For ERT
     *        .withTokenAuth(true)
     *        .withPassword("ert-password")
     *        .withServiceDiscovery(true)
     *        .withRegion("eu")
     *
     *        // Token store location used to support multiple ERT mounts. This can be
     *        the Dataflow Temp location
     *        .withTokenStore("gs://gcs-token-store")
     *
     *        .withInstrumentTuples(Lists.newArrayList(instrument1, instrument2))
     *
     *        // Rest of the settings are optional:
     *
     *        // Cache these fields values and populate in every update
     *        .withCachedFields(Sets.newHashSet("PROD_PERM","CONTR_MNTH"))
     *
     *        // ADS websocket port, if unset 15000 is used
     *        .withPort(15000)
     *
     *        // The maximum number of ADS mounts (overridden if the number of
     *        // desired splits is smaller). If unset then 1 is used.
     *        .withMaxMounts(1)
     *
     *        // The DACS position, if unset the IP address of the local host is used
     *        .withPosition("192.168.1.104")
     *
     *        // The DACS application ID, if unset 256 is used
     *       .withAppId("123")
     *
     *        // The websocket MaxSessionIdleTimeout in milliSeconds.
     *        // Note: this must be greater that 6000 (twice the ping timeout)
     *        .withTimeout(60000)
     *
     *        // For ERT to override the defaults for authentication server/port/path and discovery path
     *       .withAuthServer("api.edp.thomsonreuters.com")
     *       .withAuthPort(443)
     *       .withTokenAuthPath("/auth/oauth2/beta1/token")
     *       .withDiscoveryPath("/streaming/pricing/v1/")
     *
     *       );
     *
     *       messages.apply(....) // PCollection<MarketPriceMessage>
     *     }
     * </pre>
     */
    public static Read<MarketPriceMessage> read() {

        return new AutoValue_TrepWsIO_Read.Builder<MarketPriceMessage>()
                .setMaxNumRecords(Long.MAX_VALUE)
                .setPort(15000)
                .setTokenAuth(false)
                .setServiceDiscovery(false)
                .setRegion("us")
                .setAuthServer("api.edp.thomsonreuters.com")
                .setAuthPort(443)
                .setTokenAuthPath("/auth/oauth2/beta1/token")
                .setDiscoveryPath("/streaming/pricing/v1/")
                .setAppId("256")
                .setTimeout(60000)
                .setMaxMounts(1)
                .setCoder(SerializableCoder.of(MarketPriceMessage.class))
                .setMessageMapper(
                        (MessageMapper<MarketPriceMessage>)
                        new MessageMapper<MarketPriceMessage>() {

                            @Override
                            public MarketPriceMessage mapMessage(ConcurrentHashMap<String, LoadingCache<String, String>> cache,
                                    MarketPrice message,
                                    Set<String> cachedFields) {

                                String jsonString = StringUtils.defaultIfBlank(jsonString = message.getJsonString(),"{}");

                                Map<String,String> fields = cacheAdd(cache, message.getKey().getName(), message.getFields(), cachedFields);

                                try {
                                    if (cachedFields != null && !cachedFields.isEmpty()) {
                                        jsonString = new JSONObject(message.getJsonString()).put(FIELDS, fields).toString();
                                    }
                                } catch (Exception e) {
                                    LOG.error("ERROR: updating json string {}",
                                        String.format("msg:%s json:%s fields:%s", message.getJsonString(),
                                            jsonString, fields),e);
                                }

                                MarketPriceMessage mp =  new MarketPriceMessage(
                                        message.getID(),
                                        message.getType(),
                                        message.getUpdateType(),
                                        message.getSeqNumber(),
                                        message.getKey().getName(),
                                        message.getKey().getService(),
                                        fields,
                                        jsonString,
                                        message.getTimestamp());
                                return mp;
                            }

                            private Map<String, String> cacheAdd(
                                    ConcurrentHashMap<String, LoadingCache<String, String>> cache, String ric,
                                    Map<String, String> fields, Set<String> cachedFields) {

                                if (cachedFields == null || cachedFields.isEmpty())
                                    return fields;

                                Map<String, String> fieldz = Maps.newLinkedHashMap(fields);

                                if (!cache.containsKey(ric)) {
                                    cache.put(ric, initCache());
                                }

                                fields.entrySet().stream().filter(e -> cachedFields.contains(e.getKey()))
                                        .filter(e -> e.getValue() != null)
                                        .forEach(f -> cache.get(ric).put(f.getKey(), f.getValue()));

                                fieldz.putAll(cache.get(ric).asMap());

                                return fieldz;
                            }

                            private LoadingCache<String, String> initCache() {
                                return CacheBuilder.newBuilder().build(new CacheLoader<String, String>() {
                                    @Override
                                    public String load(String key) throws Exception {
                                        return key;
                                    }
                                });
                            }
                        })
                .build();
    }

      /**
     * <p>An Instrument Tuple of Service, Instrument list and Field list.
     * <ul>
     * <li>Service can be null, in which case the default ADS Service will be used
     * <li>RIC list cannot be null
     * <li>Fields can be null, in which case field filtering (a View) will not apply
     * <li>PE can be null
     * <br><br>
     *
     * @param service
     * @param instruments
     * @param fields
     * @return InstrumentTuple
     */
    @AutoValue
    public abstract static class InstrumentTuple implements Serializable {
    public static InstrumentTuple of(String service, List<String> instruments, List<String> fields) {
        return new AutoValue_TrepWsIO_InstrumentTuple(service, instruments, fields);
    }

    /** TREP Service Name */
    @Nullable
    public abstract String getService();

    /** TREP Instrument List
     * <p>Cannot be null
     */
    public abstract List<String> getInstruments();

    /** TREP Field List */
    @Nullable
    public abstract List<String> getFields();

    }

    @AutoValue
    public abstract static class Read<T> extends PTransform<PBegin, PCollection<T>> {

        @Nullable
        abstract String getHostname();

        abstract int getPort();

        abstract boolean getTokenAuth();

        @Nullable
        abstract String getRegion();

        @Nullable
        abstract String getAuthServer();

        abstract int getAuthPort();

        abstract boolean getServiceDiscovery();

        @Nullable
        abstract String getTokenAuthPath();

        @Nullable
        abstract String getDiscoveryPath();

        @Nullable
        abstract String getUsername();

        @Nullable
        abstract String getPassword();

        @Nullable
        abstract String getPosition();

        @Nullable
        abstract String getAppId();

        @Nullable
        abstract List<InstrumentTuple> getInstrumentTuples();

        abstract long getMaxNumRecords();

        abstract int  getTimeout();

        abstract int  getMaxMounts();

        @Nullable
        abstract MessageMapper<T> getMessageMapper();

        @Nullable
        abstract Coder<T> getCoder();

        @Nullable
        abstract Set<String> getCachedFields();

        @Nullable
        abstract String getTokenStore();

        abstract Builder<T> builder();

        @AutoValue.Builder
        abstract static class Builder<T> {

            abstract Builder<T> setHostname(String hostname);

            abstract Builder<T> setPort(int port);

            abstract Builder<T> setTokenAuth(boolean token);

            abstract Builder<T> setRegion(String region);

            abstract Builder<T> setAuthServer(String authServer);

            abstract Builder<T> setAuthPort(int authPort);

            abstract Builder<T> setServiceDiscovery(boolean discovery);

            abstract Builder<T> setTokenAuthPath(String authPath);

            abstract Builder<T> setDiscoveryPath(String discoveryPath);

            abstract Builder<T> setUsername(String username);

            abstract Builder<T> setPassword(String password);

            abstract Builder<T> setPosition(String position);

            abstract Builder<T> setAppId(String appId);

            abstract Builder<T> setMaxNumRecords(long maxNumRecords);

            abstract Builder<T> setTimeout(int timeout);

            abstract Builder<T> setMaxMounts(int maxMounts);

            abstract Builder<T> setMessageMapper(MessageMapper<T> messageMapper);

            abstract Builder<T> setCoder(Coder<T> coder);

            abstract Builder<T> setInstrumentTuples(List<InstrumentTuple> instrumentTuples);

            abstract Builder<T> setCachedFields(Set<String> cachedFields);

            abstract Builder<T> setTokenStore(String tokenStoreLocation);

            abstract Read<T> build();
        }

        /**
         * <p>Specifies a java.util.List of InstrumentTuples to request from the ADS.
         * <p>A tuple is made up of a Service name, Instrument (RIC) list and Field list.
         * <ul><li>If the Service name is null, the default ADS Service name will be used.
         * <li>The Instrument list cannot be null.<br>
         * <li>If the Field list is null, Field filtering will not apply.
         * <br><br>
         * @param instrumentTuples
         */
        public Read<T> withInstrumentTuples(List<InstrumentTuple> instrumentTuples) {
            checkArgument(instrumentTuples != null, "instrumentTuples cannot be null");
            checkArgument(!instrumentTuples.isEmpty(), "instrumentTuples list cannot be empty");
            return builder().setInstrumentTuples(instrumentTuples).build();
        }

        /**
         * Specifies the hostname where the ADS is running.
         * @param hostname
         */
        public Read<T> withHostname(String hostname) {
            return builder().setHostname(hostname).build();
        }

        /**
         * Specifies the region selected when performing ERT service discovery.
         * @param region
         * @return
         */
        public Read<T> withRegion(String region) {
            checkArgument(region != null, "region cannot be null");
            return builder().setRegion(region).build();
        }

        /**
         * Specifies the port number on which the ADS is listening for Websocket connections.
         * @param port
         */
        public Read<T> withPort(int port) {
            return builder().setPort(port).build();
        }

        /**
         * Specifies whether to use ERT token authentication.
         * @param tokenAuth
         * @return
         */
        public Read<T> withTokenAuth(boolean tokenAuth) {
            return builder().setTokenAuth(tokenAuth).build();
        }

        /**
         * Specifies the ERT token authentication server.
         * @param authServer
         * @return
         */
        public Read<T> withAuthServer(String authServer) {
            checkArgument(authServer != null, "authServer cannot be null");
            return builder().setUsername(authServer).build();
        }

        /**
         * Specifies the token authentication server port.
         * @param authPort
         * @return
         */
        public Read<T> withAuthPort(int authPort) {
            return builder().setAuthPort(authPort).build();
        }

        /**
         * Specifies whether to use ERT service discovery to find endpoints.
         * @param discovery
         * @return
         */
        public Read<T> withServiceDiscovery(boolean discovery) {
            return builder().setServiceDiscovery(discovery).build();
        }

        /**
         * Specifies the token authentication server path.
         * @param authPath
         * @return
         */
        public Read<T> withTokenAuthPath(String authPath) {
            checkArgument(authPath != null, "authPath cannot be null");
            return builder().setTokenAuthPath(authPath).build();
        }

        /**
         * Specifies the service discovery server path.
         * @param discoveryPath
         * @return
         */
        public Read<T> withDiscoveryPath(String discoveryPath) {
            checkArgument(discoveryPath != null, "discoveryPath cannot be null");
            return builder().setDiscoveryPath(discoveryPath).build();
        }

        /**
         * Specifies the DACS/ERT username used when connecting.
         * @param username
         */
        public Read<T> withUsername(String username) {
            checkArgument(username != null, "username cannot be null");
            return builder().setUsername(username).build();
        }

        /**
         * Specifies the ERT user password.
         * @param password
         * @return
         */
        public Read<T> withPassword(String password) {
            return builder().setPassword(password).build();
        }

        /**
         * Specifies the DACS/ERT position parameter.<br>
         * If unset the  address of the local host is used.
         * This is achieved by retrieving the name of the host from the system,
         * then resolving that name into an InetAddress.
         * @param appId
         */
        public Read<T> withPosition(String position) {
            checkArgument(position != null, "position cannot be null");
            return builder().setPosition(position).build();
        }

        /**
         * Specifies the DACS/ERT Application Id parameter.
         * @param appId
         */
        public Read<T> withAppId(String appId) {
            checkArgument(appId != null, "appId cannot be null");
            return builder().setAppId(appId).build();
        }

        /**
         * <p>Specifies the Websocket timeout in MilliSeconds.
         * <p>This must be >= 60000mS (twice ping timeout)
         * @param timeout
         */
        public Read<T> withTimeout(int timeout) {
            checkArgument(timeout >= 60000, "timeout must be > 60000 (larger than ping timer x2), but was: %s", timeout);
            return builder().setTimeout(timeout).build();
        }

        /**
         * <p>Specified the maximum number of mounts.
         * @param maxMounts
         */
        public Read<T> withMaxMounts(int maxMounts) {
            checkArgument(maxMounts > 0, "maxMounts must be greater than zero", maxMounts);
            return builder().setMaxMounts(maxMounts).build();
        }
        /**
         * When set to less than {@code Long.MAX_VALUE} act as a Bounded Source. Used for testing.
         * @param maxNumRecords
         */
        public Read<T> withMaxNumRecords(long maxNumRecords) {
            checkArgument(maxNumRecords > 0, "maxNumRecords must be greater than zero, but was: %s", maxNumRecords);
            return builder().setMaxNumRecords(maxNumRecords).build();
        }

        /**
         * When set, these field values will be cached and returned in each update.
         * @param cachedFields
         */
        public Read<T> withCachedFields(Set<String> cachedFields) {
            checkArgument(cachedFields != null,  "cachedFields cannot be null");
            return builder().setCachedFields(cachedFields).build();
        }

        public Read<T> withTokenStore(String tokenStoreLocation) {
            checkArgument(tokenStoreLocation != null,  "tokenStore cannot be null");
            return builder().setTokenStore(tokenStoreLocation).build();
        }
        /**
         * Specifies the MarketPriceMessage coder.
         * @param coder
         */
        public Read<T> withCoder(Coder<T> coder) {
            checkArgument(coder != null, "coder cannot be null");
            return builder().setCoder(coder).build();
        }

        @Override
        public PCollection<T> expand(PBegin input) {

            checkArgument(getUsername() != null, "withUsername() is required");
            checkArgument(getTokenAuth() == false || getPassword() != null, "withPassword cannot be null");
            checkArgument(getTokenAuth() == false || getTokenStore() != null, "withTokenStore location cannot be null");
            checkArgument(getInstrumentTuples() != null, "withInstrumentTuples() is required");
            checkArgument(getServiceDiscovery() == true || getHostname() != null, "withHostname() is required");

            // Handles unbounded source to bounded conversion if maxNumRecords is set.
            Unbounded<T> unbounded = org.apache.beam.sdk.io.Read.from(createSource());

            PTransform<PBegin, PCollection<T>> transform = unbounded;

            if (getMaxNumRecords() < Long.MAX_VALUE) {
                transform = unbounded.withMaxNumRecords(getMaxNumRecords());
            }

            return input.getPipeline().apply(transform);
        }

        @Override
        public void populateDisplayData(DisplayData.Builder builder) {
            super.populateDisplayData(builder);
            builder.addIfNotNull(DisplayData.item("hostname", getHostname()));
            builder.addIfNotNull(DisplayData.item("port",     getPort()));
            builder.addIfNotNull(DisplayData.item("username", getUsername()));
            builder.addIfNotNull(DisplayData.item("position", getPosition()));
            builder.addIfNotNull(DisplayData.item("appid",    getAppId()));
        }

        /**
         * Creates an UnboundedSource with the configuration in {@link Read}. Primary
         * use case is unit tests, should not be used in an application.
         */
        @VisibleForTesting
        UnboundedTrepWsSource<T> createSource() {
            return new UnboundedTrepWsSource<T>(this, 0);
        }

        @Override
        public String toString() {
            return "Read [" + (getHostname() != null ? "hostname=" + getHostname() + ", " : "") + "port="
                    + getPort() + ", " + (getUsername() != null ? "username=" + getUsername() + ", " : "")
                    + (getPosition() != null ? "position=" + getPosition() + ", " : "")
                    + (getAppId() != null ? "appId=" + getAppId() + ", " : "")
                    + "maxMounts=" + getMaxMounts() + "]";
        }

    }

    private TrepWsIO() {}

    @FunctionalInterface
    public interface MessageMapper<T> extends Serializable {
        T mapMessage(ConcurrentHashMap<String, LoadingCache<String, String>> cache, MarketPrice message, Set<String> set);
    }

    @VisibleForTesting
    protected static class UnboundedTrepWsSource<T> extends UnboundedSource<T, TrepCheckpointMark> {

        private final Read<T> spec;
        private final int id;

        public UnboundedTrepWsSource(Read<T> spec, int id) {
            this.spec = spec;
            this.id = id;
        }

        /**
         * <p>
         * The watchlist (the list of RICs in each InstrumentTuple) is split between ADS
         * mounts/readers in round robin order.
         * <p>
         * The number of splits is the minimum of desiredNumSplits and the getMaxMounts
         * parameter. Note the default getMaxMounts is 1.
         * <p>
         * @throws IOException
         * @throws URISyntaxException
         * @see org.apache.beam.sdk.io.UnboundedSource#split(int,
         *      org.apache.beam.sdk.options.PipelineOptions)
         */
        @Override
        public List<UnboundedTrepWsSource<T>> split(int desiredNumSplits, PipelineOptions options) throws Exception {

            String tokenStore = spec.getTokenStore();

            // Retrieve initial authentication token, store it and pass to each instance....
            if (spec.getTokenAuth()) {

                tokenStore = appendIfMissing(spec.getTokenStore(), "/") + UUID.randomUUID().toString() + ".token";
                LOG.info("Setting token store to {}", tokenStore);

                JSONObject token = getAuthenticationInfo(null, spec.getUsername(), spec.getPassword(),
                        new URIBuilder().setScheme("https")
                                .setHost(String.format("%s:%s", spec.getAuthServer(), spec.getAuthPort()))
                                .setPath(spec.getTokenAuthPath()).build(), true);

                if (token == null) {
                    throw new IOException("Unable to connect to authentication server");
                }

                try {
                    writeFile(tokenStore, token.toString(2));
                    LOG.info("Writing token {} to {}", token.toString(2), tokenStore);
                } catch (Exception e) {
                    throw new IOException("Unable to write to file " + tokenStore);
                }
            }

            List<UnboundedTrepWsSource<T>> sources = new ArrayList<>();

            List<InstrumentTuple> instruments = new ArrayList<>(spec.getInstrumentTuples());
            // (a) fetch all instrumentTuples
            // (b) split list of instrumentTuples into a single tuple per RIC
            // (c) round-robin assign the tuples to splits

            int maxMounts = spec.getMaxMounts();;

            List<InstrumentTuple> singleInstrument = new ArrayList<>();
            for (InstrumentTuple tuple : instruments) {
                for (String ric : tuple.getInstruments()) {
                    singleInstrument.add(
                        InstrumentTuple.of(tuple.getService(), Lists.newArrayList(ric), tuple.getFields()));
                }
            }

            checkArgument(desiredNumSplits > 0);

            int numSplits = Math.min(desiredNumSplits, maxMounts);
            LOG.info("Splitting into {} sources from {} desired splits", numSplits, desiredNumSplits);

            List<List<InstrumentTuple>> assignments = new ArrayList<>(numSplits);

            for (int i = 0; i < numSplits; i++) {
                assignments.add(new ArrayList<>());
            }
            for (int i = 0; i < singleInstrument.size(); i++) {
                assignments.get(i % numSplits).add(singleInstrument.get(i));
            }

            for (int id = 0; id < numSplits; id++) {

              List<InstrumentTuple> assignedToSplit = assignments.get(id);
                if (assignments.get(id).size() > 0) {

                    List<String> rics = Lists.newArrayList();
                    assignedToSplit.forEach(t -> rics.addAll(t.getInstruments()));

                    LOG.info("Instruments assigned to split {} (total {}) {}", new Object[] {
                        id, assignedToSplit.size(), Joiner.on(",").join(rics)});

                    sources.add(new UnboundedTrepWsSource<>(
                            spec.builder()
                            .setInstrumentTuples(assignedToSplit)
                            .setTokenStore(tokenStore)
                            .build(), id));
                } else {
                    LOG.info("Source {} not created as there are no instruments to assign!", id);
                }
            }

            return sources;
        }

        @Override
        public UnboundedReader<T> createReader(
                PipelineOptions options, TrepCheckpointMark checkpointMark) {
            return new UnboundedTrepWsReader<T>(this, checkpointMark, id);
        }

        @Override
        public Coder<T> getOutputCoder() {
            return this.spec.getCoder();
        }

        @Override
        public Coder<TrepCheckpointMark> getCheckpointMarkCoder() {
            return SerializableCoder.of(TrepCheckpointMark.class);
        }
    }

    @VisibleForTesting
    static class UnboundedTrepWsReader<T> extends UnboundedReader<T> {

        private Read<T> spec;
        private String                   position;
        private String                   server;
        private List<String>             protocols;
        protected Session                session;
        private int                      id;

        private T                        currentMessage;
        private TrepCheckpointMark       checkpointMark;
        private UnboundedTrepWsSource<T> source;
        private Instant                  currentTimestamp;
        private JSONObject               serviceJson;
        private Deque<MarketPrice>       dispatchQueue;
        private ListeningExecutorService service;
        private ScheduledExecutorService scheduledService1;
        private ScheduledExecutorService scheduledService2;

        private AtomicInteger               qSize;
        private AtomicBoolean               qFlag;

        private ConcurrentHashMap<String, LoadingCache<String, String>> cache;

        protected GsonBuilder builder;
        protected Gson gson;

        public UnboundedTrepWsReader(UnboundedTrepWsSource<T> source, TrepCheckpointMark checkpointMark, int id) {
            this.source = source;
            this.id = id;

            if (checkpointMark != null) {
                this.checkpointMark = checkpointMark;
            } else {
                this.checkpointMark = new TrepCheckpointMark();
            }
            currentMessage = null;
        }

        @Override
        public boolean start() throws IOException {

            spec = source.spec;
            LOG.info("Starting {} with config {}",id, spec.toString());

            protocols = Lists.newArrayList("tr_json2");
            serviceJson = null;
            cache = new ConcurrentHashMap<String, LoadingCache<String, String>>();
            dispatchQueue = new ConcurrentLinkedDeque<MarketPrice>();
            qSize = new AtomicInteger();
            qFlag = new AtomicBoolean(false);

            service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
            scheduledService1 = Executors.newSingleThreadScheduledExecutor();
            scheduledService2 = Executors.newSingleThreadScheduledExecutor();

            builder = new GsonBuilder();
            builder.disableHtmlEscaping();
            builder.excludeFieldsWithoutExposeAnnotation();
            builder.setLenient();
            gson = builder.create();

            try {

                // Note: Login request send immediately after connection
                session = connect();

                // Periodically set a flag to update the dispatchQueue size counter
                queueSize.update(0);
                scheduledService2.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        qFlag.set(true);
                    }
                }, 60, 1, TimeUnit.SECONDS);



            } catch (Exception e) {

                String error = "ERROR: Cannot connect to " + server +
                        " with username:" + spec.getUsername() +
                        " appId:" + spec.getAppId() +
                        " & position:" + position +
                        " id " + String.valueOf(id) +
                        " due to " + e.getMessage();
                LOG.error(error, e);

                throw new IOException(error, e);
            }

            return advance();
        }

        @Override
        public boolean advance() throws IOException {

            MarketPrice message = dispatchQueue.poll();

            if (message == null) {
                currentMessage = null;
                return false;
            }

            try {

                if (message.getType().equalsIgnoreCase("Update"))
                    qSize.set(Math.max(qSize.get(), dispatchQueue.size()));

                if (qFlag.getAndSet(false)) {
                    queueSize.update(qSize.getAndSet(0));
                }

                if (!nullToEmpty(message.getDomain()).isEmpty())  {
                    LOG.info("Domain message on mount {} {}", id, message.getJsonString());

                    // Inspect the login message
                    if (nullToEmpty(message.getDomain()).equalsIgnoreCase(LOGIN)) {

                        // Error if login is unsuccessful, stream will be Closed
                        if (nullToEmpty(message.getState().getStream()).equalsIgnoreCase(CLOSED)) {
                            throw new Exception("Login unsuccessful " + message.getJsonString());

                        } else {

                            LOG.info("Sending {} requests to mount {}", spec.getInstrumentTuples().size(), id);
                            records.inc(sendRequest(session, spec.getInstrumentTuples(), id));
                        }
                    }
                    return false;

                // Log status messages
                } else if (nullToEmpty(message.getType()).equalsIgnoreCase(STATUS)) {
                    LOG.warn("Status message on mount {} {}", id, message.getJsonString());
                    status.inc();
                    return false;

               // Exception on either a websocket or TREP error message
                } else if (nullToEmpty(message.getType()).equalsIgnoreCase(ERROR)) {

                    LOG.error("ERROR: {} Websocket or TREP message on mount {}", id, message.toString());
                    currentMessage = null;
                    throw new Exception(message.toString());

                // Else process the Refresh (Image) and Update messages
                } else {

                    // Using a checkpoint with a firehose source like TREP is moot as messages
                    // do not need to be acknowledged. However we can use the Checkpoint to
                    // calculate the oldest timestamp for watermarking.
                    checkpointMark.addMessage(message);

                    currentTimestamp = new Instant(message.getTimestamp());
                    currentMessage   = this.source.spec.getMessageMapper().mapMessage(cache, message, spec.getCachedFields());

                    messages.inc();

                    // Uncomment for testing only!
                    LOG.info(String.format("Mount:%d %s %s", id, ((MarketPriceMessage) currentMessage).getName(),
                            ((MarketPriceMessage) currentMessage).getFields()));
                    return true;
                }

            } catch (Exception e) {
                currentMessage = null;
                throw new IOException(String.format("%s exception:%s", message.toString(), e));
            }
        }

        @Override
        public Instant getWatermark() {
            Instant watermark = checkpointMark.getOldestPendingTimestamp();
            //LOG.info("Watermark:{} {}", watermark, id);
            return watermark;
        }

        @Override
        public CheckpointMark getCheckpointMark() {
            return checkpointMark;
        }

        @Override
        public UnboundedSource<T, ?> getCurrentSource() {
            return source;
        }

        @Override
        public T getCurrent() throws NoSuchElementException {

            if (currentMessage == null) {
                throw new NoSuchElementException();
            }

            return currentMessage;
        }

        @Override
        public Instant getCurrentTimestamp() throws NoSuchElementException {

            if (currentMessage == null) {
                throw new NoSuchElementException();
            }
            return currentTimestamp;
        }

        /** Close the TREP session
         * @see org.apache.beam.sdk.io.Source.Reader#close()
         */
        @Override
        public void close() {

            LOG.info("Close called on mount {}", id);

            try {
                deleteFile(spec.getTokenStore());
                session.getBasicRemote().sendText(CLOSE);
                session.close();
            } catch (Exception e) {
                LOG.debug("Unable to close (because we never logged on?)");
            } finally {
                scheduledService1.shutdownNow();
                scheduledService2.shutdownNow();
                service.shutdown();
            }
        }

        /**
         * <p>
         * Connect to the ADS websocket server.
         * <br>
         * Use cloud storage to share the access token between instances when using an ERT in Cloud connection.
         * <ul>
         * <li>server ADS websocket connection e.g. ws://ads1:15000/WebSocket
         * <li>user DACS user
         * <li>appId DACS appId
         * <li>position DACS position
         * <li>timeout websocket timeout <br>
         * <br>
         *
         * @return Session
         * @throws Exception
         */
        private Session connect() throws Exception {

            int expireTime = 0;
            final int delay = 15;
            String authToken = null;

            if (spec.getPosition() == null) {
                position = Inet4Address.getLocalHost().getHostAddress();
            } else {
                position = spec.getPosition();
            }

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();

            if (spec.getTokenAuth()) {

                JSONObject token = new JSONObject(readFile(spec.getTokenStore()));
                if (spec.getServiceDiscovery()) {
                    serviceJson = queryServiceDiscovery(token.getString("access_token"),
                            new URIBuilder().setScheme("https")
                                    .setHost(String.format("%s:%s", spec.getAuthServer(), spec.getAuthPort()))
                                    .setPath(spec.getDiscoveryPath()).setParameter("transport", "websocket").build());

                    Services services = gson.fromJson(serviceJson.toString(), Services.class);

                    services.getServices().stream()
                        .filter(e -> e.getLocation().size() >= 2)
                        .filter(e -> e.getLocation().get(0).startsWith(spec.getRegion()))
                        .filter(e -> e.getTransport().equalsIgnoreCase("websocket"))
                        .forEach(e -> {
                            LOG.info("{}", e);
                            server = String.format("wss://%s:%s/WebSocket", e.getEndpoint(), e.getPort());
                            protocols = e.getDataFormat();
                    });
                    if (server == null)
                        throw new IOException("No endpoints found in region " + spec.getRegion());

                } else {
                    server = String.format("wss://%s:%s/WebSocket", spec.getHostname(), spec.getPort());
                }

                // Determine when the access token expires. We will re-authenticate before then.
                expireTime = Integer.parseInt(token.getString("expires_in")) - 60;
                authToken = token.getString("access_token");

                if (authToken == null)
                    throw new Exception("Unable to connect to authentication server");

                LOG.info("Connecting to WebSocket {} with token expire time of {} seconds, delay {}, id {} ",
                        new Object[] { server, expireTime, (id == 0 ? delay : delay * 2), id });

                // Use cloud storage to share the access token between instances
                scheduledService1.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        try {

                            if (id == 0) {

                                JSONObject newToken = getAuthenticationInfo(
                                        new JSONObject(readFile(spec.getTokenStore())),
                                        spec.getUsername(),
                                        spec.getPassword(),
                                        new URIBuilder().setScheme("https")
                                        .setHost(String.format("%s:%s", spec.getAuthServer(), spec.getAuthPort()))
                                        .setPath(spec.getTokenAuthPath()).build(),
                                        true);

                                if (newToken == null)
                                    throw new Exception("Unable to connect to authentication server");

                                writeFile(spec.getTokenStore(), newToken.toString(2));
                            }

                            String token = new JSONObject(readFile(spec.getTokenStore())).getString("access_token");

                            sendLoginRequest(session, spec.getUsername(), spec.getAppId(), position,
                                    token, false, true, id);

                            System.out.println(
                                    String.format("Sent login reissue request to mount %d from session id %s token %s",
                                            id, session.getId(), right(token, 60)));

                        } catch (Exception e) {
                            System.err.println("Error resending login reissse reqeuest " + e.getMessage());
                        }
                    }
                }, (id == 0 ? delay : delay * 2), expireTime, TimeUnit.SECONDS);

            } else {
                server = String.format("ws://%s:%s/WebSocket", spec.getHostname(), spec.getPort());
                LOG.info("Connecting to WebSocket {}", server);
            }

            ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create()
                    .preferredSubprotocols(protocols).build();

            container.setDefaultMaxSessionIdleTimeout(spec.getTimeout());

            LOG.info("Connecting {} to WebSocket {} with username:{} appId:{} & position:{}",
                    new Object[] { id, server, spec.getUsername(), spec.getAppId(), position });

            TrepWsListener listener = new TrepWsListener(dispatchQueue, server, spec.getUsername(), spec.getAppId(),
                    position, authToken, spec.getTokenAuth(), id);

            ListenableFuture<Session> future = service.submit(new Callable<Session>() {
                public Session call() throws Exception {
                    return container.connectToServer(listener, clientEndpointConfig, new URI(server));
                }
            });

            final Session session = future.get(spec.getTimeout(), TimeUnit.MILLISECONDS);

            return session;
        }
    }

    protected static String tokenInfo(JSONObject previous) {
        return StringUtils.right(previous.getString("access_token"), 30);
    }

    /**
     * Authenticate to the gateway via an HTTP post request. Initially authenticates
     * using the specified password. If information from a previous authentication
     * response is provided, it instead authenticates using the refresh token from
     * that response.
     *
     * @param previousAuthResponseJson Information from a previous authentication,
     * if available
     * @return A JSONObject containing the authentication information from the
     * response.
     */
    protected static JSONObject getAuthenticationInfo(JSONObject previousAuthResponseJson, String username,
            String password, URI url, boolean takeExclusiveSignOnControl) {

        try {
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(new SSLContextBuilder().build());

            HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            HttpPost httppost = new HttpPost(url);

            // Set request parameters.
            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("client_id", username));
            params.add(new BasicNameValuePair("username", username));

            // Note: If user credentials permit multiple concurrent sign-ons, this parameter
            // invalidates the Refresh token for all previous sign-ons when it is set to true
            params.add(new BasicNameValuePair("takeExclusiveSignOnControl", Boolean.toString(takeExclusiveSignOnControl)));

            if (previousAuthResponseJson == null) {
                // First time through, send password.
                params.add(new BasicNameValuePair("grant_type", "password"));
                params.add(new BasicNameValuePair("password", password));
                params.add(new BasicNameValuePair("scope", "trapi"));
                LOG.info("Sending authentication request with password to {} {}", url, params);
            } else {
                // Use the refresh token we got from the last authentication response.
                params.add(new BasicNameValuePair("grant_type", "refresh_token"));
                params.add(new BasicNameValuePair("refresh_token", previousAuthResponseJson.getString("refresh_token")));
                LOG.info("Sending authentication request with refresh token to {}", url);
            }

            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            // Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                // Authentication failed.
                LOG.error("EDP-GW authentication failure: {} {}",response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase());
                LOG.error("Text: {}", EntityUtils.toString(response.getEntity()));

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
                        && previousAuthResponseJson != null) {
                    // If we got a 401 response (unauthorised), our refresh token may have expired.
                    // Try again using our password.
                    LOG.warn("401 response (unauthorised), our refresh token may have expired.");
                    return getAuthenticationInfo(null, username, password, url, takeExclusiveSignOnControl);
                }

                return null;
            }

            // Authentication was successful. Deserialize the response and return it.
            JSONObject responseJson = new JSONObject(EntityUtils.toString(response.getEntity()));
            LOG.info("EDP-GW Authentication succeeded. {}", tokenInfo(responseJson));
            return responseJson;

        } catch (Exception e) {
            LOG.error("EDP-GW authentication failure:", e);
            return null;
        }
    }

    /**
     * Retrieve service information indicating locations to connect to.
     *
     * @return A JSONObject containing the service information.
     */
    public static JSONObject queryServiceDiscovery(String authToken, URI uri) {
        try {
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(new SSLContextBuilder().build());

            HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            HttpGet httpget = new HttpGet(uri);

            httpget.setHeader("Authorization", "Bearer " + authToken);

            LOG.info("Sending EDP-GW service discovery request to {}", uri.toString());

            // Execute and get the response.
            HttpResponse response = httpclient.execute(httpget);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                // Discovery request failed.
                LOG.error("EDP-GW Service discovery result failure: {} {} Text: {}",
                        new Object[] { response.getStatusLine().getStatusCode(),
                                response.getStatusLine().getReasonPhrase(),
                                EntityUtils.toString(response.getEntity()) });
                return null;
            }

            // Discovery request was successful. Deserialize the response and return it.
            JSONObject responseJson = new JSONObject(EntityUtils.toString(response.getEntity()));
            LOG.info("EDP-GW Service discovery succeeded. {}",responseJson.toString());
            return responseJson;

        } catch (Exception e) {
            LOG.error("EDP-GW Service discovery failure:", e);
            return null;
        }
    }


    /**
     * Read contents of a file from cloud storage.
     * @param resource (String)
     * @return String
     * @throws IOException
     */
    public static String readFile(String resource) throws IOException {
        FileSystems.setDefaultPipelineOptions(PipelineOptionsFactory.create());
        ResourceId existingFileResourceId = FileSystems.matchSingleFileSpec(resource).resourceId();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                ReadableByteChannel readerChannel = FileSystems.open(existingFileResourceId);
                WritableByteChannel writerChannel = Channels.newChannel(out)) {
            ByteStreams.copy(readerChannel, writerChannel);
            return out.toString();
        }
    }

    /**
     * Write string to a file in cloud storage.
     * @param resource (String)
     * @param contentToWrite
     * @throws IOException
     */
    public static void writeFile(String resource, String contentToWrite) throws IOException {
        FileSystems.setDefaultPipelineOptions(PipelineOptionsFactory.create());
        ResourceId newFileResourceId = FileSystems.matchNewResource(resource, false);
        try (ByteArrayInputStream in = new ByteArrayInputStream(contentToWrite.getBytes());
                ReadableByteChannel readerChannel = Channels.newChannel(in);
                WritableByteChannel writerChannel = FileSystems.create(newFileResourceId, MimeTypes.TEXT)) {
            ByteStreams.copy(readerChannel, writerChannel);
        }
    }

    /**
     * Delete a file from cloud storage.
     * @param resource (String)
     * @throws IOException
     */
    public static void deleteFile(String resource) throws IOException {
        FileSystems.setDefaultPipelineOptions(PipelineOptionsFactory.create());
        ResourceId existingFileResourceId = FileSystems.matchSingleFileSpec(resource).resourceId();
        FileSystems.delete(Collections.singletonList(existingFileResourceId),
                MoveOptions.StandardMoveOptions.IGNORE_MISSING_FILES);
        System.out.println(String.format("Deleting file %s", resource));
    }

}
