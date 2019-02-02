# Example Google Dataflow (Apache Beam) pipeline


A Spring boot application that will start a Google Dataflow pipeline which uses the TrepWs-IO source to consume data from TREP and stream to BigQuery and/or PubSub.

A developer may customise this example or recreate a new pipeline to fulfil their specific requirements with additional beam-io transforms.

To run the pipeline unzip the `trepws-pipeline_1.0.0.zip` file, `cd` to the directory and type: `java -jar trepws-pipeline-1.0.0.jar`

Note: you may also need to set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to point to a service account key file when running this application.

The application configuration is in the config\spring.xml file (below).

```
    <bean name="streaming-options" factory-method="initStreaming" class="com.refinitiv.beamio.trepws.dataflow.Options">
        <property name="project"             value="YOUR GCP PROJECT NAME" />
        <property name="AppName"             value="StreamingPipeline"/>
        <property name="zone"                value="YOUR PREFFERED GCP ZONE" />
        <property name="region"              value="YOUR PREFFERED GCP REGION" />
        <property name="location"            value="YOUR PREFFERED GCP LOCATION" /> 
        <property name="numWorkers"          value="1" />
        <property name="maxNumWorkers"       value="1" />
        <property name="tempLocation"        value="YOUR GOOGLE STORAGE (gs://bucket/path)" />
        <!-- single CPU, 3.75 GB mem -->
        <property name="workerMachineType"   value="n1-standard-1" /> 
        <property name="diskSizeGb"          value="20" />
        <!-- <property name="usePublicIps"        value="true" /> -->
    </bean>
    
    <bean name="streaming"
        class="com.refinitiv.beamio.trepws.dataflow.PipelineBean">
        
        <!-- Setting a unique job name and re-running this app will start multiple pipelines -->
        <!-- You'll probably have to set a unique position too -->
        <property name="jobName"     value="trep-websocket-io" />
        
        <property name="hostname"    value="YOUR TREP/ADS hostname or IP address" />
        <property name="port"        value="15000" />
        
        <!-- The maximum number of ADS mounts (overridden if the number of  -->
        <!-- desired splits is smaller). If unset then 1 is used -->
        <property name="maxMounts"   value="4" />

        <property name="username"    value="YOUR TREP/DACS id/username" />
        <property name="timeout"     value="60000" />
        
        <!-- can default to 256 if property removed -->
        <!-- <property name="appId"       value="256" /> -->
        
        <!-- can default to local IP if property removed --> 
        <!-- <property name="position"    value="192.168.1.105" /> -->
 
        <!-- Note: please choose either one or both of the following two options! -->

        <!-- Removing this property will not add a PubSub publish step to the pipeline -->
        <!-- <property name="pubSub"      value="projects/YOUR GCP PROJECT NAME/topics/TOPIC TO PUBLISH TO" />  -->
               
        <!-- Table is create if needed and append. Removing this property will not add a BigQuery write step to the pipeline -->
        <!-- Note: the DATASET must exit! -->
        <property name="bigQuery" value="YOUR GCP PROJECT NAME:DATASET.TABLE" />
        
         <!-- Time partitioning field -->
        <property name="partition"      value="Time" />
        
        <!-- Table Schema -->
        <property name="schema">
            <map>
                <entry key="RIC"        value="STRING" />
                <entry key="Time"       value="TIMESTAMP" />
                <entry key="Type"       value="STRING" />
                <entry key="UpdateType" value="STRING" />
                <!-- As per fieldList -->
                <entry key="PROD_PERM"  value="STRING" />
                <entry key="DSPLY_NAME" value="STRING" />
                <entry key="BID"        value="FLOAT" />
                <entry key="ASK"        value="FLOAT" />
                <entry key="TRDPRC_1"   value="FLOAT" />
                <entry key="TRDVOL_1"   value="FLOAT" />
                <entry key="VALUE_DT1"  value="DATE" />
                <entry key="VALUE_TS1"  value="TIME" />
                <entry key="TRADE_DATE" value="DATE" />
                <entry key="TRDTIM_1"   value="TIME" />
            </map>
        </property>
    
        <!-- can default to the trep configured default service if property removed -->
        <!-- <property name="service"     value="hdEDD" /> --> 
        
        <!-- field list must align with the BigQuery database schema above -->
        <property name="fieldList">  
            <list>
                <value>PROD_PERM</value>
                <value>DSPLY_NAME</value>
                <value>BID</value>
                <value>ASK</value>
                <value>TRDPRC_1</value>
                <value>TRDVOL_1</value>               
                <value>VALUE_DT1</value>
                <value>VALUE_TS1</value>
                <value>TRADE_DATE</value>
                <value>TRDTIM_1</value>
            </list>
        </property>

        <!-- GBP= consolidated British Pound, .TRXVUSGOV10U 10 years US Govt Bond index, GB50YT=RR UK 50 years benchmark -->
        <property name="ricList">
            <list>
                <value>GBP=</value>
                <value>.TRXVUSGOV10U</value>
                <value>GB50YT=RR</value>
            </list>
        </property>
    </bean>
```