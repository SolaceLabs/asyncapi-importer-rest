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

public class MemoryAppender extends AppenderBase<ILoggingEvent> {

    private List<String> memoryLogList = new ArrayList<>();

    PatternLayoutEncoder encoder;

    String threadNameFilter;

    public MemoryAppender( PatternLayoutEncoder encoder, String threadNameFilter )
    {
        this.encoder = encoder;
        this.threadNameFilter = threadNameFilter;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (threadNameFilter == null || !threadNameFilter.contentEquals(eventObject.getThreadName())) {
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

    public List<String> getMemoryLogList() {
        return memoryLogList;
    }

    public void clear() {
        memoryLogList.clear();
    }

}
