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

	@Override
	public String toString() {
		return "Elements [" + (applicationName != null ? "applicationName=" + applicationName + ", " : "")
				+ (applicationId != null ? "applicationId=" + applicationId + ", " : "")
				+ (position != null ? "position=" + position + ", " : "")
				+ (pingTimeout != null ? "pingTimeout=" + pingTimeout + ", " : "")
				+ (maxMsgSize != null ? "maxMsgSize=" + maxMsgSize + ", " : "")
				+ (supportOptimizedPauseResume != null
						? "supportOptimizedPauseResume=" + supportOptimizedPauseResume + ", "
						: "")
				+ (supportViewRequests != null ? "supportViewRequests=" + supportViewRequests + ", " : "")
				+ (supportBatchRequests != null ? "supportBatchRequests=" + supportBatchRequests + ", " : "")
				+ (supportEnhancedSymbolList != null ? "supportEnhancedSymbolList=" + supportEnhancedSymbolList + ", "
						: "")
				+ (supportStandby != null ? "supportStandby=" + supportStandby + ", " : "")
				+ (singleOpen != null ? "singleOpen=" + singleOpen + ", " : "")
				+ (supportPauseResume != null ? "supportPauseResume=" + supportPauseResume + ", " : "")
				+ (allowSuspectData != null ? "allowSuspectData=" + allowSuspectData + ", " : "")
				+ (providePermissionProfile != null ? "providePermissionProfile=" + providePermissionProfile + ", "
						: "")
				+ (providePermissionExpressions != null
						? "providePermissionExpressions=" + providePermissionExpressions + ", "
						: "")
				+ (supportOMMPost != null ? "supportOMMPost=" + supportOMMPost : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((allowSuspectData == null) ? 0 : allowSuspectData.hashCode());
		result = prime * result + ((applicationId == null) ? 0 : applicationId.hashCode());
		result = prime * result + ((applicationName == null) ? 0 : applicationName.hashCode());
		result = prime * result + ((maxMsgSize == null) ? 0 : maxMsgSize.hashCode());
		result = prime * result + ((pingTimeout == null) ? 0 : pingTimeout.hashCode());
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		result = prime * result
				+ ((providePermissionExpressions == null) ? 0 : providePermissionExpressions.hashCode());
		result = prime * result + ((providePermissionProfile == null) ? 0 : providePermissionProfile.hashCode());
		result = prime * result + ((singleOpen == null) ? 0 : singleOpen.hashCode());
		result = prime * result + ((supportBatchRequests == null) ? 0 : supportBatchRequests.hashCode());
		result = prime * result + ((supportEnhancedSymbolList == null) ? 0 : supportEnhancedSymbolList.hashCode());
		result = prime * result + ((supportOMMPost == null) ? 0 : supportOMMPost.hashCode());
		result = prime * result + ((supportOptimizedPauseResume == null) ? 0 : supportOptimizedPauseResume.hashCode());
		result = prime * result + ((supportPauseResume == null) ? 0 : supportPauseResume.hashCode());
		result = prime * result + ((supportStandby == null) ? 0 : supportStandby.hashCode());
		result = prime * result + ((supportViewRequests == null) ? 0 : supportViewRequests.hashCode());
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
		Elements other = (Elements) obj;
		if (allowSuspectData == null) {
			if (other.allowSuspectData != null)
				return false;
		} else if (!allowSuspectData.equals(other.allowSuspectData))
			return false;
		if (applicationId == null) {
			if (other.applicationId != null)
				return false;
		} else if (!applicationId.equals(other.applicationId))
			return false;
		if (applicationName == null) {
			if (other.applicationName != null)
				return false;
		} else if (!applicationName.equals(other.applicationName))
			return false;
		if (maxMsgSize == null) {
			if (other.maxMsgSize != null)
				return false;
		} else if (!maxMsgSize.equals(other.maxMsgSize))
			return false;
		if (pingTimeout == null) {
			if (other.pingTimeout != null)
				return false;
		} else if (!pingTimeout.equals(other.pingTimeout))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (providePermissionExpressions == null) {
			if (other.providePermissionExpressions != null)
				return false;
		} else if (!providePermissionExpressions.equals(other.providePermissionExpressions))
			return false;
		if (providePermissionProfile == null) {
			if (other.providePermissionProfile != null)
				return false;
		} else if (!providePermissionProfile.equals(other.providePermissionProfile))
			return false;
		if (singleOpen == null) {
			if (other.singleOpen != null)
				return false;
		} else if (!singleOpen.equals(other.singleOpen))
			return false;
		if (supportBatchRequests == null) {
			if (other.supportBatchRequests != null)
				return false;
		} else if (!supportBatchRequests.equals(other.supportBatchRequests))
			return false;
		if (supportEnhancedSymbolList == null) {
			if (other.supportEnhancedSymbolList != null)
				return false;
		} else if (!supportEnhancedSymbolList.equals(other.supportEnhancedSymbolList))
			return false;
		if (supportOMMPost == null) {
			if (other.supportOMMPost != null)
				return false;
		} else if (!supportOMMPost.equals(other.supportOMMPost))
			return false;
		if (supportOptimizedPauseResume == null) {
			if (other.supportOptimizedPauseResume != null)
				return false;
		} else if (!supportOptimizedPauseResume.equals(other.supportOptimizedPauseResume))
			return false;
		if (supportPauseResume == null) {
			if (other.supportPauseResume != null)
				return false;
		} else if (!supportPauseResume.equals(other.supportPauseResume))
			return false;
		if (supportStandby == null) {
			if (other.supportStandby != null)
				return false;
		} else if (!supportStandby.equals(other.supportStandby))
			return false;
		if (supportViewRequests == null) {
			if (other.supportViewRequests != null)
				return false;
		} else if (!supportViewRequests.equals(other.supportViewRequests))
			return false;
		return true;
	}

}