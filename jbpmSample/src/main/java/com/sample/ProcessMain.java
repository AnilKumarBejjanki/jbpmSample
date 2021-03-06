/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package com.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.process.core.event.EventFilter;
import org.jbpm.process.core.event.EventTypeFilter;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorManager;
import org.jbpm.test.JBPMHelper;
import org.jbpm.workflow.core.node.EventNode;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.conf.AuditMode;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.manager.context.EmptyContext;
/*import org.kie.internal.runtime.manager.deploy.DeploymentDescriptorManager;
import org.kie.test.util.db.PersistenceUtil;*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample file to launch a process.
 */
public class ProcessMain {

	private static final Logger logger = LoggerFactory.getLogger(ProcessMain.class);
	private static final boolean usePersistence = true;

	public static final void main(String[] args) throws Exception {
		// load up the knowledge base
		// KieBase kbase = readKnowledgeBase();
		// StatefulKnowledgeSession ksession = newStatefulKnowledgeSession(kbase);
		// start a new process instance
		// ksession.startProcess("com.sample.bpmn.hello");
		//fluentAPI();
		createEventWithFluent();
		logger.info("Process started ...");
	}

	private static KieBase readKnowledgeBase() throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(ResourceFactory.newClassPathResource("com/sample/sample.bpmn"), ResourceType.BPMN2);
		return kbuilder.newKieBase();
	}

	public static StatefulKnowledgeSession newStatefulKnowledgeSession(KieBase kbase) {
		RuntimeEnvironmentBuilder builder = null;
		if (usePersistence) {
			Properties properties = new Properties();
			properties.put("driverClassName", "org.h2.Driver");
			properties.put("className", "org.h2.jdbcx.JdbcDataSource");
			properties.put("user", "sa");
			properties.put("password", "");
			properties.put("url", "jdbc:h2:tcp://localhost/~/jbpm-db");
			properties.put("datasourceName", "jdbc/jbpm-ds");
			//PersistenceUtil.setupPoolingDataSource(properties);
			JBPMHelper.setupDataSource();
			JBPMHelper.startH2Server();
			//PersistenceUtil.
			Map<String, String> map = new HashMap<String, String>();
			map.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
			builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder().entityManagerFactory(emf);
		} else {
			builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultInMemoryBuilder();
			DeploymentDescriptor descriptor = new DeploymentDescriptorManager().getDefaultDescriptor().getBuilder()
					.auditMode(AuditMode.NONE).get();
			builder.addEnvironmentEntry("KieDeploymentDescriptor", descriptor);
		}
		builder.knowledgeBase(kbase);
		RuntimeManager manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get());
		return (StatefulKnowledgeSession) manager.getRuntimeEngine(EmptyContext.get()).getKieSession();
	}

	public static void fluentAPI() {
		RuleFlowProcessFactory factory =

				RuleFlowProcessFactory.createProcess("org.jbpm.HelloWorld");

		factory.name("HelloWorldProcess").version("1.0").packageName("org.jbpm").startNode(1).name("Start").done()
				.actionNode(2).name("Action").action("java", "System.out.println(\"Fluent API sample Process\");").done().endNode(3)
				.name("End").done().connection(1, 2).connection(2, 3);

		RuleFlowProcess process = factory.validate().getProcess();

		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

		kbuilder.add(ResourceFactory.newByteArrayResource(

				XmlBPMNProcessDumper.INSTANCE.dump(process).getBytes()), ResourceType.BPMN2);

		KieBase kbase = kbuilder.newKieBase();

		StatefulKnowledgeSession ksession = newStatefulKnowledgeSession(kbase);
		// start a new process instance
		ksession.startProcess("org.jbpm.HelloWorld");
		logger.info("Process started ...");

	}
	
	public static void createEventWithFluent() {
		
		 RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess("com.sample.process");
         factory
             .eventNode(4)
             .name("TimerEvent")
             .eventType("TimerXXX") // Place holder to be swapped later because "attachedTo" is unknown at this moment
             .done();
         
         RuleFlowProcess process = factory.validate().getProcess();
         
         // set meta data parameters and attach the filter for BoundaryTimer
         HumanTaskNode humanTaskNode = (HumanTaskNode)process.getNode(2);
         EventNode eventNode = (EventNode)process.getNode(4);
         
         String nodeAttachedTo = (String)humanTaskNode.getMetaData("UniqueId");
         
         String timeCycle = "3s###3s";
         
         eventNode.setMetaData("AttachedTo", nodeAttachedTo);
         eventNode.setMetaData("CancelActivity", false);
         eventNode.setMetaData("TimeCycle", timeCycle);
         EventTypeFilter eventFilter = new EventTypeFilter();
         eventFilter.setType("Timer-" + nodeAttachedTo + "-" + timeCycle);
         List<EventFilter> eventFilters = new ArrayList<EventFilter>();
         eventFilters.add(eventFilter);
         eventNode.setEventFilters(eventFilters);
         process.addNode(eventNode);
         
         KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

 		kbuilder.add(ResourceFactory.newByteArrayResource(

 				XmlBPMNProcessDumper.INSTANCE.dump(process).getBytes()), ResourceType.BPMN2);

 		KieBase kbase = kbuilder.newKieBase();

 		StatefulKnowledgeSession ksession = newStatefulKnowledgeSession(kbase);
 		// start a new process instance
 		ksession.startProcess("com.sample.process");
 		logger.info("Process started ...");
		
	}
}
