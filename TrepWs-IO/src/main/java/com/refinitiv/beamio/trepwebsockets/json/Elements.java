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

public class Elements implements Serializable {

	@SerializedName("ApplicationName")
	@Expose
	private String applicationName;
	@SerializedName("SupportOptimizedPauseResume")
	@Expose
	private Long supportOptimizedPauseResume;
	@SerializedName("Position")
	@Expose
	private String position;
	@SerializedName("SupportViewRequests")
	@Expose
	private Long supportViewRequests;
	@SerializedName("SupportBatchRequests")
	@Expose
	private Long supportBatchRequests;
	@SerializedName("SupportEnhancedSymbolList")
	@Expose
	private Long supportEnhancedSymbolList;
	@SerializedName("SupportStandby")
	@Expose
	private Long supportStandby;
	@SerializedName("SingleOpen")
	@Expose
	private Long singleOpen;
	@SerializedName("SupportPauseResume")
	@Expose
	private Long supportPauseResume;
	@SerializedName("AllowSuspectData")
	@Expose
	private Long allowSuspectData;
	@SerializedName("ProvidePermissionProfile")
	@Expose
	private Long providePermissionProfile;
	@SerializedName("ProvidePermissionExpressions")
	@Expose
	private Long providePermissionExpressions;
	@SerializedName("ApplicationId")
	@Expose
	private String applicationId;
	@SerializedName("SupportOMMPost")
	@Expose
	private Long supportOMMPost;
	@SerializedName("PingTimeout")
	@Expose
	private Long pingTimeout;
	@SerializedName("MaxMsgSize")
	@Expose
	private Long maxMsgSize;
    @SerializedName("AuthenticationToken")
    @Expose
    private String authenticationToken;

	private final static long serialVersionUID = -5454523231053331691L;

	public Long getPingTimeout() {
		return pingTimeout;
	}

	public void setPingTimeout(Long pingTimeout) {
		this.pingTimeout = pingTimeout;
	}

	public Elements withPingTimeout(Long pingTimeout) {
		this.pingTimeout = pingTimeout;
		return this;
	}

	public Long getMaxMsgSize() {
		return maxMsgSize;
	}

	public void setMaxMsgSize(Long maxMsgSize) {
		this.maxMsgSize = maxMsgSize;
	}

