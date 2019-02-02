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
package com.refinitiv.beamio.trepwebsockets.json;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * <p>
 * Deserialize a MarketPrice message
 * <p>
 * where MarketPrice fields are converted to a {@code Map<String,String>} of Key/Value pairs
 */
public class MarketPriceDeserializer implements JsonDeserializer<MarketPrice>, Serializable {
		
	private static final long serialVersionUID = -4326025988507520754L;
	private static final Logger LOG = LoggerFactory.getLogger(MarketPriceDeserializer.class);	
	
	public static final String CLOSED          = "Closed";
	public static final String CODE            = "Code";
	public static final String DATA            = "Data";
	public static final String DO_NOT_CONFLATE = "DoNotConflate";
	public static final String DOMAIN          = "Domain";
	public static final String ERROR           = "Error";
	public static final String FIELDS          = "Fields";
	public static final String ID              = "ID";
	public static final String KEY             = "Key";
	public static final String LOGIN           = "Login";
	public static final String NAME            = "Name";
	public static final String NONE            = "None";
	public static final String PERM_DATA       = "PermData";
	public static final String PING            = "Ping";
	public static final String REFRESH         = "Refresh";
	public static final String SEQ_NUMBER      = "SeqNumber";
	public static final String SERVICE         = "Service";
	public static final String STATE           = "State";
	public static final String STATUS          = "Status";
	public static final String STREAM          = "Stream";
	public static final String TEXT            = "Text";
	public static final String TYPE            = "Type";
	public static final String UPDATE          = "Update";
	public static final String UPDATE_TYPE     = "UpdateType"; 

	private Instant instance;
	
	@Override
	public MarketPrice deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		instance = Instant.now();
		MarketPrice marketPrice = null;
		
		try {
			
			final JsonObject jsonObject = json.getAsJsonObject();
			 
			if (jsonObject.get(TYPE).getAsString().equalsIgnoreCase(PING)) {
				return null;
			}
			
			final Long id        = jsonObject.get(ID) != null ? jsonObject.get(ID).getAsLong() : -1L;
			final Long seqNumber = jsonObject.get(SEQ_NUMBER) != null ? jsonObject.get(SEQ_NUMBER).getAsLong() : -1L;
			final String domain  = jsonObject.get(DOMAIN) != null ? jsonObject.get(DOMAIN).getAsString() : EMPTY;
			final String type    = jsonObject.get(TYPE) != null ? jsonObject.get(TYPE).getAsString() : EMPTY;
			
			marketPrice = new MarketPrice(id, type, seqNumber, domain, json.toString());
			
			if (jsonObject.get(STATE) != null) {
				
				State state = new State()
						.withStream(jsonObject.get(STATE).getAsJsonObject().get(STREAM).getAsString())
						.withData(jsonObject.get(STATE).getAsJsonObject().get(DATA).getAsString())
						.withText(jsonObject.get(STATE).getAsJsonObject().get(TEXT).getAsString());

				if (jsonObject.get(STATE).getAsJsonObject().get(CODE) != null) {
					state.setCode(jsonObject.get(STATE).getAsJsonObject().get(CODE).getAsString());
				} else {
					state.setCode(NONE);
				}
				marketPrice = marketPrice.withState(state);
			}
			
			if (jsonObject.get(KEY) != null) {
				Key key = new Key()
						.withName(jsonObject.get(KEY).getAsJsonObject().get(NAME).getAsString());
				if (jsonObject.get(KEY).getAsJsonObject().get(SERVICE) != null) 
					key = key.withService(jsonObject.get(KEY).getAsJsonObject().get(SERVICE).getAsString());
		
				marketPrice = marketPrice.withKey(key);
			}

			if (jsonObject.get(FIELDS) != null) {
				
				Map<String,String> fieldList = new TreeMap<String,String>();

				final JsonObject fields = jsonObject.get(FIELDS).getAsJsonObject();
				
				for (String fid : fields.keySet()) {

					//TODO convert value to object type based on the RDMFieldDictionary
					String value = null;
					if (!fields.get(fid).isJsonNull()) {
						value = fields.get(fid).getAsString();
					}
					fieldList.put(fid, value);
				}
				marketPrice = marketPrice.withFields(fieldList);
			}	
			
			if (jsonObject.get(PERM_DATA) != null) {
				marketPrice = marketPrice.withPermData(jsonObject.get(PERM_DATA).getAsString());
			} 
			
			if (jsonObject.get(TEXT) != null) {
				marketPrice = marketPrice.withText(jsonObject.get(TEXT).getAsString());
			}
			
			if (jsonObject.get(UPDATE_TYPE) != null) {
				marketPrice = marketPrice.withUpdateType(jsonObject.get(UPDATE_TYPE).getAsString());
			}
			
			if (jsonObject.get(DO_NOT_CONFLATE) != null) {
				Boolean doNotConflate = jsonObject.get(DO_NOT_CONFLATE).getAsBoolean();
				marketPrice = marketPrice.withDoNotConflate(doNotConflate);
			}
			
		} catch (Exception e) {
			LOG.error("Deserialization error: {}", e.getMessage(), e);
			throw new JsonParseException("Deserialization error: " + e.getMessage());
		}
		
		return marketPrice;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((instance == null) ? 0 : instance.hashCode());
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
		MarketPriceDeserializer other = (MarketPriceDeserializer) obj;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		return true;
	}

}
