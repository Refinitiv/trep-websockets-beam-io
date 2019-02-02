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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ImportResource("file:config/spring.xml")
@PropertySource("classpath:application.properties")
@EnableAutoConfiguration()

public class TrepPipeline implements CommandLineRunner {

    public static final Logger logger = LoggerFactory.getLogger(TrepPipeline.class);

    @Autowired
    private PipelineBean pipelineBean;

	public static void main(String[] args) {
	  	SpringApplication spring = new SpringApplication(TrepPipeline.class);
    	spring.setBannerMode(Banner.Mode.LOG);
    	spring.run(args);
	}

	public void run(String... args) throws Exception {
		logger.info("Starting...");
		pipelineBean.runPipeline(args);
	}
}
