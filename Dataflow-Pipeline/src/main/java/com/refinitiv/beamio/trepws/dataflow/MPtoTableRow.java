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

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import com.google.api.services.bigquery.model.TableRow;
import com.refinitiv.beamio.trepwebsockets.MarketPriceMessage;

public class MPtoTableRow extends DoFn<MarketPriceMessage,TableRow> {

	private static final long serialVersionUID = -8006588041424355423L;
	private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

    private Counter messages    = Metrics.counter(MPtoTableRow.class, "Rows");

	@ProcessElement
	public void processElement(ProcessContext context) {
		MarketPriceMessage mp = context.element();

		Instant timestamp = mp.getTimestamp();

		TableRow row = new TableRow();

		row.put("RIC",  mp.getName());
		row.put("Time", mp.getTimestamp().toString(fmt));
		row.put("Type", mp.getType());
		row.put("UpdateType", mp.getUpdateType());
		row.put("SeqNumber", mp.getSeqNumber());

		for (String fid : mp.getFields().keySet()) {
			row.put(fid, mp.getFields().get(fid));
		}
		context.outputWithTimestamp(row, timestamp);
		messages.inc();
	}
}


