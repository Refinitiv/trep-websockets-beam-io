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

import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.DOMAIN;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.ERROR;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.LOGIN;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.PING;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.REFRESH;
import static com.refinitiv.beamio.trepwebsockets.json.MarketPriceDeserializer.TYPE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

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
    
    private Deque<MarketPrice>   dispatchQueue;

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
	 * @param reader 
	 */
    public TrepWsListener(Deque<MarketPrice> dispatchQueue, String server, String user, String appId,
            String position, int reader) {
        this.server = server;
        this.user = user;
        this.appId = appId;
        this.position = position;
        this.dispatchQueue = dispatchQueue;
        this.reader = reader;
		
		id = new AtomicInteger(2);
		builder = new GsonBuilder();
		builder.disableHtmlEscaping();
		builder.excludeFieldsWithoutExposeAnnotation();
		builder.registerTypeAdapter(MarketPrice.class, new MarketPriceDeserializer());
		gson = builder.create();	    
	}

	/**
	 * Called when handshake is complete and websocket is open, send login
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		LOG.debug("WebSocket successfully connected! from reader {}", reader);

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
			sendLoginRequest(session, user, appId, position);
		} catch (IOException e) {
			LOG.error("Unable send login request with {} to {} from reader {}", 
			        new Object[] {user, session, reader});
		}
	}
	
	@Override
	public void onClose(Session session, CloseReason closeReason) {

		if (closeReason.getCloseCode() != CloseCodes.NORMAL_CLOSURE) {
			MarketPrice error = new MarketPrice(0L, ERROR, 0L, EMPTY, WS_CLOSE_ERROR);
			error = error.withText(closeReason.toString());
			dispatchQueue.add(error);
		} else {
			LOG.info("Websocket closing with {} {} {}", 
			        new Object[] {closeReason.getCloseCode(), closeReason.getReasonPhrase(), reader});
		}
	}
	
	@Override
	public void onError(Session session, Throwable throwable) {
		
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
        LOG.info("onError {} {}", reader, sw.toString());
		
		MarketPrice error = new MarketPrice(0L, ERROR, 0L, EMPTY, WS_ERROR);
		error = error.withText(sw.toString());
		dispatchQueue.add(error);
	}

	/**
	 * Create and send a Login request
	 */
	private void sendLoginRequest(Session websocket, String user, String appId, String position)
			throws IOException {

		Login login = new Login()
				.withID(1L)
				.withDomain(LOGIN)
				.withKey(new Key()
					.withName(user)
					.withElements(new Elements()
						.withApplicationId(appId)
						.withPosition(position)));
		
		websocket.getBasicRemote().sendText(gson.toJson(login));
		LOG.info("Sent login {} {}", reader, gson.toJson(login));
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

                Request request = new Request()
                        .withID(id.getAndIncrement())
                        .withKey(key);

                if (tuple.getFields() != null && !tuple.getFields().isEmpty()) {
                    request = request.withView(tuple.getFields());
                }
                LOG.info("Sent request {} {}", reader, gson.toJson(request));
                session.getBasicRemote().sendText(gson.toJson(request));
                records++;
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

		String messageType = messageJson.getString(TYPE);

		MarketPrice mpMessage = gson.fromJson(messageJson.toString(), MarketPrice.class);
		  
		switch (messageType) {

		case REFRESH:
			if (messageJson.has(DOMAIN)) {
				String messageDomain = messageJson.getString(DOMAIN);
				if (messageDomain.equals(LOGIN)) {
					LOG.info("Login success {} {}", reader, messageJson.toString());
				}
			} 

			queue.add(mpMessage);
			break;

		case PING:
			try {
				websocket.getBasicRemote().sendText(PONG);
			} catch (IOException e) {
				LOG.warn("Unable to send Pong message {}", reader);
			} 
			break;

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
