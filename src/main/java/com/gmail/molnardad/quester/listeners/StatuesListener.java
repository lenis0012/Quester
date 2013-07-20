package com.gmail.molnardad.quester.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.gmail.molnardad.quester.ActionSource;
import com.gmail.molnardad.quester.LanguageManager;
import com.gmail.molnardad.quester.QConfiguration;
import com.gmail.molnardad.quester.QuestHolder;
import com.gmail.molnardad.quester.QuestHolderManager;
import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.elements.Objective;
import com.gmail.molnardad.quester.exceptions.HolderException;
import com.gmail.molnardad.quester.exceptions.QuesterException;
import com.gmail.molnardad.quester.objectives.NpcObjective;
import com.gmail.molnardad.quester.profiles.ProfileManager;
import com.gmail.molnardad.quester.quests.Quest;
import com.gmail.molnardad.quester.quests.QuestManager;
import com.gmail.molnardad.quester.strings.QuesterLang;
import com.gmail.molnardad.quester.utils.Util;
import com.lenis0012.bukkit.statues.api.IStatue;
import com.lenis0012.bukkit.statues.api.events.PlayerInteractStatueEvent;
import com.lenis0012.bukkit.statues.api.events.PlayerInteractStatueEvent.InteractionType;

public class StatuesListener implements Listener {
	private QuestManager qMan;
	private QuestHolderManager holMan;
	private LanguageManager langMan;
	private ProfileManager profMan;
	
