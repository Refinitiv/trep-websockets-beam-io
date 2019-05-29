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

public class State implements Serializable {

	@SerializedName("Stream")
	@Expose
	private String stream;
	@SerializedName("Data")
	@Expose
	private String data;
	@SerializedName("Code")
	@Expose
	private String code;
	@SerializedName("Text")
	@Expose
	private String text;

	private final static long serialVersionUID = -8979189921742102609L;

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public State withStream(String stream) {
		this.stream = stream;
		return this;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public State withData(String data) {
		this.data = data;
		return this;
	}

	public String getCode() {
		return data;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public State withCode(String code) {
		this.code = code;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public State withText(String text) {
		this.text = text;
		return this;
	}

    @Override
    public String toString() {
        return String.format("State [stream=%s, data=%s, code=%s, text=%s]", stream, data, code, text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, data, stream, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        State other = (State) obj;
        return Objects.equals(code, other.code) && Objects.equals(data, other.data)
                && Objects.equals(stream, other.stream) && Objects.equals(text, other.text);
    }

}
