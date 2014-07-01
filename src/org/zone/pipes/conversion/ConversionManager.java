package org.zone.pipes.conversion;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.zone.pipes.PipesPlugin;
import org.zone.pipes.Util;
import org.zone.pipes.annotations.ConvertMethod;
import org.zone.pipes.errors.ConversionException;

public class ConversionManager{
	
	private PipesPlugin plugin;
	private List<ConversionRegistrant> registrations = new LinkedList<ConversionRegistrant>();
	@SuppressWarnings("rawtypes")
	private Map<Class<?>, List<Converter>> converters = null;
	
	public ConversionManager(PipesPlugin plugin){
		this.plugin = plugin;
		register(new DefaultConversionRegistrant(this.plugin), plugin);
	}
	
	public void register(ConversionRegistrant registrant, Plugin owner){
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
			for(final ConversionRegistrant r:registrations){
				for(final Method m:r.getClass().getMethods()){
					if(m.isAnnotationPresent(ConvertMethod.class) && (
							m.getParameterTypes().length == 1 || m.getParameterTypes().length == 2)){
						if(converters.get(m.getReturnType()) == null){
							converters.put(m.getReturnType(), new LinkedList<Converter>());
						}
						List<Converter> classConverters = converters.get(m.getReturnType());
						classConverters.add(new Converter(){

							@Override
							public Object convert(Object from, CommandSender context) throws ConversionException {
									try {
										if(m.getParameterTypes().length == 1)
											return m.invoke(r, from);
										else if(m.getParameterTypes()[1].isInstance(context))
											return m.invoke(r, from, context);
										else
											throw new ConversionException("Invalid converter for this context");
									} catch (InvocationTargetException e) {
										if(e.getCause() instanceof ConversionException){
											throw (ConversionException) e.getCause();
										}else{
											throw new ConversionException("Unknown error during conversion", e.getCause());
										}
									} catch (ConversionException e){
										throw e;
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

							@Override
							public boolean isStep() {
								return true;
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
		System.out.println(c.convert("1 2", Integer.class, null, false));
	}
	
	public <T> T convert(Object o, Class<T> target, CommandSender context) throws ConversionException{
		return this.convert(o, target, context, false).get(0);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> List<T> convert(Object o, final Class<T> target, CommandSender context, boolean multiple) throws ConversionException{
		Deque<ConversionStep> steps = new LinkedList<ConversionStep>();
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
					Object out = step.convert(o, context);
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
								if(c.isStep())
									steps.add(new ArrayConversionStep(c, step));
								else
									steps.addFirst(new ArrayConversionStep(c, step));
							}
						}
					}
				}else{
					steps.addFirst(new ArrayFirstChoiceStep(step));
				}
				List<Converter> converters = this.getConverters().get(step.getFrom());
				if(converters != null){
					for(Converter c:converters){
						if(!step.checkDuplicates(c.getFrom())){
							if(c.isStep())
								steps.add(new ConversionStep(c, step));
							else
								steps.addFirst(new ConversionStep(c, step));
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
	
	private class ArrayFirstChoiceStep extends ConversionStep{
		
		public ArrayFirstChoiceStep(ConversionStep next){
			super(null, next);
		}
		
		@Override
		public Object convert(Object o, CommandSender context) throws ConversionException{
			Object[] oa = (Object[]) o;
			if(oa.length > 0){
				return next.convert(oa[0], context);
			}else{
				throw new ConversionException("No " + oa.getClass().getComponentType().getSimpleName() + "s found.");
			}
		}
		
		public boolean checkDuplicates(Class<?> clazz){
			return next.checkDuplicates(clazz);
		}
		
		@SuppressWarnings("rawtypes")
		public Class getFrom(){
			return Array.newInstance(next.getFrom(), 0).getClass();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private class ArrayConversionStep extends ConversionStep{
		
		public ArrayConversionStep(Converter c, ConversionStep next){
			super(c, next);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Object convert(Object o, CommandSender context) throws ConversionException{
			Object[] oa = (Object[]) o;
			Object[] output = new Object[oa.length];
			for(int i = 0;i<oa.length;i++){
				try{
					output[i] = c.convert(oa[i], context);
				}catch(ConversionException e){
					throw e;
				}catch(Exception e){
					throw new ConversionException(e);
				}
			}
			return next.convert(output, context);
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
		public Object convert(Object o, CommandSender context) throws ConversionException{
			if(c==null){
				return o;
			}else{
				Object converted;
				try{
					converted = c.convert(o, context);
				}catch(ConversionException e){
					throw e;
				}catch(Exception e){
					throw new ConversionException(e);
				}
				return next.convert(converted, context);
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
	
}
