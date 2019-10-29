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
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import avro.shaded.com.google.common.collect.Maps;

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

	@Override
	public MarketPrice deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		MarketPrice marketPrice = null;
		final JsonObject jsonObject = json.getAsJsonObject();

		try {

			if (jsonObject.has(TYPE) && jsonObject.get(TYPE).getAsString().equalsIgnoreCase(PING)) {
				return null;
			}

			final Long id        = jsonObject.has(ID)         ? jsonObject.get(ID).getAsLong() : -1L;
			final String type    = jsonObject.has(TYPE)       ? jsonObject.get(TYPE).getAsString() : EMPTY;
			final Long seqNumber = jsonObject.has(SEQ_NUMBER) ? jsonObject.get(SEQ_NUMBER).getAsLong() : -1L;
			final String domain  = jsonObject.has(DOMAIN)     ? jsonObject.get(DOMAIN).getAsString() : EMPTY;

			marketPrice = new MarketPrice(id, type, seqNumber, domain, jsonObject.toString(), Instant.now());

			if (jsonObject.has(STATE)) {

				State state = new State()
						.withStream(jsonObject.get(STATE).getAsJsonObject().get(STREAM).getAsString())
						.withData(jsonObject.get(STATE).getAsJsonObject().get(DATA).getAsString())
						.withText(jsonObject.get(STATE).getAsJsonObject().get(TEXT).getAsString());

				if (jsonObject.get(STATE).getAsJsonObject().has(CODE)) {
					state.setCode(jsonObject.get(STATE).getAsJsonObject().get(CODE).getAsString());
				} else {
					state.setCode(NONE);
				}
				marketPrice = marketPrice.withState(state);
			}

			if (jsonObject.has(KEY)) {
				Key key = new Key()
						.withName(jsonObject.get(KEY).getAsJsonObject().get(NAME).getAsString());
				if (jsonObject.get(KEY).getAsJsonObject().has(SERVICE) )
					key = key.withService(jsonObject.get(KEY).getAsJsonObject().get(SERVICE).getAsString());

				marketPrice = marketPrice.withKey(key);
			}

			if (jsonObject.has(FIELDS)) {

				Map<String,String> fieldList = Maps.newLinkedHashMap();

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

			if (jsonObject.has(PERM_DATA)) {
				marketPrice = marketPrice.withPermData(jsonObject.get(PERM_DATA).getAsString());
			}

			if (jsonObject.has(TEXT) && !jsonObject.get(TEXT).isJsonNull()) {
				marketPrice = marketPrice.withText(defaultIfBlank(jsonObject.get(TEXT).toString(), EMPTY));
			}

			if (jsonObject.has(UPDATE_TYPE)) {
				marketPrice = marketPrice.withUpdateType(jsonObject.get(UPDATE_TYPE).getAsString());
			}

			if (jsonObject.has(DO_NOT_CONFLATE)) {
				marketPrice = marketPrice.withDoNotConflate(jsonObject.get(DO_NOT_CONFLATE).getAsBoolean());
			}

		} catch (Exception e) {
            LOG.error("ERROR: Deserialization json:{} {} {}", jsonObject.toString(), e.getMessage(), e);
            throw new JsonParseException(
                    String.format("%s json:%s %s", "ERROR: Deserialization ", jsonObject.toString(), e.getMessage()));
		}

		return marketPrice;
	}

}
