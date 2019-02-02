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

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nullable;

import org.joda.time.Instant;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MarketPrice implements Serializable {

	private static final long serialVersionUID = -1613203974707615163L;

	@SerializedName("Type")
	@Expose
	private String type;
	
	@SerializedName("Fields")
	@Expose
	@Nullable private Map<String, String> fields;
	
	@SerializedName("Qos")
	@Expose
	@Nullable private Qos qos;
	
	@SerializedName("State")
	@Expose
	@Nullable private State state;
	
	@SerializedName("PermData")
	@Expose
	@Nullable private String permData;
	
	@SerializedName("ID")
	@Expose
	@Nullable private Long iD;
	
	@SerializedName("SeqNumber")
	@Expose
	@Nullable private Long seqNumber;
	
	@SerializedName("Domain")
	@Expose
	@Nullable private String domain;
	
	@SerializedName("Elements")
	@Expose
	@Nullable private Elements elements;
	
	@SerializedName("Key")
	@Expose
	@Nullable private Key key;

	@SerializedName("UpdateType")
	@Expose
	@Nullable private String updateType;
	
	@SerializedName("DoNotConflate")
	@Expose
	@Nullable private Boolean doNotConflate;

	@SerializedName("Text")
	@Expose
	@Nullable private String text;
	
	private String jsonString;
	
	Instant timestamp;
	

    /**
     * MarketPrice JSON 
     * 
     * @param id
     * @param type
     * @param seqNumber
     * @param domain
     * @param jsonString
     */
public MarketPrice(Long id, String type, Long seqNumber, String domain, String jsonString) {
		this.type = type;
		this.iD = id;
		this.seqNumber = seqNumber;
		this.domain = domain;
		this.jsonString = jsonString;
		timestamp = Instant.now();
	}

	public Instant getTimestamp() {
		return timestamp;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public MarketPrice withType(String type) {
		this.type = type;
		return this;
	}

	public Map<String, String> getFields() {
		return fields;
	}

	public void setFields(Map<String, String> fields) {
		this.fields = fields;
	}

	public MarketPrice withFields(Map<String, String> fieldList) {
		this.fields = fieldList;
		return this;
	}

	public String getUpdateType() {
		return updateType;
	}

	public void setUpdateType(String updateType) {
		this.updateType = updateType;
	}
	
	public MarketPrice withUpdateType(String updateType) {
		this.updateType = updateType;
		return this;
	}
	
	public Qos getQos() {
		return qos;
	}

	public void setQos(Qos qos) {
		this.qos = qos;
	}

	public MarketPrice withQos(Qos qos) {
		this.qos = qos;
		return this;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public MarketPrice withState(State state) {
		this.state = state;
		return this;
	}

	public String getPermData() {
		return permData;
	}

	public void setPermData(String permData) {
		this.permData = permData;
	}

	public MarketPrice withPermData(String permData) {
		this.permData = permData;
		return this;
	}

	public Long getID() {
		return iD;
	}

	public void setID(Long iD) {
		this.iD = iD;
	}

	public MarketPrice withID(Long iD) {
		this.iD = iD;
		return this;
	}

	public Long getSeqNumber() {
		return seqNumber;
	}

	public void setSeqNumber(Long seqNumber) {
		this.seqNumber = seqNumber;
	}

	public MarketPrice withSeqNumber(Long seqNumber) {
		this.seqNumber = seqNumber;
		return this;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public MarketPrice withDomain(String domain) {
		this.domain = domain;
		return this;
	}

	public Elements getElements() {
		return elements;
	}

	public void setElements(Elements elements) {
		this.elements = elements;
	}

	public MarketPrice withElements(Elements elements) {
		this.elements = elements;
		return this;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public MarketPrice withKey(Key key) {
		this.key = key;
		return this;
	}

	public Boolean isDoNotConflate() {
		return doNotConflate;
	}

	public void setDoNotConflate(Boolean doNotConflate) {
		this.doNotConflate = doNotConflate;
	}
	
	public MarketPrice withDoNotConflate(Boolean doNotConflate) {
		this.doNotConflate = doNotConflate;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public MarketPrice withText(String text) {
		this.text = text;
		return this;
	}

	public String getJsonString() {
		return jsonString;
	}

	public void setJsonString(String jsonString) {
		this.jsonString = jsonString;
	}

	public Boolean getDoNotConflate() {
		return doNotConflate;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "MarketPrice [" + (type != null ? "Type=" + type + " " : "")
				+ (text != null ? "Text=" + text + " " : "") 
				+ (updateType != null ? "UpdateType=" + updateType + " " : "") 
				+ (iD != null ? "ID=" + iD + " " : "")
				+ (state != null ? "State=" + state + " " : "") 
				+ (domain != null ? "Domain=" + domain + ", " : "")
				+ (elements != null ? "Elements=" + elements + " " : "") 
				+ (key != null ? "Key=" + key + " " : "")
				+ (fields != null ? "Fields=" + fields.toString() + " " : "")
				+ (permData != null ? "PermData=" + permData + " " : "")
				+ (seqNumber != null ? "SeqNumber=" + seqNumber + " " : "") 
				+ (doNotConflate != null ? "DoNotConflate=" + doNotConflate + " " : "") 
				+ (qos != null ? "Qos=" + qos : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((doNotConflate == null) ? 0 : doNotConflate.hashCode());
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + ((iD == null) ? 0 : iD.hashCode());
		result = prime * result + ((jsonString == null) ? 0 : jsonString.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((permData == null) ? 0 : permData.hashCode());
		result = prime * result + ((qos == null) ? 0 : qos.hashCode());
		result = prime * result + ((seqNumber == null) ? 0 : seqNumber.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		MarketPrice other = (MarketPrice) obj;
		if (doNotConflate == null) {
			if (other.doNotConflate != null)
				return false;
		} else if (!doNotConflate.equals(other.doNotConflate))
			return false;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (iD == null) {
			if (other.iD != null)
				return false;
		} else if (!iD.equals(other.iD))
			return false;
		if (jsonString == null) {
			if (other.jsonString != null)
				return false;
		} else if (!jsonString.equals(other.jsonString))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (permData == null) {
			if (other.permData != null)
				return false;
		} else if (!permData.equals(other.permData))
			return false;
		if (qos == null) {
			if (other.qos != null)
				return false;
		} else if (!qos.equals(other.qos))
			return false;
		if (seqNumber == null) {
			if (other.seqNumber != null)
				return false;
		} else if (!seqNumber.equals(other.seqNumber))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
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