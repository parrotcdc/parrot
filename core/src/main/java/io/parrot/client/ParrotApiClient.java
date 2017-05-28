/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.parrot.client;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.core.api.config.ConfigProperty;

import io.parrot.api.model.ParrotProcessorApi;
import io.parrot.api.model.ParrotProcessorNodeApi;
import io.parrot.config.IParrotConfigProperties;
import io.parrot.debezium.connectors.Error;
import io.parrot.exception.GenericApiException;
import io.parrot.exception.ParrotApiException;
import io.parrot.exception.ParrotException;
import io.parrot.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class ParrotApiClient {

	@Inject
	ApplicationMessages message;

	@Inject
	@ConfigProperty(name = IParrotConfigProperties.P_PARROT_API_URL)
	String parrotApiUrl;

	ParrotApiService parrotService;

	/**
	 * Get the list of Processors
	 * 
	 * @return The list of Processors
	 */
	public List<String> getProcessors() throws ParrotApiException {
		try {
			return getParrotService().getProcessors().execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pId
	 *            A Processor ID
	 * @return The Processor
	 * @throws ParrotApiException
	 */
	public ParrotProcessorApi getProcessor(String pId) throws ParrotApiException {
		try {
			return getParrotService().getProcessor(pId).execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pProcessor
	 *            A Processor to add
	 * @return The added Processor
	 * @throws ParrotApiException
	 */
	public ParrotProcessorApi addProcessor(ParrotProcessorApi pProcessor) throws ParrotApiException {
		try {
			Response<ParrotProcessorApi> response = getParrotService().addProcessor(pProcessor).execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				Error error = JsonUtils.byteArrayToObject(response.errorBody().bytes(), Error.class);
				throw new ParrotException(message.parrotApiAddProcessorError(pProcessor.getId(), error.message));
			}
		} catch (Exception e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pId
	 *            A Processor ID
	 * @return The Processor cluster
	 * @throws ParrotApiException
	 */
	public List<ParrotProcessorNodeApi> getProcessorCluster(String pId) throws ParrotApiException {
		try {
			return getParrotService().getProcessorCluster(pId).execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pId
	 *            A Processor ID to start
	 * @return The started Processor
	 * @throws ParrotApiException
	 */
	public ParrotProcessorApi startProcessor(String pId) throws ParrotApiException {
		try {
			return getParrotService().startProcessor(pId).execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pId
	 *            A Processor ID to stop
	 * @return The stopped Processor
	 * @throws ParrotApiException
	 */
	public ParrotProcessorApi stopProcessor(String pId) throws ParrotApiException {
		try {
			return getParrotService().stopProcessor(pId).execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pId
	 *            A Processor ID to restart
	 * @return The restarted Processor
	 * @throws ParrotApiException
	 */
	public ParrotProcessorApi restartProcessor(String pId) throws ParrotApiException {
		try {
			return getParrotService().restartProcessor(pId).execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * @param pId
	 *            A Processor ID to delete
	 * @return
	 * @throws ParrotApiException
	 */
	public ResponseBody deleteProcessor(String pId) throws ParrotApiException {
		try {
			return getParrotService().deleteProcessor(pId).execute().body();
		} catch (IOException e) {
			throw new GenericApiException(e.getMessage());
		}
	}

	/**
	 * Private
	 * 
	 * @return A Parrot service instance
	 */
	ParrotApiService getParrotService() {
		try {
			if (parrotService == null) {
				HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
				logging.setLevel(Level.BODY);
				OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
				httpClient.addInterceptor(logging);
				Retrofit retrofit = new Retrofit.Builder().baseUrl(parrotApiUrl).client(httpClient.build())
						.addConverterFactory(JacksonConverterFactory.create()).build();
				parrotService = retrofit.create(ParrotApiService.class);
			}
		} catch (Exception e) {
			throw new ParrotException(e.getMessage(), e);
		}
		return parrotService;
	}
}