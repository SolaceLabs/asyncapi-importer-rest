/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solace.ep.asyncapi.rest.log;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Custom MemoryAppender class extended from Logback base
 * The purpose is to instantiate a MemoryAppender object for each invokation of
 * the importer POST method to capture activity and report back to the calling app.
 */
public class MemoryAppender extends AppenderBase<ILoggingEvent> {

    private List<String> memoryLogList = new ArrayList<>();

    private PatternLayoutEncoder encoder;

    private String threadNameFilter;

    private String threadPrefixFilter;

    /**
     * Constructor
     * @param encoder - Encoder instantiated for the application instance
     * @param threadNameFilter - The name of the thread for which logs should be captured
     */
    public MemoryAppender( PatternLayoutEncoder encoder, String threadNameFilter, String threadPrefixFilter )
    {
        this.encoder = encoder;
        this.threadNameFilter = threadNameFilter;
        this.threadPrefixFilter = threadPrefixFilter;
    }

    /**
     * Append method called by logger. Filters log entries by matching the producing thread against
     * the threadNameFilter passed in the constructor.
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (threadNameFilter == null) {
            return;
        } else  if (!eventObject.getThreadName().equals(threadNameFilter) &&
                   (threadPrefixFilter == null || !eventObject.getThreadName().startsWith(threadPrefixFilter))) {
            return;
        }
        try {
            final byte[] encodedMsg = encoder.encode(eventObject);
            final String formattedMessage = new String(encodedMsg);

            memoryLogList.add(formattedMessage);
        } catch (Exception e) {
            addError("Error encoding log message", e);
        }
    }

    /**
     * Get the logs captured in memory for this appender
     * @return
     */
    public List<String> getMemoryLogList() {
        return memoryLogList;
    }

    /**
     * Clear the logs captured in memory for this appender
     */
    public void clear() {
        memoryLogList.clear();
    }

}
