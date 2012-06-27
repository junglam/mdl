package com.tdc.ws;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import com.tdc.sh.GreetingSH;

@WebService
@SOAPBinding(style=Style.RPC)
public interface HelloWS {
	
	@WebMethod(exclude=true)
	public void setGreetingSH(GreetingSH bean);
	
	@WebMethod
	public String sayHello(String s);
	
}
