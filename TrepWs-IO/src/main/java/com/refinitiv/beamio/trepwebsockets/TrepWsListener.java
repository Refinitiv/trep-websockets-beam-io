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

import static com.google.common.base.Strings.nullToEmpty;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.DOMAIN;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.ERROR;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.LOGIN;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.PING;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.REFRESH;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.TYPE;
import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.joda.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO.InstrumentTuple;
import com.refinitiv.beamio.trepwebsockets.json.Elements;
import com.refinitiv.beamio.trepwebsockets.json.Key;
import com.refinitiv.beamio.trepwebsockets.json.Login;
import com.refinitiv.beamio.trepwebsockets.json.MarketPrice;
import com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer;
import com.refinitiv.beamio.trepwebsockets.json.Request;

public class TrepWsListener extends Endpoint {

    protected static final String CLOSE        = "{\"Domain\":\"Login\",\"ID\":1,\"Type\":\"Close\"}";
	private static final String PONG           = "{\"Type\":\"Pong\"}";
	private static final String WS_CLOSE_ERROR = "{\"Error\":\"Closing Websocket\"}";
	private static final String WS_ERROR       = "{\"Error\":\"Websocket Error\"}";

	private static final Logger LOG = LoggerFactory.getLogger(TrepWsListener.class);

    private String server;
    private String user;
    private String appId;
    private String position;
    private int reader;
    private String authToken;

    private Deque<MarketPrice>   dispatchQueue;
    private boolean ert_rt;

    protected static HashMap<String, Long> instrumentList;

    private static Gson          gson;
    private static GsonBuilder   builder;
    private static AtomicInteger id;

	/**
     * A TREP websocket message listener
     *
	 * @param dispatchQueue
	 * @param queueSize2
	 * @param server
	 * @param user
	 * @param appId
	 * @param position
	 * @param authToken
	 * @param ert_rt
	 * @param reader
	 */
    public TrepWsListener(Deque<MarketPrice> dispatchQueue, String server, String user, String appId,
            String position, String authToken, boolean ert_rt, int reader) {
        this.server = server;
        this.user = user;
        this.appId = appId;
        this.position = position;
        this.dispatchQueue = dispatchQueue;
        this.reader = reader;
        this.authToken = authToken;
        this.ert_rt = ert_rt;

        instrumentList = new HashMap<String, Long>();

        id = new AtomicInteger(2);
        builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        builder.excludeFieldsWithoutExposeAnnotation();
        builder.setLenient();
        builder.registerTypeAdapter(MarketPrice.class, new MarketPriceDeserializer());
        gson = builder.create();
	}

