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
 * <li>an <code>Instant</code> <b>timestamp</b> (the message receive time)</li>
 * <li>the <code>String</code> raw <b>jsonString</b> received from the ADS</li>
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
	
	private final Instant timestamp = Instant.now();
	
	public MarketPriceMessage(long id, String type, String updateType, long seqNumber, String name, String service,
			Map<String, String> fields, String jsonString) {
		super();
		this.id = id;
		this.type = type;
		this.updateType = updateType;
		this.seqNumber = seqNumber;
		this.name = name;
		this.service = service;
		this.fields = fields;
		this.jsonString = jsonString;
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
		return "MarketPriceMessage [id=" + id + ", " 
				+ (type != null ? "type=" + type + ", " : "")
				+ (updateType != null ? "updateType=" + updateType + ", " : "") 
				+ "seqNumber=" + seqNumber + ", "
				+ (name != null ? "name=" + name + ", " : "") 
				+ (service != null ? "service=" + service + ", " : "")
				+ (fields != null ? "fields=" + fields + ", " : "")
				+ (timestamp != null ? "timestamp=" + timestamp : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (seqNumber ^ (seqNumber >>> 32));
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((updateType == null) ? 0 : updateType.hashCode());
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
		MarketPriceMessage other = (MarketPriceMessage) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (seqNumber != other.seqNumber)
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (updateType == null) {
			if (other.updateType != null)
				return false;
		} else if (!updateType.equals(other.updateType))
			return false;
		return true;
	}

}

