package org.zone.pipes.converters;

public abstract class Parser<To> implements Converter<String, To> {

	private Class<To> to;
	
	public Parser(Class<To> to){
		this.to = to;
	}
	
	@Override
	public abstract To convert(String from);

	@Override
	public Class<To> getTo() {
		return to;
	}

	@Override
	public Class<String> getFrom() {
		return String.class;
	}

}
