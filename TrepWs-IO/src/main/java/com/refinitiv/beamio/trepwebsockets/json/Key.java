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

public class Key implements Serializable {

    @SerializedName("Elements")
	@Expose
	private Elements elements;

	@SerializedName("Service")
	@Expose
	private String service;

	@SerializedName("Name")
	@Expose
	private String name;

    @SerializedName("NameType")
    @Expose
    private String nameType;

	private final static long serialVersionUID = 8128289660364757210L;

	public Elements getElements() {
		return elements;
	}

	public void setElements(Elements elements) {
		this.elements = elements;
	}

	public Key withElements(Elements elements) {
		this.elements = elements;
		return this;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Key withService(String service) {
		this.service = service;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Key withName(String name) {
		this.name = name;
		return this;
	}

	public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }

    public Key withNameType(String nameType) {
        this.nameType = nameType;
        return this;
    }

    @Override
    public String toString() {
        return String.format("Key [elements=%s, service=%s, name=%s, nameType=%s]", elements, service, name, nameType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, name, nameType, service);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Key other = (Key) obj;
        return Objects.equals(elements, other.elements) && Objects.equals(name, other.name)
                && Objects.equals(nameType, other.nameType) && Objects.equals(service, other.service);
    }

}