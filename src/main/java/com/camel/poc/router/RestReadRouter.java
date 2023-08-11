package com.camel.poc.router;

import javax.jms.JMSException;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

@Component
public class RestReadRouter extends RouteBuilder {

	
	@Autowired
	private JmsTemplate jmsTemplate;

	@Override
	public void configure() throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.setStreamCaching(true);
		camelRestSendRoute();
	}

	
	void camelRestSendRoute() {
		from("direct:retriveUser").process(this::processRead);

		restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);
		
		rest()
		.produces(MediaType.TEXT_PLAIN_VALUE)
		.get("/showUser")
		.to("direct:retriveUser");
	}
	
	
	void processRead(Exchange exchange) throws JsonMappingException, JsonProcessingException, JMSException {
		ActiveMQTextMessage msg = (ActiveMQTextMessage)jmsTemplate. receive("camel-active-mq");
		System.out.println(msg.getText());
		Message restMsg = exchange.getMessage();
		restMsg.setBody(msg.getText());
		exchange.setMessage(restMsg);
	}

}
