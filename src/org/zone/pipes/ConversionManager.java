package org.zone.pipes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.bukkit.plugin.Plugin;
import org.zone.pipes.annotations.ConvertMethod;
import org.zone.pipes.converters.Converter;
import org.zone.pipes.converters.SuperclassConverter;
import org.zone.pipes.errors.ConversionException;

public class ConversionManager implements PipesRegistrant{
	
	private PipesPlugin plugin;
	private List<PipesRegistrant> registrations = new LinkedList<PipesRegistrant>();
	private Map<Class<?>, List<Converter>> converters = null;
	
	public ConversionManager(PipesPlugin plugin){
		this.plugin = plugin;
		register(this, plugin);
	}
	
	public void register(PipesRegistrant registrant, Plugin owner){
		registrations.add(registrant);
		converters = null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<Class<?>, List<Converter>> getConverters(){
		if(converters != null){
			return converters;
		}else{
			converters = new HashMap<Class<?>, List<Converter>>();
			// Find converters in Registrants
			for(final PipesRegistrant r:registrations){
				for(final Method m:r.getClass().getMethods()){
					if(m.isAnnotationPresent(ConvertMethod.class) && m.getParameterTypes().length == 1){
						if(converters.get(m.getReturnType()) == null){
							converters.put(m.getReturnType(), new LinkedList<Converter>());
						}
						List<Converter> classConverters = converters.get(m.getReturnType());
						classConverters.add(new Converter(){

							@Override
							public Object convert(Object from) throws ConversionException {
								try {
									return m.invoke(r, from);
								} catch (Exception e) {
									throw new ConversionException("Error invoking conversion method", e);
								}
							}

							@Override
							public Class<?> getTo() {
								return m.getReturnType();
							}

							@Override
							public Class<?> getFrom() {
								return m.getParameterTypes()[0];
							}
							
						});
					}
				}
			}
			//Create superclass converters
			for(Map.Entry<Class<?>, List<Converter>> entry:new LinkedList<Map.Entry<Class<?>, List<Converter>>>(converters.entrySet())){
				for(Class<?> superClass:Util.getInheritance(entry.getKey())){
					Converter<?,?> c = new SuperclassConverter(entry.getKey(), superClass);
					if(converters.get(superClass) == null){
						converters.put(superClass, new LinkedList<Converter>());
					}
					List<Converter> classConverters = converters.get(superClass);
					classConverters.add(c);
				}
			}
			return converters;
		}
	}
	
	@ConvertMethod
	public Integer parseInt(String s){
		return Integer.parseInt(s);
	}
	
	@ConvertMethod
	public Float parseFloat(String s){
		return Float.parseFloat(s);
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ConversionException{
		ConversionManager c = new ConversionManager(null);
		System.out.println(c.convert("he", Number.class));
	}
	
	public <T> T convert(Object o, final Class<T> target) throws ConversionException{
		Queue<ConversionStep> steps = new LinkedList<ConversionStep>();
		steps.add(new ConversionStep(null, null){
			public Class getFrom(){
				return target;
			}
		});
		ConversionException latest = null;
		while(steps.size() > 0){
			ConversionStep step = steps.poll();
			if(step.getFrom().isAssignableFrom(o.getClass())){
				System.out.println(step.getFrom());
				try{
					return (T) step.convert(o);
				}catch(ConversionException ce){
					latest = ce;
				}
			}else{
				List<Converter> converters = this.getConverters().get(step.getFrom());
				if(converters == null)
					continue;
				for(Converter c:converters){
					if(!step.checkDuplicates(c.getFrom())){
						steps.add(new ConversionStep(c, step));
					}
				}
			}
		}
		throw latest;
	}
	
	@SuppressWarnings("rawtypes")
	private class ConversionStep{
		
		
		private Converter c;
		private ConversionStep next;
		
		public ConversionStep(Converter c, ConversionStep next){
			this.c = c;
			this.next = next;
		}
		
		@SuppressWarnings("unchecked")
		public Object convert(Object o) throws ConversionException{
			if(c==null){
				return o;
			}else{
				Object converted;
				try{
					converted = c.convert(o);
				}catch(ConversionException e){
					throw e;
				}catch(Exception e){
					throw new ConversionException(e);
				}
				return next.convert(converted);
			}
		}
		
		public boolean checkDuplicates(Class clazz){
			if(c==null)
				return false;
			return clazz.isAssignableFrom(c.getTo()) || next.checkDuplicates(clazz);
		}
		
		public Class getFrom(){
			return c.getFrom();
		}
	}
}
