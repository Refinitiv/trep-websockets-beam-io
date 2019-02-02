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

import java.util.HashMap;
import java.util.Map;

import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowWorkerLoggingOptions.WorkerLogLevelOverrides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.beam.sdk.options.PipelineOptionsFactory;

@SuppressWarnings("deprecation")
public abstract class Options {

	private static final Logger logger = LoggerFactory.getLogger(Options.class);
	
	static String[] defaults = new String[] {};
	
	public static ProjectOptions initStreaming() {
        PipelineOptionsFactory.register(ProjectOptions.class);
        ProjectOptions options = PipelineOptionsFactory
                .fromArgs(defaults)
                .withValidation()
                .create()
                .as(ProjectOptions.class);
        
        options.setAppName("StreamingPipeline");
        options.setRunner(DataflowRunner.class);

        Map<String, String> map = new HashMap<String, String>();
        map.put("com.refinitiv.beamio.trepwebsockets","DEBUG");       
        options.setWorkerLogLevelOverrides(WorkerLogLevelOverrides.from(map));
        logger.info(options.toString());
        return options;
    } 
}
