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
import static com.refinitiv.beamio.trepwebsockets.TrepWsListener.sendRequest;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.CLOSED;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.ERROR;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.LOGIN;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.STATUS;

import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.URI;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.Read.Unbounded;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.io.UnboundedSource.CheckpointMark;
import org.apache.beam.sdk.io.UnboundedSource.UnboundedReader;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.refinitiv.beamio.trepwebsockets.json.MarketPrice;

@SuppressWarnings("serial")
@Experimental(Experimental.Kind.SOURCE_SINK)
public class TrepWsIO {
	
	private static final Logger LOG = LoggerFactory.getLogger(TrepWsIO.class);
	
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
     * This source will split into <i>n</i> parts which is the minimum of desiredNumSplits
     * and the getMaxMounts parameter. Note the default getMaxMounts is 1. The
     * watchlist sent to each ADS mount is derived by extracting each RIC from the
     * InstrumentTuples and distributing them in round robin order.
     * <p>
     * This source does not process checkpoint marks, but does record sequence
     * numbers and timestamps for watermarking.
     * 
     * <p>
     * To configure a TREP Websocket source, you must specify at the minimum ADS
     * hostname, DACS username and a list of instrument tuples. For example:
     * 
     * <pre>
     * {@code
     * 
     * // Firstly create any number instrument tuples of Service, RIC list and Field list
     * //   Service can be null, in which case the default ADS Service will be used
     * //   RIC list cannot be null
     * //   Fields can be null, in which case field filtering (a View) will not apply
     * 
     * InstrumentTuple instrument1 = InstrumentTuple.of(
     *   "IDN_RDF", 
     *   Lists.newArrayList("EUR=","JPY="),
     *   Lists.newArrayList("BID","ASK"));
     *   
     * InstrumentTuple instrument2 = InstrumentTuple.of(
     *   null, 
     *   Lists.newArrayList("MSFT.O","IBM.N"),  
     *   null);
     *	
     * PCollection<MarketPriceMessage> messages = 
     *   pipeline.apply(TrepWsIO.read()
     *    .withHostname("websocket-ads") // ADS hostname
     *    .withUsername("user")          // DACS username
     *    .withInstrumentTuples(Lists.newArrayList(instrument1, instrument2))
     *    // Above three are required configuration
     *				
     *    // Rest of the settings are optional:
     *    
     *    // ADS websocket port, if unset 15000 is used
     *    .withPort(15000) 
     *    
     *    // The maximum number of ADS mounts (overridden if the number of 
     *    // desired splits is smaller). If unset then 1 is used
     *    .withMaxMounts(4)
     * 
     *    // The DACS position, if unset the IP address of the local host is used
     *    .withPosition("192.168.1.104")
     *    
     *    // The DACS application ID, if unset 256 is used
     *    .withAppId("123")
     *				
     *    // The wesocket MaxSessionIdleTimeout in milliSeconds. 
     *    // Note: this must be greater that 6000 (twice the ping timeout)
     *    .withTimeout(60000)
     *			
     *   );
     *   
     *   messages.apply(....) // PCollection<MarketPriceMessage>
     * }
     * </pre>
     */
	public static Read<MarketPriceMessage> read() {
		return new AutoValue_TrepWsIO_Read.Builder<MarketPriceMessage>()
				.setMaxNumRecords(Long.MAX_VALUE)
				.setPort(15000)
				.setAppId("256")
				.setTimeout(60000)
				.setMaxMounts(1)
				.setCoder(SerializableCoder.of(MarketPriceMessage.class))
				.setMessageMapper(
					(MessageMapper<MarketPriceMessage>)
					new MessageMapper<MarketPriceMessage>() {

					    @Override
					    public MarketPriceMessage mapMessage(MarketPrice message) {
					        return new MarketPriceMessage(
			                        message.getID(),
			                        message.getType(),
			                        message.getUpdateType(), 
			                        message.getSeqNumber(),
			                        message.getKey().getName(),
			                        message.getKey().getService(),
			                        message.getFields(),
			                        message.getJsonString());	
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

		@Nullable
		abstract String getUsername();

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
		
		abstract Builder<T> builder();

		@AutoValue.Builder
		abstract static class Builder<T> {	 
 
			abstract Builder<T> setHostname(String hostname);

			abstract Builder<T> setPort(int port);

			abstract Builder<T> setUsername(String username);

			abstract Builder<T> setPosition(String position);

			abstract Builder<T> setAppId(String appId);
						
			abstract Builder<T> setMaxNumRecords(long maxNumRecords);
			
			abstract Builder<T> setTimeout(int timeout);
			
			abstract Builder<T> setMaxMounts(int maxMounts);
			
			abstract Builder<T> setMessageMapper(MessageMapper<T> messageMapper);

			abstract Builder<T> setCoder(Coder<T> coder);
			
			abstract Builder<T> setInstrumentTuples(List<InstrumentTuple> instrumentTuples);
						
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
			checkArgument(instrumentTuples != null, "instrumentTuples can not be null");
			checkArgument(!instrumentTuples.isEmpty(), "instrumentTuples list can not be empty");
			return builder().setInstrumentTuples(instrumentTuples).build();
		}
		
		/**
		 * Specifies the hostname where the ADS is running.
		 * @param hostname
		 */
		public Read<T> withHostname(String hostname) {
			checkArgument(hostname != null, "hostname can not be null");
			return builder().setHostname(hostname).build();
		}
		/**
		 * Specifies the port number on which the ADS is listening for Websocket connections.
		 * @param port
		 */
		public Read<T> withPort(int port) {
			return builder().setPort(port).build();
		}

		/**
		 * Specifies the DACS username used when connecting to the ADS.
		 * @param username
		 */
		public Read<T> withUsername(String username) {
			checkArgument(username != null, "username can not be null");
			return builder().setUsername(username).build();
		}

		/**
		 * Specifies the DACS position parameter.<br>
		 * If unset the  address of the local host is used. 
		 * This is achieved by retrieving the name of the host from the system, 
		 * then resolving that name into an InetAddress.
		 * @param appId
		 */
		public Read<T> withPosition(String position) {
			checkArgument(position != null, "position can not be null");
			return builder().setPosition(position).build();
		}

		/**
		 * Specifies the DACS Application Id parameter.
		 * @param appId
		 */
		public Read<T> withAppId(String appId) {
			checkArgument(appId != null, "appId can not be null");
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
		 * <p>Specified the maximum number of ADS mounts
		 * @param maxMounts
		 */
		public Read<T> withMaxMounts(int maxMounts) {
		    return builder().setMaxMounts(maxMounts).build();
		}
		/**
		 * When set to less than {@code Long.MAX_VALUE} act as a Bounded Source. Used for testing.
		 * @param maxNumRecords
		 */
		public Read<T> withMaxNumRecords(long maxNumRecords) {
			checkArgument(maxNumRecords >= 0, "maxNumRecords must be > 0, but was: %s", maxNumRecords);
			return builder().setMaxNumRecords(maxNumRecords).build();
		}
		
		/**
		 * Specifies the MarketPriceMessage coder.
		 * @param coder
		 */
		public Read<T> withCoder(Coder<T> coder) {
			checkArgument(coder != null, "coder can not be null");
			return builder().setCoder(coder).build();
		}

		@Override
		public PCollection<T> expand(PBegin input) {
			
			checkArgument(getHostname() != null, "withHostname() is required");
			checkArgument(getUsername() != null, "withUsername() is required");
			checkArgument(getInstrumentTuples() != null, "withInstrumentTuples() is required");
			
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
		T mapMessage(MarketPrice message);
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
         * @see org.apache.beam.sdk.io.UnboundedSource#split(int,
         *      org.apache.beam.sdk.options.PipelineOptions)
         */
		@Override
		public List<UnboundedTrepWsSource<T>> split(int desiredNumSplits, PipelineOptions options) 
				throws Exception {

			List<UnboundedTrepWsSource<T>> sources = new ArrayList<>();
			
			List<InstrumentTuple> instruments = new ArrayList<>(spec.getInstrumentTuples());
		    // (a) fetch all instrumentTuples
			// (b) split list of instrumentTuples into a single tuple per RIC
		    // (c) round-robin assign the tuples to splits

			int maxMounts = spec.getMaxMounts();
			
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

            for (int i = 0; i < numSplits; i++) {
                
              List<InstrumentTuple> assignedToSplit = assignments.get(i);
                if (assignments.get(i).size() > 0) {
                    
                    LOG.info("Instruments assigned to split {} (total {}) {}", 
                        i, assignedToSplit.size(),
                        Joiner.on(",").join(assignedToSplit));
                
                    sources.add(new UnboundedTrepWsSource<>(
                            spec.builder().setInstrumentTuples(assignedToSplit).build(), i));
                } else {
                    LOG.info("Source {} not created as there are no instruments to assign!",i);
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
		private int id;
		private UnboundedTrepWsSource<T> source;
		private TrepCheckpointMark checkpointMark;
		
		private T       currentMessage;
        private Instant currentTimestamp;

        private String server;
        private String position;

        private Deque<MarketPrice>       dispatchQueue;
        protected Session                session;
        private ListeningExecutorService service;
  
		public UnboundedTrepWsReader(UnboundedTrepWsSource<T> source, TrepCheckpointMark checkpointMark, int id) {
			this.source = source;
			this.id = id;
			if (checkpointMark != null) {
				this.checkpointMark = checkpointMark;
			} else {
				this.checkpointMark = new TrepCheckpointMark();
			}
			this.currentMessage = null;	
		}

		@Override
		public boolean start() throws IOException {
			
            spec = source.spec;
            LOG.info("Starting {} with config {}",id, spec.toString());
            
            dispatchQueue = new ConcurrentLinkedDeque<MarketPrice>();
            
            service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    			
			try {

				if (spec.getPosition() == null) {
					position = Inet4Address.getLocalHost().getHostAddress();
				} else {
					position = spec.getPosition();
				}
				
				server = String.format("ws://%s:%s/WebSocket", spec.getHostname(), spec.getPort());
	
				// Note: Login request send immediately after connection
				session = connect(server, spec.getUsername(), spec.getAppId(), position, spec.getTimeout());
				
			} catch (Exception e) {
				
				String error = "Cannot connect to " + server + 
						" with username:" + spec.getUsername() + 
						" appId:" + spec.getAppId() +
						" & position:" + position +
						" id " + String.valueOf(id) +
						" due to " + e.getMessage();
				LOG.error(error);
				
				throw new IOException(error,e);
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
		        
		        queueSize.update(dispatchQueue.size());

		        if (!nullToEmpty(message.getDomain()).isEmpty())  {
		            LOG.info("Domain message {} {}", id, message.getJsonString());

		            // Inspect the login message
		            if (nullToEmpty(message.getDomain()).equalsIgnoreCase(LOGIN)) {

		                // Error if login is unsuccessful, stream will be Closed
		                if (nullToEmpty(message.getState().getStream()).equalsIgnoreCase(CLOSED)) { 
		                    throw new Exception("Login unsuccessful " + message.getJsonString());

		                } else {

		                    LOG.info("Sending requests to ADS {}", id);
		                    records.inc(sendRequest(session, spec.getInstrumentTuples(), id));
		                }
		            }
		            return false;		

		        // Log status messages
		        } else if (nullToEmpty(message.getType()).equalsIgnoreCase(STATUS)) {
                    LOG.warn("Status message {} {}", id, message.getJsonString());
		            status.inc();
		            return false;

		       // Exception on either a websocket or TREP error message
		        } else if (nullToEmpty(message.getType()).equalsIgnoreCase(ERROR)) {

		            LOG.error("Error received {} {}", id, message.toString());	
		            throw new Exception(message.toString());

		        // Else process the Refresh (Image) and Update messages	
		        } else {

                    // Using a checkpoint with a firehose source like TREP is moot as messages
                    // do not need to be acknowledged. However we can use the Checkpoint to
                    // calculate the oldest timestamp for watermarking.
		            checkpointMark.addMessage(message);

                    currentTimestamp = new Instant(message.getTimestamp());		             
		            currentMessage   = this.source.spec.getMessageMapper().mapMessage(message);

		            messages.inc();
		            return true;
		        }

		    } catch (Exception e) {
		        throw new IOException(e);
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
			
				LOG.info("Close {}", id);
			
			try {
				session.getBasicRemote().sendText(CLOSE);
				session.close();
			} catch (Exception e) {
				LOG.debug("Unable to close (because we never logged on?)");
			}
			service.shutdown();
			
		}

		/**
         * <p>
         * Connect to the ADS websocket server.
         * <ul>
         * <li>server ADS websocket connection e.g. ws://ads1:15000/WebSocket
         * <li>user DACS user
         * <li>appId DACS appId
         * <li>position DACS position
         * <li>timeout websocket timeout <br>
         * <br>
         * 
         * @param server
         * @param user
         * @param appId
         * @param position
         * @param timeout
         * @return Session
         * @throws Exception
         */
		private Session connect(String server, String user, String appId, String position, Integer timeout) 
		        throws Exception {

		    WebSocketContainer container = ContainerProvider.getWebSocketContainer();

		    ClientEndpointConfig clientEndpointConfig =
		            ClientEndpointConfig.Builder.create()
		            .preferredSubprotocols(Lists.newArrayList("tr_json2"))
		            .build();

		    if (timeout != null) {
		        container.setDefaultMaxSessionIdleTimeout(timeout);
		        LOG.info("Setting {} MaxSessionIdleTimeout to {}mS", id, timeout);
		    }

		    LOG.info("Connecting {} to WebSocket {} with username:{} appId:{} & position:{}",
		            new Object[] { id, server, spec.getUsername(), appId, position});

		    ListenableFuture<Session> future = service.submit(new Callable<Session>() {
		        public Session call() throws Exception {

                    return container.connectToServer(
                            new TrepWsListener(dispatchQueue, server, user, appId, position, id), 
                            clientEndpointConfig, new URI(server));
		        }
		    });
		    return future.get(timeout, TimeUnit.MILLISECONDS);

		}
	}
}
