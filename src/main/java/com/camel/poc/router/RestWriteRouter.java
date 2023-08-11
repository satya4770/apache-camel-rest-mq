package com.camel.poc.router;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.camel.poc.dto.CustomerDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RestWriteRouter extends RouteBuilder {

	@Autowired
	private ReadSourceBean readSourceBean;

	@Autowired
	private ProcessSourceBean processSourceBean;
	
	@Autowired
	private JmsTemplate jmsTemplate;

	@Override
	public void configure() throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.setStreamCaching(true);
		camelRestSendRoute();
	}

	
	void camelRestSendRoute() {
		from("direct:TransForm").process(this::process);

		restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);
		
		rest()
		.consumes(MediaType.APPLICATION_JSON_VALUE)
		.produces(MediaType.TEXT_PLAIN_VALUE)
		.post("/registerUser")
		.to("direct:TransForm");
	}
	
	void process(Exchange exchange) throws JsonMappingException, JsonProcessingException {
		Message msg = exchange.getIn();
		ObjectMapper mapper = new ObjectMapper();
		CustomerDTO dto = mapper.readValue(msg.getBody().toString(), CustomerDTO.class);
		String alteredMsg = dto.getName()+" is succesfully registered with "+ dto.getEmail()+ " and " + dto.getMobile();
		jmsTemplate.convertAndSend("camel-active-mq", alteredMsg);
		msg.setBody(alteredMsg);
		exchange.setMessage(msg);
	}
	
	void counterDisplay(){
		from("timer:first-timer") 
		.bean(readSourceBean, "readSource")
		.bean(processSourceBean) 
		.to("log:first-timer");
	}
	void counterPushActiveMq(){
		from("timer:active-mq-timer?period=10000") 
		.bean(readSourceBean,"readSource") 
		.log("${body}") 
		.to("activemq:camel-active-mq");
	}
	void camelServletRoute() {
		from("rest:post:register")
		.process(new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
				Message msg = exchange.getMessage();
				msg.setBody("altered");
				exchange.setMessage(msg);
			}
		});
	}
	
	public void activeMQRouter() throws Exception {
		 from("activemq:camel-active-mq") 
		 .to("log:read from camel-active-mq");
	}
	

}


@Component
class ReadSourceBean {
	Integer counter = Integer.valueOf(0);

	public String readSource() {
		return "Message " + ++counter;
	}
}

@Component
class ProcessSourceBean {
	private Logger logger = LoggerFactory.getLogger(ProcessSourceBean.class);

	public void processSource(String message) {
		logger.info(message + " and processed");
	}
}
