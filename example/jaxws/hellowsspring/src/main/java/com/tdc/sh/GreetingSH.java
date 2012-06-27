package com.tdc.sh;

public class GreetingSH {
	
	public String sayHello(String s){
		if(s.equals("")){
			return "Hello there! Can I have your name, please?";
		}
		return "Hello, " + s + "!";
	}

}
