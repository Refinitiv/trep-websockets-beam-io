package com.refinitiv.beamio.trepwebsockets.json;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Services {

    @SerializedName("services")
    @Expose
    private List<Endpoint> services = null;

    public List<Endpoint> getServices() {
        return services;
    }

    public void setServices(List<Endpoint> services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return String.format("Services [services=%s]", services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(services);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Services other = (Services) obj;
        return Objects.equals(services, other.services);
    }

}