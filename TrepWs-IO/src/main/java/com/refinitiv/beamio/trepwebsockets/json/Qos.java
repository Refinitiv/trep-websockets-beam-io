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

public class Qos implements Serializable {

	@SerializedName("Rate")
	@Expose
	private String rate;
	@SerializedName("Timeliness")
	@Expose
	private String timeliness;
	private final static long serialVersionUID = 8936491750608574839L;

	public String getRate() {
		return rate;
	}

	public void setRate(String rate) {
		this.rate = rate;
	}

	public Qos withRate(String rate) {
		this.rate = rate;
		return this;
	}

	public String getTimeliness() {
		return timeliness;
	}

	public void setTimeliness(String timeliness) {
		this.timeliness = timeliness;
	}

	public Qos withTimeliness(String timeliness) {
		this.timeliness = timeliness;
		return this;
	}

    @Override
    public String toString() {
        return String.format("Qos [rate=%s, timeliness=%s]", rate, timeliness);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate, timeliness);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Qos other = (Qos) obj;
        return Objects.equals(rate, other.rate) && Objects.equals(timeliness, other.timeliness);
    }

}