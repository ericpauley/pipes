package org.zone.pipes;

import org.bukkit.plugin.java.JavaPlugin;
import org.zone.pipes.conversion.ConversionManager;

public class PipesPlugin extends JavaPlugin {

	ConversionManager conversionManager;
	
	public void onEnable(){
		this.conversionManager = new ConversionManager(this);
	}
	
	
	
}
