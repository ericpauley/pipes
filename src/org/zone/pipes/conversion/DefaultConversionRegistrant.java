package org.zone.pipes.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.zone.pipes.PipesPlugin;
import org.zone.pipes.annotations.ConvertMethod;
import org.zone.pipes.errors.ConversionException;

public class DefaultConversionRegistrant implements ConversionRegistrant {

	private PipesPlugin plugin;
	
	public DefaultConversionRegistrant(PipesPlugin plugin){
		this.plugin = plugin;
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
	public World[] getWorlds(String name){
		name = name.toLowerCase();
		List<World> matches = new ArrayList<World>();
		for(World w:plugin.getServer().getWorlds()){
			if(w.getName().toLowerCase().startsWith(name)){
				matches.add(w);
			}
		}
		Collections.sort(matches, new Comparator<World>(){
			public int compare(World q1, World q2){
				return q1.getName().length()-q2.getName().length();
			}
		});
		return matches.toArray(new World[0]);
	}
	
	@ConvertMethod
	public Player[] getPlayers(String name){
		name = name.toLowerCase();
		List<Player> matches = new ArrayList<Player>();
		for(Player p:plugin.getServer().getOnlinePlayers()){
			if(p.getName().toLowerCase().startsWith(name)){
				matches.add(p);
			}
		}
		Collections.sort(matches, new Comparator<Player>(){
			public int compare(Player p1, Player p2){
				return p1.getName().length()-p2.getName().length();
			}
		});
		return matches.toArray(new Player[0]);
	}
	
	@ConvertMethod
	public Location getLocation(Entity e){
		return e.getLocation();
	}
	
}
