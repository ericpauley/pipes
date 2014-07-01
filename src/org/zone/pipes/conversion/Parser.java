package org.zone.pipes.conversion;

import org.bukkit.command.CommandSender;

public abstract class Parser<To> implements Converter<String, To> {

	private Class<To> to;
	
	public Parser(Class<To> to){
		this.to = to;
	}
	
	@Override
	public abstract To convert(String from, CommandSender context);

	@Override
	public Class<To> getTo() {
		return to;
	}

	@Override
	public Class<String> getFrom() {
		return String.class;
	}

}
