package com.gmail.molnardad.quester.listeners;

import java.util.List;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;

import com.gmail.molnardad.quester.Quest;
import com.gmail.molnardad.quester.QuestManager;
import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.objectives.EnchantObjective;
import com.gmail.molnardad.quester.objectives.Objective;


public class EnchantListener implements Listener {

	private QuestManager qm = Quester.qMan;
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEnchant(EnchantItemEvent event) {
	    Player player = event.getEnchanter();
	    if(qm.hasQuest(player.getName())) {
	    	Quest quest = qm.getPlayerQuest(player.getName());
	    	if(!quest.allowedWorld(player.getWorld().getName().toLowerCase()))
	    		return;
	    	List<Objective> objs = quest.getObjectives();
	    	// if quest is ordered, process current objective
	    	if(quest.isOrdered()) {
	    		int curr = qm.getCurrentObjective(player);
	    		Objective obj = objs.get(curr);
	    		if(obj != null) {
	    			if(obj.getType().equalsIgnoreCase("ENCHANT")) {
	    				EnchantObjective eObj = (EnchantObjective)obj;
		    			ItemStack item = event.getItem();
		    			Map<Enchantment, Integer> enchs = event.getEnchantsToAdd();
		    			if(eObj.check(item, enchs)) {
		    				qm.incProgress(player, curr);
		    				return;
		    			}
	    			}
	    		}
	    		return;
	    	}
	    	for(int i = 0; i < objs.size(); i++) {
	    		// check if Objective is type CRAFT
	    		if(objs.get(i).getType().equalsIgnoreCase("ENCHANT")) {
		    		if(qm.achievedTarget(player, i)){
	    				continue;
	    			}
	    			EnchantObjective obj = (EnchantObjective)objs.get(i);
	    			ItemStack item = event.getItem();
	    			Map<Enchantment, Integer> enchs = event.getEnchantsToAdd();
	    			if(obj.check(item, enchs)) {
	    				qm.incProgress(player, i);
	    				return;
	    			}
	    		}
	    	}
	    	
	    }
	}
	
}
