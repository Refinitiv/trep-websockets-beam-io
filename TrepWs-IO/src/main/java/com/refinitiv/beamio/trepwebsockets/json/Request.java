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
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Request implements Serializable {

	@SerializedName("ID")
	@Expose
	private Integer iD;
	
	@SerializedName("Key")
	@Expose
	private Key key;

	@SerializedName("View")
	@Expose
	private List<String> view = null;
	
	private final static long serialVersionUID = 8026475551191846496L;

	public Integer getID() {
		return iD;
	}

	public void setID(Integer iD) {
		this.iD = iD;
	}

	public Request withID(Integer iD) {
		this.iD = iD;
		return this;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Request withKey(Key key) {
		this.key = key;
		return this;
	}

	public List<String> getView() {
		return view;
	}

	public void setView(List<String> view) {
		this.view = view;
	}

	public Request withView(List<String> view) {
		this.view = view;
		return this;
	}

	@Override
	public String toString() {
		return "Request [" + (iD != null ? "id=" + iD + ", " : "") + (key != null ? "key=" + key + ", " : "")
				+ (view != null ? "view=" + view : "") + "]";
	}


}