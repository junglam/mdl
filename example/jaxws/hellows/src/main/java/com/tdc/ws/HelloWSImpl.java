package com.tdc.ws;

import javax.jws.WebService;

@WebService(endpointInterface="com.tdc.ws.HelloWS")
public class HelloWSImpl implements HelloWS {
	
	public String sayHello(String s) {
		if(s.equals("")){
			return "Hello there";
		}
		return "Hello, " + s;
	}
}
