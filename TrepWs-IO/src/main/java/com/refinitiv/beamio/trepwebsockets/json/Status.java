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
import java.util.Objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Status implements Serializable {

	@SerializedName("Type")
	@Expose
	private String type;
	@SerializedName("State")
	@Expose
	private State state;
	@SerializedName("ID")
	@Expose
	private Long iD;
	@SerializedName("Domain")
	@Expose
	private String domain;
	@SerializedName("Elements")
	@Expose
	private Elements elements;
	@SerializedName("Key")
	@Expose
	private Key key;

	private final static long serialVersionUID = 2519343993787445470L;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Status withType(String type) {
		this.type = type;
		return this;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Status withState(State state) {
		this.state = state;
		return this;
	}

	public Long getID() {
		return iD;
	}

	public void setID(Long iD) {
		this.iD = iD;
	}

	public Status withID(Long iD) {
		this.iD = iD;
		return this;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Status withDomain(String domain) {
		this.domain = domain;
		return this;
	}

	public Elements getElements() {
		return elements;
	}

	public void setElements(Elements elements) {
		this.elements = elements;
	}

	public Status withElements(Elements elements) {
		this.elements = elements;
		return this;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Status withKey(Key key) {
		this.key = key;
		return this;
	}

    @Override
    public String toString() {
        return String.format("Status [type=%s, state=%s, iD=%s, domain=%s, elements=%s, key=%s]", type, state, iD,
                domain, elements, key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, elements, iD, key, state, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Status other = (Status) obj;
        return Objects.equals(domain, other.domain) && Objects.equals(elements, other.elements)
                && Objects.equals(iD, other.iD) && Objects.equals(key, other.key) && Objects.equals(state, other.state)
                && Objects.equals(type, other.type);
    }

}