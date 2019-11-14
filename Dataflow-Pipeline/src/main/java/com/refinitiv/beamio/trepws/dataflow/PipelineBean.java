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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.refinitiv.beamio.trepwebsockets.MarketPriceMessage;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO;
import com.refinitiv.beamio.trepwebsockets.TrepWsIO.Read;

public class PipelineBean {
	private static final Logger logger = LoggerFactory.getLogger(PipelineBean.class);

	private ProjectOptions options;

	private int maxMounts = 1;
	private int port = 15000;
	private String appId = "256";
	private String hostname;
	private String position;
	private String service = null;
	private String username;
    private boolean serviceDiscovery = false;
    private boolean tokenAuth = false;
    private String password;
    private String region = null;

    private Set<String> cachedFields = Sets.newHashSet();
	private List<String> ricList = null;
	private List<String> fieldList = null;
	private Map<String,String> schema = null;
	private String partition;
	private String ricListQuery;
	private int timeout = 60000;

	private String jobName;
	private String bigQuery;
	private String pubSub;

	private static BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

	public PipelineBean(ProjectOptions options) {
		this.options = options;
		options.setStreaming(true);
		options.setStableUniqueNames(CheckEnabled.OFF);

		logger.info(options.getProject());
	}

	public void runPipeline(String[] args) throws JobException, InterruptedException {

		options.setJobName(getJobName());
		logger.info(String.format("runPipeline %s %s...", options.getAppName(), options.getJobName()));
		logger.info(this.toString());

        options.setFilesToStage(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)).stream()
                .map(entry -> new File(entry).toString()).collect(Collectors.toList()));

        List<String> rics = Lists.newArrayList();

        try {

            if (getRicList().isEmpty()) {

                QueryJobConfiguration queryConfig = QueryJobConfiguration
                    .newBuilder(getRicListQuery()).build();

                for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
                    for (FieldValue val : row) {
                        rics.add(val.getStringValue());
                    }
                }
            } else {
                rics = Lists.newArrayList(getRicList());
            }
        } catch (Exception e1) {
            logger.error("Error reading from BigQuery {}",e1);
            System.exit(1);
        }

		Pipeline pipeline = Pipeline.create(options);

		InstrumentTuple instrumentTuple = InstrumentTuple.of(getService(), rics , getFieldList());
		logger.info("InstrumentTuple " + rics);

		Read<MarketPriceMessage> trepIO = TrepWsIO.read()
				.withHostname(getHostname())
				.withPassword(getPassword())
				.withTokenAuth(isTokenAuth())
                .withTokenStore(options.getGcpTempLocation())
				.withServiceDiscovery(isServiceDiscovery())
				.withPort(getPort())
				.withMaxMounts(getMaxMounts())
				.withUsername(getUsername())
				.withInstrumentTuples(Lists.newArrayList(instrumentTuple))
				.withCachedFields(getCachedFields())
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

            logger.info(String.format("Table Schema:%s, Partition:%s", schema, partition));

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

			logger.info("Streaming to BigQuery");
		}

		if (getPubSub() != null) {

			messages
				.apply("MarketPrice to String", ParDo.of(new MPtoString()))

				.apply("Send PubSub Message", PubsubIO.writeStrings()
						.to(getPubSub()));

			logger.info("Streaming to PubSub");
		}

        // The options.setUpdate(true) seems to create two pipelines. This can cause
        // problems with multiple tokens being created and stored when using ERT.
        // Therefore, please cancel the running pipeline and start a new one.
		options.setUpdate(false);
		pipeline.run(options);

		System.out.println("Done...");
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

	public Set<String> getCachedFields() {
        return cachedFields;
    }

    public void setCachedFields(Set<String> cachedFields) {
        this.cachedFields = cachedFields;
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

    public String getRicListQuery() {
        return ricListQuery;
    }

    public void setRicListQuery(String ricListQuery) {
        this.ricListQuery = ricListQuery;
    }

    public boolean isTokenAuth() {
        return tokenAuth;
    }

    public void setTokenAuth(boolean tokenAuth) {
        this.tokenAuth = tokenAuth;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isServiceDiscovery() {
        return serviceDiscovery;
    }

    public void setServiceDiscovery(boolean serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return String.format(
                "PipelineBean [hostname=%s, username=%s, port=%s, appId=%s, position=%s, maxMounts=%s, service=%s, serviceDiscovery=%s, tokenAuth=%s, region=%s, cachedFields=%s, ricList=%s, fieldList=%s, schema=%s, partition=%s, ricListQuery=%s, timeout=%s]",
                hostname, username, port, appId, position, maxMounts, service, serviceDiscovery, tokenAuth, region,
                cachedFields, ricList, fieldList, schema, partition, ricListQuery, timeout);
    }

}
