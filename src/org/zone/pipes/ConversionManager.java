package org.zone.pipes;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	@SuppressWarnings("rawtypes")
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
			converters = new LinkedHashMap<Class<?>, List<Converter>>();
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
									} catch (InvocationTargetException e) {
										if(e.getCause() instanceof ConversionException){
											throw (ConversionException) e.getCause();
										}else{
											throw new ConversionException("Unknown error during conversion", e.getCause());
										}
									} catch (Exception e) {
										throw new ConversionException("Unknown error during conversion", e.getCause());
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
				if(!entry.getKey().isArray()){
					for(Class<?> superClass:Util.getInheritance(entry.getKey())){
						Converter<?,?> c = new SuperclassConverter(entry.getKey(), superClass);
						if(converters.get(superClass) == null){
							converters.put(superClass, new LinkedList<Converter>());
						}
						List<Converter> classConverters = converters.get(superClass);
						classConverters.add(c);
					}
				}
			}
			return converters;
		}
	}
	
	public static void main(String[] args) throws ConversionException{
		ConversionManager c = new ConversionManager(null);
		System.out.println(c.convert("1 2 3", Integer.class, true));
	}
	
	public <T> T convert(Object o, Class<T> target) throws ConversionException{
		return this.convert(o, target, false).get(0);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> List<T> convert(Object o, final Class<T> target, boolean multiple) throws ConversionException{
		Queue<ConversionStep> steps = new LinkedList<ConversionStep>();
		steps.add(new ConversionStep(null, null){
			public Class getFrom(){
				return target;
			}
		});
		if(multiple){
			steps.add(new ConversionStep(null, null){
				public Class getFrom(){
					return Array.newInstance(target, 0).getClass();
				}
			});
		}
		System.out.println(steps.peek().getFrom());
		ConversionException latest = null;
		while(steps.size() > 0){
			ConversionStep step = steps.poll();
			if(step.getFrom().isAssignableFrom(o.getClass())){
				try{
					Object out = step.convert(o);
					List<T> output = new ArrayList<T>();
					if(out.getClass().isArray()){
						for(Object item:(Object[])out){
							output.add((T) item);
						}
					}else{
						output.add((T) out);
					}
					return output;
				}catch(ConversionException ce){
					latest = ce;
				}
			}else{
				if(step.getFrom().isArray()){
					List<Converter> converters = this.getConverters().get(step.getFrom().getComponentType());
					if(converters != null){
						for(Converter c:converters){
							if(!step.checkDuplicates(c.getFrom()) && !c.getTo().isArray()){
								steps.add(new ArrayConversionStep(c, step));
							}
						}
					}
				}
				List<Converter> converters = this.getConverters().get(step.getFrom());
				if(converters != null){
					for(Converter c:converters){
						if(!step.checkDuplicates(c.getFrom())){
							steps.add(new ConversionStep(c, step));
						}
					}
				}
			}
		}
		if(latest != null){
			throw latest;
		}else{
			throw new ConversionException("No conversion found for argument");
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	private class ArrayConversionStep extends ConversionStep{
		
		public ArrayConversionStep(Converter c, ConversionStep next){
			super(c, next);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Object convert(Object o) throws ConversionException{
			Object[] oa = (Object[]) o;
			Object[] output = new Object[oa.length];
			for(int i = 0;i<oa.length;i++){
				try{
					output[i] = c.convert(oa[i]);
				}catch(ConversionException e){
					throw e;
				}catch(Exception e){
					throw new ConversionException(e);
				}
			}
			return next.convert(output);
		}
		
		public boolean checkDuplicates(Class<?> clazz){
			if(c==null)
				return false;
			return clazz.isAssignableFrom(c.getTo()) || next.checkDuplicates(clazz);
		}
		
		public Class getFrom(){
			return Array.newInstance(c.getFrom(), 0).getClass();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private class ConversionStep{
		
		protected Converter c;
		protected ConversionStep next;
		
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
		
		public boolean checkDuplicates(Class<?> clazz){
			if(c==null)
				return false;
			return clazz.isAssignableFrom(c.getTo()) || next.checkDuplicates(clazz);
		}
		
		public Class getFrom(){
			return c.getFrom();
		}
	}
	
	@ConvertMethod
	public Double parseDouble(String s) throws ConversionException{
		try{
			return Double.parseDouble(s);
		}catch(NumberFormatException e){
			throw new ConversionException('"'+s + "\" is not a number");
		}
			
	}
	
	@ConvertMethod
	public Float parseFloat(String s) throws ConversionException{
		try{
			return Float.parseFloat(s);
		}catch(NumberFormatException e){
			throw new ConversionException('"'+s + "\" is not a number");
		}
	}
	
	@ConvertMethod
	public Integer parseInt(String s) throws ConversionException{
		try{
			return Integer.parseInt(s);
		}catch(NumberFormatException e){
			throw new ConversionException('"'+s + "\" is not a number");
		}
	}
	
	@ConvertMethod
	public Long parseLong(String s) throws ConversionException{
		try{
			return Long.parseLong(s);
		}catch(NumberFormatException e){
			throw new ConversionException('"'+s + "\" is not a number");
		}
	}
	
	@ConvertMethod
	public String[] split(String s){
		return s.split(" ");
	}
	
}
