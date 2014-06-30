package org.zone.pipes.converters;

import org.zone.pipes.errors.ConversionException;

public interface Converter<From, To> {
	public To convert(From from) throws ConversionException;
	public Class<To> getTo();
	public Class<From> getFrom();
}
