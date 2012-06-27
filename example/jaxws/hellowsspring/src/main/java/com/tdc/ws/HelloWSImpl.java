package com.tdc.ws;

import javax.jws.WebService;


import com.tdc.sh.GreetingSH;

@WebService(endpointInterface="com.tdc.ws.HelloWS")
public class HelloWSImpl implements HelloWS {
	
	// inject bean
	private GreetingSH greetingSH;
		
	public void setGreetingSH(GreetingSH bean){
		greetingSH = bean;
	}
	
	public String sayHello(String s) {
		return greetingSH.sayHello(s);
	}
}
