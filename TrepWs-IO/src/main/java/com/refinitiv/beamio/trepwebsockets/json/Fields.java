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

public class Fields implements Serializable {

	private final static long serialVersionUID = 4075688646935590617L;

	public Fields() {
	}

	@Override
	public String toString() {
		return "Fields []";
	}
	
/*	//@SerializedName("ASK")
	//@Expose
	private Double aSK;
	//@SerializedName("BID")
	//@Expose
	private Double bID;
	

	public Double getASK() {
		return aSK;
	}

	public void setASK(Double aSK) {
		this.aSK = aSK;
	}

	public Fields withASK(Double aSK) {
		this.aSK = aSK;
		return this;
	}

	public Double getBID() {
		return bID;
	}

	public void setBID(Double bID) {
		this.bID = bID;
	}

	public Fields withBID(Double bID) {
		this.bID = bID;
		return this;
	}*/
/*
	@Override
	public String toString() {
		return new ToStringBuilder(this,ToStringStyle.NO_CLASS_NAME_STYLE).append("ASK", aSK).append("BID", bID).toString();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(aSK).append(bID).toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Fields) == false) {
			return false;
		}
		Fields rhs = ((Fields) other);
		return new EqualsBuilder().append(aSK, rhs.aSK).append(bID, rhs.bID).isEquals();
	}*/

}
