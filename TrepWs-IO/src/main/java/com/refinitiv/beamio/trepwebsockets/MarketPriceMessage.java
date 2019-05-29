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

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.joda.time.Instant;

/**
 * <b>A class representing a TREP MarketPrice Domain message</b>
 * <p>A MarketPriceMessage includes:
 * <ul>
 * <li>a <code>long</code> <b>id</b> (the request ID)</li>
 * <li>a <code>String</code> <b>type</b> (Refresh, Update, etc.)</li>
 * <li>a <code>String</code> <b>updateType</b> (Trade, Quote, ClosingRun, etc.)</li>
 * <li>a <code>long</code> <b>seqNumber</b> (the backbone sequence number of each message)</li>
 * <li>a <code>String</code> <b>name</b> (the RIC name)</li>
 * <li>a <code>String</code> <b>service</b> (the ADS service name)</li>
 * <li>a {@code Map<String, String>} list of <b>fields</b> (e.g. ASK=1.1512, BID=1.151, etc.)</li>
 * <li>the <code>String</code> raw <b>jsonString</b> received from the ADS</li>
 * <li>an <code>Instant</code> <b>timestamp</b> (the message receive time)</li>
 * </ul>
 */

public class MarketPriceMessage implements Serializable {

	private static final long serialVersionUID = 5117958681080078223L;
	private final long   id;
	private final String type;
	private final String updateType;
	private final long   seqNumber;
	private final String name;
	private final String service;
	private final Map<String, String> fields;
	private final String jsonString;
	private final Instant timestamp;

	public MarketPriceMessage(long id, String type, String updateType, long seqNumber, String name, String service,
			Map<String, String> fields, String jsonString, Instant timestamp) {
		this.id = id;
		this.type = type;
		this.updateType = updateType;
		this.seqNumber = seqNumber;
		this.name = name;
		this.service = service;
		this.fields = fields;
		this.jsonString = jsonString;
		this.timestamp = timestamp;
	}

	public long getId() {
		return id;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getType() {
		return type;
	}

	public long getSeqNumber() {
		return seqNumber;
	}

	public String getUpdateType() {
		return updateType;
	}

	public String getName() {
		return name;
	}

	public String getService() {
		return service;
	}

	public Map<String, String> getFields() {
		return fields;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public String getJsonString() {
		return jsonString;
	}

	@Override
    public String toString() {
        return String.format(
                "MarketPriceMessage [id=%s, type=%s, updateType=%s, seqNumber=%s, name=%s, service=%s, fields=%s, jsonString=%s, timestamp=%s]",
                id, type, updateType, seqNumber, name, service, fields, jsonString, timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, id, jsonString, name, seqNumber, service, timestamp, type, updateType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MarketPriceMessage other = (MarketPriceMessage) obj;
        return Objects.equals(fields, other.fields) && id == other.id && Objects.equals(jsonString, other.jsonString)
                && Objects.equals(name, other.name) && seqNumber == other.seqNumber
                && Objects.equals(service, other.service) && Objects.equals(timestamp, other.timestamp)
                && Objects.equals(type, other.type) && Objects.equals(updateType, other.updateType);
    }

}

