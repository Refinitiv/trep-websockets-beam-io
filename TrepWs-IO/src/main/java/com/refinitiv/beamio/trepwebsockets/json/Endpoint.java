package com.refinitiv.beamio.trepwebsockets.json;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Endpoint {

    @SerializedName("endpoint")
    @Expose
    private String       endpoint;
    @SerializedName("port")
    @Expose
    private Integer      port;
    @SerializedName("provider")
    @Expose
    private String       provider;
    @SerializedName("dataFormat")
    @Expose
    private List<String> dataFormat = null;
    @SerializedName("location")
    @Expose
    private List<String> location   = null;
    @SerializedName("transport")
    @Expose
    private String       transport;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(List<String> dataFormat) {
        this.dataFormat = dataFormat;
    }

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    @Override
    public String toString() {
        return String.format("Endpoint [endpoint=%s, port=%s, provider=%s, dataFormat=%s, location=%s, transport=%s]",
                endpoint, port, provider, dataFormat, location, transport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataFormat, endpoint, location, port, provider, transport);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Endpoint other = (Endpoint) obj;
        return Objects.equals(dataFormat, other.dataFormat) && Objects.equals(endpoint, other.endpoint)
                && Objects.equals(location, other.location) && Objects.equals(port, other.port)
                && Objects.equals(provider, other.provider) && Objects.equals(transport, other.transport);
    }

}
