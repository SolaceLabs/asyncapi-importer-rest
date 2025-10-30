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

package com.solace.ep.asyncapi.rest.utils;

import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import com.solace.ep.asyncapi.rest.log.MemoryAppender;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

/**
 * Class containing static functions to support logging. Specifically, log
 * messages captured to report back in the HTTP ResponseEntity for the current
 * operation.
 */
public class LogUtils {

    /**
     * Get LoggerContext
     * @return
     */
    public static LoggerContext getContext()
    {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    /**
     * Get PatternLayoutEncoder (Log Layout) to use. Intended for use with
     * custom log MemoryAppender.
     * @param context
     * @param patternString
     * @return
     */
    public static PatternLayoutEncoder getMemoryEncoder(
        LoggerContext context,
        String patternString
    )
    {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern(patternString);
        encoder.start();
        return encoder;
    }

    /**
     * Instantiate the memory appender for the current thread
     * @param context
     * @param encoder
     * @param threadNameFilter
     * @return
     */
    public static MemoryAppender getMemoryAppender(
        LoggerContext context,
        PatternLayoutEncoder encoder,
        String threadNameFilter,
        String threadPrefixFilter
    )
    {
        MemoryAppender memoryAppender = new MemoryAppender(encoder, threadNameFilter, threadPrefixFilter);
        memoryAppender.setContext(context);
        memoryAppender.start();
        return memoryAppender;
    }
}
