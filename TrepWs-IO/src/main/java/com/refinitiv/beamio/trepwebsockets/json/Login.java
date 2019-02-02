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
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Login implements Serializable {

	@SerializedName("ID")
	@Expose
	private Long iD;
	@SerializedName("Domain")
	@Expose
	private String domain;
	@SerializedName("Key")
	@Expose
	private Key key;
	private final static long serialVersionUID = 8633297709127600321L;

	public Long getID() {
		return iD;
	}

	public void setID(Long iD) {
		this.iD = iD;
	}

	public Login withID(Long iD) {
		this.iD = iD;
		return this;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Login withDomain(String domain) {
		this.domain = domain;
		return this;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Login withKey(Key key) {
		this.key = key;
		return this;
	}

	@Override
	public String toString() {
		return "Login [" + (iD != null ? "id=" + iD + ", " : "") + (domain != null ? "domain=" + domain + ", " : "")
				+ (key != null ? "key=" + key : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((iD == null) ? 0 : iD.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		Login other = (Login) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (iD == null) {
			if (other.iD != null)
				return false;
		} else if (!iD.equals(other.iD))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}



}