	public Elements withMaxMsgSize(Long maxMsgSize) {
		this.maxMsgSize = maxMsgSize;
		return this;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public Elements withApplicationName(String applicationName) {
		this.applicationName = applicationName;
		return this;
	}

	public Long getSupportOptimizedPauseResume() {
		return supportOptimizedPauseResume;
	}

	public void setSupportOptimizedPauseResume(Long supportOptimizedPauseResume) {
		this.supportOptimizedPauseResume = supportOptimizedPauseResume;
	}

	public Elements withSupportOptimizedPauseResume(Long supportOptimizedPauseResume) {
		this.supportOptimizedPauseResume = supportOptimizedPauseResume;
		return this;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Elements withPosition(String position) {
		this.position = position;
		return this;
	}

	public Long getSupportViewRequests() {
		return supportViewRequests;
	}

	public void setSupportViewRequests(Long supportViewRequests) {
		this.supportViewRequests = supportViewRequests;
	}

	public Elements withSupportViewRequests(Long supportViewRequests) {
		this.supportViewRequests = supportViewRequests;
		return this;
	}

	public Long getSupportBatchRequests() {
		return supportBatchRequests;
	}

	public void setSupportBatchRequests(Long supportBatchRequests) {
		this.supportBatchRequests = supportBatchRequests;
	}

	public Elements withSupportBatchRequests(Long supportBatchRequests) {
		this.supportBatchRequests = supportBatchRequests;
		return this;
	}

	public Long getSupportEnhancedSymbolList() {
		return supportEnhancedSymbolList;
	}

	public void setSupportEnhancedSymbolList(Long supportEnhancedSymbolList) {
		this.supportEnhancedSymbolList = supportEnhancedSymbolList;
	}

	public Elements withSupportEnhancedSymbolList(Long supportEnhancedSymbolList) {
		this.supportEnhancedSymbolList = supportEnhancedSymbolList;
		return this;
	}

	public Long getSupportStandby() {
		return supportStandby;
	}

	public void setSupportStandby(Long supportStandby) {
		this.supportStandby = supportStandby;
	}

	public Elements withSupportStandby(Long supportStandby) {
		this.supportStandby = supportStandby;
		return this;
	}

	public Long getSingleOpen() {
		return singleOpen;
	}

	public void setSingleOpen(Long singleOpen) {
		this.singleOpen = singleOpen;
	}

	public Elements withSingleOpen(Long singleOpen) {
		this.singleOpen = singleOpen;
		return this;
	}

	public Long getSupportPauseResume() {
		return supportPauseResume;
	}

	public void setSupportPauseResume(Long supportPauseResume) {
		this.supportPauseResume = supportPauseResume;
	}

	public Elements withSupportPauseResume(Long supportPauseResume) {
		this.supportPauseResume = supportPauseResume;
		return this;
	}

	public Long getAllowSuspectData() {
		return allowSuspectData;
	}

	public void setAllowSuspectData(Long allowSuspectData) {
		this.allowSuspectData = allowSuspectData;
	}

	public Elements withAllowSuspectData(Long allowSuspectData) {
		this.allowSuspectData = allowSuspectData;
		return this;
	}

	public Long getProvidePermissionProfile() {
		return providePermissionProfile;
	}

	public void setProvidePermissionProfile(Long providePermissionProfile) {
		this.providePermissionProfile = providePermissionProfile;
	}

	public Elements withProvidePermissionProfile(Long providePermissionProfile) {
		this.providePermissionProfile = providePermissionProfile;
		return this;
	}

	public Long getProvidePermissionExpressions() {
		return providePermissionExpressions;
	}

	public void setProvidePermissionExpressions(Long providePermissionExpressions) {
		this.providePermissionExpressions = providePermissionExpressions;
	}

	public Elements withProvidePermissionExpressions(Long providePermissionExpressions) {
		this.providePermissionExpressions = providePermissionExpressions;
		return this;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public Elements withApplicationId(String applicationId) {
		this.applicationId = applicationId;
		return this;
	}

	public Long getSupportOMMPost() {
		return supportOMMPost;
	}

	public void setSupportOMMPost(Long supportOMMPost) {
		this.supportOMMPost = supportOMMPost;
	}

	public Elements withSupportOMMPost(Long supportOMMPost) {
		this.supportOMMPost = supportOMMPost;
		return this;
	}

	public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }
    public Elements withAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
        return this;
    }

    @Override
    public String toString() {
        return String.format(
                "Elements [applicationName=%s, supportOptimizedPauseResume=%s, position=%s, supportViewRequests=%s, supportBatchRequests=%s, supportEnhancedSymbolList=%s, supportStandby=%s, singleOpen=%s, supportPauseResume=%s, allowSuspectData=%s, providePermissionProfile=%s, providePermissionExpressions=%s, applicationId=%s, supportOMMPost=%s, pingTimeout=%s, maxMsgSize=%s, authenticationToken=%s]",
                applicationName, supportOptimizedPauseResume, position, supportViewRequests, supportBatchRequests,
                supportEnhancedSymbolList, supportStandby, singleOpen, supportPauseResume, allowSuspectData,
                providePermissionProfile, providePermissionExpressions, applicationId, supportOMMPost, pingTimeout,
                maxMsgSize, authenticationToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowSuspectData, applicationId, applicationName, authenticationToken, maxMsgSize,
                pingTimeout, position, providePermissionExpressions, providePermissionProfile, singleOpen,
                supportBatchRequests, supportEnhancedSymbolList, supportOMMPost, supportOptimizedPauseResume,
                supportPauseResume, supportStandby, supportViewRequests);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Elements other = (Elements) obj;
        return Objects.equals(allowSuspectData, other.allowSuspectData)
                && Objects.equals(applicationId, other.applicationId)
                && Objects.equals(applicationName, other.applicationName)
                && Objects.equals(authenticationToken, other.authenticationToken)
                && Objects.equals(maxMsgSize, other.maxMsgSize) && Objects.equals(pingTimeout, other.pingTimeout)
                && Objects.equals(position, other.position)
                && Objects.equals(providePermissionExpressions, other.providePermissionExpressions)
                && Objects.equals(providePermissionProfile, other.providePermissionProfile)
                && Objects.equals(singleOpen, other.singleOpen)
                && Objects.equals(supportBatchRequests, other.supportBatchRequests)
                && Objects.equals(supportEnhancedSymbolList, other.supportEnhancedSymbolList)
                && Objects.equals(supportOMMPost, other.supportOMMPost)
                && Objects.equals(supportOptimizedPauseResume, other.supportOptimizedPauseResume)
                && Objects.equals(supportPauseResume, other.supportPauseResume)
                && Objects.equals(supportStandby, other.supportStandby)
                && Objects.equals(supportViewRequests, other.supportViewRequests);
    }
}