	public StatuesListener(Quester plugin) {
		this.qMan = plugin.getQuestManager();
		this.holMan = plugin.getHolderManager();
		this.langMan = plugin.getLanguageManager();
		this.profMan = plugin.getProfileManager();
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteractStatue(PlayerInteractStatueEvent event) {
		Player player = event.getPlayer();
		IStatue statue = event.getStatue();
		InteractionType type = event.getAction();
		boolean isQuester = statue.hasCustomMeta("quester");
		if(isQuester)
			event.setCancelled(true);
		
		if(type == InteractionType.LEFT_CLICK) {
			if(isQuester) {
				int id = statue.getCustomMeta("quester", int.class);
				QuestHolder holder = holMan.getHolder(id);
				QuesterLang lang = langMan.getPlayerLang(player.getName());
				if(!Util.permCheck(player, QConfiguration.PERM_USE_NPC, true, lang)) {
					return;
				}
				
				boolean isOp = Util.permCheck(player, QConfiguration.PERM_MODIFY, false, null);
				if(isOp) {
					if(player.getItemInHand().getTypeId() == 369) {
						statue.removeCustomMeta("quester");
						player.sendMessage(ChatColor.GREEN + lang.HOL_UNASSIGNED);
					    return;
					}
				}
				
				if(holder == null) {
					player.sendMessage(ChatColor.RED + lang.ERROR_HOL_NOT_ASSIGNED);
					return;
				}
				
				if(!holder.canInteract(player.getName())) {
					player.sendMessage(ChatColor.RED + lang.ERROR_HOL_INTERACT);
					return;
				}
				
				holder.interact(player.getName());
				Quest quest = qMan.getQuest(holMan.getOne(holder));
				if(quest != null) {
					if(profMan.getProfile(player.getName()).hasQuest(quest)) {
						return;
					}
					else {
						try {
							qMan.showQuest(player, quest.getName(), lang);
							return;
						}
						catch (QuesterException ignore) {}
					}
				}
				
				try {
					holMan.selectNext(player.getName(), holder, lang);
				} catch (HolderException e) {
					player.sendMessage(e.getMessage());
					if(!isOp) {
						return;
					}
					
				}
				
				String name = statue.isPlayer() ? statue.getName() : statue.getType().toString();
				player.sendMessage(Util.line(ChatColor.BLUE, name + "'s quests", ChatColor.GOLD));
				if(isOp) {
					holMan.showQuestsModify(holder, player);
				} else {
					holMan.showQuestsUse(holder, player);
				}
			}
		} else if(type == InteractionType.RIGHT_CLICK) {
			QuesterLang lang = langMan.getPlayerLang(player.getName());
			if(isQuester) {
				int id = statue.getCustomMeta("quester", int.class);
				QuestHolder holder = holMan.getHolder(id);
				if(!Util.permCheck(player, QConfiguration.PERM_USE_NPC, true, lang)) {
					return;
				}
				
				boolean isOP = Util.permCheck(player, QConfiguration.PERM_MODIFY, false, null);
				if(isOP) {
					if(player.getItemInHand().getTypeId() == 369) {
						int sel = profMan.getProfile(player.getName()).getHolderID();
						if(sel < 0){
							player.sendMessage(ChatColor.RED + lang.ERROR_HOL_NOT_SELECTED);
						} else {
							statue.setCustomMeta("quester", sel);
							player.sendMessage(ChatColor.GREEN + lang.HOL_ASSIGNED);
						}
					    return;
					}
				}
				
				if(holder == null) {
					player.sendMessage(ChatColor.RED + lang.ERROR_HOL_NOT_ASSIGNED);
					return;
				}
				
				if(!holder.canInteract(player.getName())) {
					player.sendMessage(ChatColor.RED + lang.ERROR_HOL_INTERACT);
					return;
				}
				
				holder.interact(player.getName());
				List<Integer> qsts = holder.getQuests();
				Quest currentQuest = profMan.getProfile(player.getName()).getQuest();
				if(!player.isSneaking()) {
					int questID = currentQuest == null ? -1 : currentQuest.getID();
					// player has quest and quest giver does not accept this quest
					if(questID >= 0 && !qsts.contains(questID)) {
						player.sendMessage(ChatColor.RED + lang.ERROR_Q_NOT_HERE);
						return;
					}
					
					// player has quest and quest giver accepts this quest
					if(questID >= 0 && qsts.contains(questID)) {
						try {
							profMan.complete(player, ActionSource.holderSource(holder), lang);
						} catch (QuesterException e) {
							try {
								profMan.showProgress(player, lang);
							} catch (QuesterException f) {
								player.sendMessage(ChatColor.DARK_PURPLE + lang.ERROR_INTERESTING);
							}
						}
						return;
					}
				}
				
				int selected = holMan.getOne(holder);
				if(selected < 0) {
					selected = holder.getSelectedId(player.getName());
				}
				
				// player doesn't have quest
				if(qMan.isQuestActive(selected)) {
					try {
						profMan.startQuest(player, qMan.getQuest(selected), ActionSource.holderSource(holder), lang);
					} catch (QuesterException e) {
						player.sendMessage(e.getMessage());
					}
				} else {
					player.sendMessage(ChatColor.RED + lang.ERROR_Q_NOT_SELECTED);
				}
			} else {
				boolean isOP = Util.permCheck(player, QConfiguration.PERM_MODIFY, false, null);
				if(isOP) {
					if(player.getItemInHand().getTypeId() == 369) {
						int sel = profMan.getProfile(player.getName()).getHolderID();
						if(sel < 0){
							player.sendMessage(ChatColor.RED + lang.ERROR_HOL_NOT_SELECTED);
						} else {
							statue.setCustomMeta("quester", sel);
							player.sendMessage(ChatColor.GREEN + lang.HOL_ASSIGNED);
						}
					    return;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onRightClickStatue(PlayerInteractStatueEvent event) {
		Player player = event.getPlayer();
		IStatue statue = event.getStatue();
		InteractionType type = event.getAction();
		
		if(type == InteractionType.RIGHT_CLICK) {
			Quest quest = profMan.getProfile(player.getName()).getQuest();
			if(quest != null) {
		    	if(!quest.allowedWorld(player.getWorld().getName().toLowerCase()))
		    		return;
		    	
		    	List<Objective> objs = quest.getObjectives();
		    	for(int i = 0; i < objs.size(); i++) {
		    		if(objs.get(i).getType().equalsIgnoreCase("NPC")) {
			    		if(!profMan.isObjectiveActive(player, i)){
		    				continue;
		    			}
			    		
		    			NpcObjective obj = (NpcObjective)objs.get(i);
		    			if(obj.checkNpc(statue.getId())) {
		    				profMan.incProgress(player, ActionSource.listenerSource(event), i);
		    				if(obj.getCancel()) {
		    					event.setCancelled(true);
		    				}
		    				
		    				return;
		    			}
		    		}
		    	}
		    }
		}
	}
}