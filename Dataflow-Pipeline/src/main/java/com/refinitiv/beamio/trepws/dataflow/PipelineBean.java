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
package com.refinitiv.beamio.trepws.dataflow;

import static com.refinitiv.beamio.trepwebsockets.TrepWsIO.InstrumentTuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.InsertRetryPolicy;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptions.CheckEnabled;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.api.services.bigquery.model.TimePartitioning;
import com.google.common.collect.Lists;
import com.refinitiv.beamio.trepwebsockets.MarketPriceMessage;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO.Read;

public class PipelineBean {
	private static final Logger logger = LoggerFactory.getLogger(PipelineBean.class);

	private ProjectOptions options;

	private String hostname;
	private int port = 15000;
	private int maxMounts = 1;
	private String username;
	private String position;
	private String appId = "256";
	private String service = null;
	private List<String> ricList = null;
	private List<String> fieldList = null;
	private Map<String,String> schema = null;
	private String partition;
	private int timeout = 60000;
	
	private String jobName;
	private String bigQuery;
	private String pubSub;
	
	public PipelineBean(ProjectOptions options) {
		this.options = options;
		options.setStreaming(true);
		options.setStableUniqueNames(CheckEnabled.OFF);
		
		logger.info(options.getProject());
	}

	public void runPipeline(String[] args) {
		
		options.setJobName(getJobName());
		
		logger.info("runPipeline {} {}...", options.getAppName(), options.getJobName());
		logger.info(this.toString());
		
		InstrumentTuple instrumentTuple = InstrumentTuple.of(getService(), getRicList(), getFieldList());

		Pipeline pipeline = Pipeline.create(options);

		Read<MarketPriceMessage> trepIO = TrepWsIO.read()
				.withHostname(getHostname())
				.withPort(getPort())
				.withMaxMounts(getMaxMounts())
				.withUsername(getUsername())
				.withInstrumentTuples(Lists.newArrayList(instrumentTuple))
				.withTimeout(getTimeout());
		
		if (getPosition() != null) {
			trepIO = trepIO.withPosition(getPosition());
		}

		if (getAppId() != null) {
			trepIO = trepIO.withAppId(getAppId());
		}
				
		PCollection<MarketPriceMessage> messages = pipeline.apply("TrepWsIO", trepIO);

		if (getBigQuery() != null) {
		    
            List<TableFieldSchema> fields = new ArrayList<>();
            for (Entry<String, String> map : getSchema().entrySet()) {
                fields.add(new TableFieldSchema().setName(map.getKey()).setType(map.getValue()));
            }
            TableSchema schema = new TableSchema().setFields(fields);

            TimePartitioning partition = new TimePartitioning().setField(getPartition()).setType("DAY");
            
	        logger.info("Table Schema:{}, Partition:{}", schema, partition);
	        
			messages
				.apply("MarketPrice to TableRow", ParDo.of(new MPtoTableRow()))
	
				.apply("Write to BigQuery",
					BigQueryIO.writeTableRows()
						.to(getBigQuery())
						.withFailedInsertRetryPolicy(InsertRetryPolicy.retryTransientErrors())
						.withTimePartitioning(partition)
						.withSchema(schema)
						.withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
						.withWriteDisposition(WriteDisposition.WRITE_APPEND));
		}
		
		if (getPubSub() != null) {
			messages
				.apply("MarketPrice to String", ParDo.of(new MPtoString()))
			
				.apply("Send PubSub Message", PubsubIO.writeStrings()
						.to(getPubSub()));
		}

		try {
			logger.info("Pipeline {} is being updated", options.getJobName());
			options.setUpdate(true);
			pipeline.run(options);
			
		} catch (Exception e) {
			logger.info("Pipeline {}, running a new one", e.getMessage(), options.getJobName());
			options.setUpdate(false);
			pipeline.run(options);
		}
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMaxMounts() {
        return maxMounts;
    }

    public void setMaxMounts(int maxMounts) {
        this.maxMounts = maxMounts;
    }

    public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public List<String> getRicList() {
		return ricList;
	}

	public void setRicList(List<String> ricList) {
		this.ricList = ricList;
	}

	public List<String> getFieldList() {
		return fieldList;
	}

	public void setFieldList(List<String> fieldList) {
		this.fieldList = fieldList;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(String timeout) {
		try {
			this.timeout = Integer.parseInt(timeout);
		} catch (NumberFormatException e) {
			logger.warn("Unable to set timeout defaulting to {}", this.timeout);
		}
	}

	public String getBigQuery() {
		return bigQuery;
	}

	public void setBigQuery(String bigQuery) {
		this.bigQuery = bigQuery;
	}

	public String getPubSub() {
		return pubSub;
	}

	public void setPubSub(String pubSub) {
		this.pubSub = pubSub;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public Map<String,String> getSchema() {
        return schema;
    }

    public void setSchema(Map<String,String> schema) {
        this.schema = schema;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    @Override
	public String toString() {
		return "PipelineBean [" + (hostname != null ? "hostname=" + hostname + ", " : "")
				+ ("port=" + port) + (username != null ? "username=" + username + ", " : "")
				+ (position != null ? "position=" + position + ", " : "")
				+ (appId != null ? "appId=" + appId + ", " : "") + (service != null ? "service=" + service + ", " : "")
				+ (bigQuery != null ? "bigQuery=" + bigQuery + ", " : "")
				+ (pubSub != null ? "pubSub=" + pubSub + ", " : "")
				+ (fieldList != null ? "fieldList=" + fieldList + ", " : "") + "timeout=" + timeout + ", "
				+ (ricList != null ? "ricList=" + ricList : "") + "]";
	}
}
