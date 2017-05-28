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
package io.parrot.context;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.properties.DefaultPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.slf4j.Logger;

import io.parrot.config.IParrotConfigProperties;
import io.parrot.exception.ParrotException;
import io.parrot.processor.ParrotProcessor;
import io.parrot.processor.ParrotRouteBuilder;
import io.parrot.processor.ProcessorManager;
import io.parrot.utils.ParrotLogFormatter;

@ApplicationScoped
@Startup
@Default
@ContextName("parrot-context")
public class ParrotContext extends CdiCamelContext {

	@Inject
	Logger LOG;

	@Inject
	ProcessorManager processorManager;

	@Inject
	@ConfigProperty(name = IParrotConfigProperties.P_ZOOKEEPER_HOSTS, defaultValue = "localhost:2181")
	String zooKeeperHosts;

	@Inject
	@ConfigProperty(name = IParrotConfigProperties.P_KAFKA_BROKERS, defaultValue = "localhost:9092")
	String kafkaBrokers;

	@Inject
	@ConfigProperty(name = IParrotConfigProperties.P_DEBEZIUM_API_URL)
	String debeziumApiUrl;

	@Inject
	@ConfigProperty(name = IParrotConfigProperties.P_PARROT_API_URL)
	String parrotApiUrl;

	@Inject
	@ConfigProperty(name = IParrotConfigProperties.P_PARROT_NODE)
	String parrotIdNode;
	@PostConstruct
	void init() {
		LOG.info(ParrotLogFormatter.formatLog("Parrot Configuration", "Context Name", getName(), "Parrot ID Node",
				parrotIdNode, "Debezium Api URL", debeziumApiUrl, "Parrot Api URL", parrotApiUrl, "ZooKeeper Hosts",
				zooKeeperHosts, "Kafka Brokers", kafkaBrokers));
		try {
			setAutoStartup(true);

			List<ParrotProcessor> processors = processorManager.startUpProcessors();
			for (ParrotProcessor p : processors) {
				LOG.info("Adding processor " + p.getProcessor().getId() + "...");
				ParrotRouteBuilder processorRoute = p.getParrotRouteBuilder();
				try {
					addRoutes(processorRoute);
					LOG.info("Processor " + p.getProcessor().getId() + " added to context '" + getName() + "'.");
				} catch (Exception e) {
					LOG.error("Unable to add Processor '" + p.getProcessor().getId() + "': " + e.getMessage());
				}
			}

		} catch (Exception e) {
			throw new ParrotException(e.getMessage());
		}
	}

	/**
	 * Defines the "PropertiesComponent" used by Camel to lookup properties
	 */
	@Produces
	@ApplicationScoped
	@Named("properties")
	public PropertiesComponent properties(PropertiesParser parser) {

		PropertiesComponent component = new PropertiesComponent();

		/**
		 * Per dire a Camel di utilizzare DeltaSpike come configurazione per
		 * Camel CDI
		 */
		component.setPropertiesParser(parser);
		return component;
	}

	/**
	 * PropertiesParser used by DeltaSpike to resolve properties
	 */
	static class DeltaSpikeParser extends DefaultPropertiesParser {
		@Override
		public String parseProperty(String key, String value, Properties properties) {
			return ConfigResolver.getPropertyValue(key);
		}
	}

	@PreDestroy
	void cleanUp() {
	}

	public String getZookeeperHosts() {
		return zooKeeperHosts;
	}

	public String getKafkaBrokers() {
		return kafkaBrokers;
	}

	public String getDebeziumApiUrl() {
		return debeziumApiUrl;
	}

	public String getParrotApiUrl() {
		return parrotApiUrl;
	}

}