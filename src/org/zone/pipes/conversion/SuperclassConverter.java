package org.zone.pipes.conversion;

import org.bukkit.command.CommandSender;

public class SuperclassConverter<From, To> implements Converter<Object, To> {

	private Class<To> to;
	private Class<From> from;
	
	public SuperclassConverter(Class<From> from, Class<To> to){
		if(!to.isAssignableFrom(from)){
			throw new IllegalArgumentException(from + " must be a subclass of " + to);
		}
		this.to = to;
		this.from = from;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public To convert(Object from, CommandSender context) {
		return (To) from;
	}

	@Override
	public Class<To> getTo() {
		return this.to;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<Object> getFrom() {
		return (Class<Object>) this.from;
	}

	@Override
	public boolean isStep() {
		return false;
	}

}
