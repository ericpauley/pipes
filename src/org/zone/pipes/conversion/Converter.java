package org.zone.pipes.conversion;

import org.bukkit.command.CommandSender;
import org.zone.pipes.errors.ConversionException;

public interface Converter<From, To> {
	public To convert(From from, CommandSender context) throws ConversionException;
	public Class<To> getTo();
	public Class<From> getFrom();
	public boolean isStep();
}