    /**
	 * Called when handshake is complete and websocket is open, send login
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		LOG.info("WebSocket successfully connected! from reader {}", reader);

		session.addMessageHandler(new MessageHandler.Whole<String>() {

			/**
			 * Called when message received, parse message into JSON for processing
			 */
			@Override
			public void onMessage(String message) {

				if (!message.isEmpty()) {
					JSONArray jsonArray = new JSONArray(message);
					for (int i = 0; i < jsonArray.length(); ++i) {
						processMessage(session, jsonArray.getJSONObject(i), dispatchQueue);
					}
				}
			}
		});

		try {
			sendLoginRequest(session, user, appId, position, authToken, true, ert_rt, reader);
		} catch (IOException e) {
			LOG.error("ERROR: Unable send login request with {} to {} from reader {}",
			        new Object[] {user, session, reader});
		}
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {

		if (closeReason.getCloseCode() != CloseCodes.NORMAL_CLOSURE) {
			MarketPrice error = new MarketPrice(0L, ERROR, 0L, "", WS_CLOSE_ERROR, Instant.now());
			error = error.withText(closeReason.toString());
			LOG.error("ERROR: websocket onClose {} {} {} see https://tools.ietf.org/html/rfc6455", new Object[] {reader,
			        closeReason, closeReason.getCloseCode()});
			dispatchQueue.add(error);
		} else {
			LOG.info("Websocket {} closing with {} {} see https://tools.ietf.org/html/rfc6455",
			        new Object[] {reader, closeReason.getCloseCode(), closeReason.getReasonPhrase()});
		}
	}

	@Override
	public void onError(Session session, Throwable throwable) {

        LOG.error("ERROR: websocket onError {} {} see https://tools.ietf.org/html/rfc6455", reader, throwable.getCause().toString());

		MarketPrice error = new MarketPrice(0L, ERROR, 0L, "", WS_ERROR, Instant.now())
		        .withText(throwable.getCause().toString());
		    dispatchQueue.add(error);
	}

	/**
	 * Create and send a Login request
	 * @param isFirstLogin
	 * @param authToken
	 * @param id 
	 * @param ert_rt2
	 */
    public static void sendLoginRequest(Session websocket, String user, String appId, String position, String authToken,
            boolean isFirstLogin, boolean ert, int id) throws IOException {

        Key key;
        if (ert) {
            key = new Key()
            .withName(user)
            .withNameType("AuthnToken")
            .withElements(new Elements()
                .withAuthenticationToken(authToken)
                .withApplicationId(appId)
                .withPosition(position));
        } else {
            key = new Key()
                    .withName(user)
                    .withElements(new Elements()
                        .withApplicationId(appId)
                        .withPosition(position));
        }

        Login login = new Login()
                .withID(1L)
                .withDomain(LOGIN)
                .withRefresh(isFirstLogin)
                .withKey(key);

		websocket.getBasicRemote().sendText(gson.toJson(login));
		LOG.info("Sent login {} {}", gson.toJson(login));
	}

	/**
	 * Create and send a Market Price request from instrument tuples
	 *
	 * @param session
	 * @param instrumentTuples
	 * @return
	 * @throws IOException
	 */
    protected static int sendRequest(Session session, List<InstrumentTuple> instrumentTuples, int reader) throws IOException {

        int records = 0;
        for (InstrumentTuple tuple : instrumentTuples) {

            for (String instrument : tuple.getInstruments()) {

                Key key = new Key().withName(instrument);

                if (tuple.getService() != null && !tuple.getService().isEmpty())
                    key = key.withService(tuple.getService());

                // if instrument contains # send request with LINK_ , LONGLINK added to fields
                // or just ignore fields altogether

                Request request = new Request()
                        .withID(id.getAndIncrement())
                        .withKey(key);

                if (tuple.getFields() != null && !tuple.getFields().isEmpty()) {
                    request = request.withView(tuple.getFields());
                }

                LOG.info("Sent request {} {}", reader, gson.toJson(request));
                session.getBasicRemote().sendText(gson.toJson(request));
                records++;
                instrumentList.put(nullToEmpty(tuple.getService()) + ":" + instrument, id.longValue());

            }
        }
        return records;
    }

	/**
	 * Parse at high level and output JSON of message
	 *
	 * @param websocket
	 * @param messageJson
	 * @param queue
	 */
	private void processMessage(Session websocket, JSONObject messageJson, Deque<MarketPrice> queue) {

	    final String jsonString =  messageJson.toString();
	    final String messageType = messageJson.getString(TYPE);

		MarketPrice mpMessage = gson.fromJson(jsonString, MarketPrice.class);

		switch (messageType) {

		case PING:
		    try {
		        websocket.getBasicRemote().sendText(PONG);
		    } catch (Exception e) {
		        LOG.error("Unable to send Pong message {}", reader);
		    }
		    break;

		case REFRESH:
		    if (messageJson.has(DOMAIN)) {
		        String messageDomain = messageJson.getString(DOMAIN);
		        if (messageDomain.equals(LOGIN)) {
		            LOG.info("Login success {} {}", reader, messageJson.toString());
		        }
		    }

		default:
		    queue.add(mpMessage);
		    break;
		}
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		result = prime * result + ((dispatchQueue == null) ? 0 : dispatchQueue.hashCode());
		result = prime * result + ((server == null) ? 0 : server.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrepWsListener other = (TrepWsListener) obj;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (dispatchQueue == null) {
			if (other.dispatchQueue != null)
				return false;
		} else if (!dispatchQueue.equals(other.dispatchQueue))
			return false;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}
