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

package com.solace.ep.asyncapi.rest.apis;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.solace.cloud.ep.designer.ApiClient;
import com.solace.cloud.ep.designer.ApiException;
import com.solace.cloud.ep.designer.api.ApplicationDomainsApi;
import com.solace.cloud.ep.designer.auth.HttpBearerAuth;
import com.solace.cloud.ep.designer.model.ApplicationDomainsResponse;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportAppDomainResponse;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportAppDomainResponse.AppDomainItem;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * This class provides methods for making calls to the Solace Cloud API outside of those
 * used to create/update Event Portal objects
 */
@Slf4j
public class SolaceCloudApiCalls {

    /**
     * Validate Event Portal Token
     * @param epToken - Token to validate as String
     * @param cloudApiBaseUrl - Solace Cloud API base URL
     * @return - ResponseEntity object to return as result of POST method
     */
    public static ResponseEntity<AsyncApiImportResponse> validateEpToken(
        final String epToken,
        final String cloudApiBaseUrl
    )
    {
        final String CLOUD_TOKEN_VALIDATION_CONTEXT = "/api/v0/token/permissions";
        final String cloudApiTokenValidationUrl = cloudApiBaseUrl + CLOUD_TOKEN_VALIDATION_CONTEXT;
        AsyncApiImportResponse response = new AsyncApiImportResponse();
        HttpStatus httpStatus = null;
        String responseMessage = null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(epToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.exchange(cloudApiTokenValidationUrl, HttpMethod.GET, entity, String.class);
            if (apiResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Successful Token Validation; HTTP Response: {}", apiResponse.getStatusCode().toString());
                responseMessage = "SUCCESS";
                httpStatus = HttpStatus.OK;
            } else {
                log.warn("Token Validation Failed; HTTP Response Code: {}; Message: {}", apiResponse.getStatusCode().toString(), apiResponse.getBody());
                responseMessage = "Token Failed Validation";
                httpStatus = HttpStatus.valueOf(apiResponse.getStatusCode().value());
            }
        } catch (Exception exc) {
            responseMessage = redactBearerTokenFromMessage(exc.getLocalizedMessage());
            log.error("AsyncApiImportController.validateToken: {}", responseMessage);
            httpStatus = HttpStatus.UNAUTHORIZED;
        }
        response.getMsgs().add(responseMessage == null ? "Unknown Status" : responseMessage);
        return new ResponseEntity<AsyncApiImportResponse>(response, httpStatus);
    }

    private static String redactBearerTokenFromMessage(final String msg)
    {
        final int bearerTokenPositionInMsg = msg.indexOf(" Bearer ");
        if (bearerTokenPositionInMsg > 0) {
            // Redact Token in error message
            return msg.substring(0, bearerTokenPositionInMsg);
        }
        return msg;
    }

    /**
     * Calls Solace Cloud API using EP bearer token passed as parameter, returns a list of
     * application domains as ID-Name elements
     * @param epToken
     * @param resolvedUrl
     * @return ResponseEntity to return
     */
    public static ResponseEntity<AsyncApiImportAppDomainResponse> getAppDomainsFromSolaceCloudApi(
        String epToken,
        String resolvedUrl
    )
    {
        final ApiClient apiClient = getApiClient(epToken, resolvedUrl);
        final ApplicationDomainsApi applicationDomainsApi = new ApplicationDomainsApi(apiClient);
        final AsyncApiImportAppDomainResponse response = new AsyncApiImportAppDomainResponse();

        try {
            int maxPages = 1;
            for (int page = 1; page <= maxPages; page++) {
                ApplicationDomainsResponse appDomainResponse = applicationDomainsApi.getApplicationDomains(20, page, null, null, null);
                if (appDomainResponse.getData().isEmpty()) {
                    break;
                }
                if (page == 1 && appDomainResponse.getMeta().getPagination().getNextPage() != null) {
                    maxPages = appDomainResponse.getMeta().getPagination().getTotalPages();
                }
                appDomainResponse.getData().forEach( appDomain -> {
                    response.getApplicationDomains().add(new AppDomainItem(appDomain.getId(), appDomain.getName()));
                } );
            }
        } catch (ApiException apiException) {
            log.error("SolaceCloudApiCalls.getAppDomainsFromSolaceCloudApi failed; Code: {}; Message: '{}'", 
                        apiException.getCode(), redactBearerTokenFromMessage(apiException.getMessage()));
            AsyncApiImportAppDomainResponse apiExcResponse = new AsyncApiImportAppDomainResponse();
            response.getMsgs().add(apiException.getMessage());
            return new ResponseEntity<>(apiExcResponse, HttpStatusCode.valueOf(apiException.getCode()));
        } catch (Exception exc) {
            log.error("SolaceCloudApiCalls.getAppDomainsFromSolaceCloudApi failed; Error: {}", redactBearerTokenFromMessage(exc.getMessage()));
            AsyncApiImportAppDomainResponse excResponse = new AsyncApiImportAppDomainResponse();
            excResponse.getMsgs().add(redactBearerTokenFromMessage(exc.getMessage()));
            return new ResponseEntity<>(excResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get Solace Cloud ApiClient object using Solace Cloud API endpoint and EP bearer
     * token passed as parameters
     * @param epToken
     * @param resolvedUrl
     * @return
     */
    public static ApiClient getApiClient(
        String epToken,
        String resolvedUrl
    ) 
    {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(resolvedUrl);
        HttpBearerAuth apiToken = (HttpBearerAuth)apiClient.getAuthentication("APIToken");
        apiToken.setBearerToken(epToken);
        return apiClient;
    }
}
