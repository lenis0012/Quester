package com.gmail.molnardad.quester.managers;

import java.io.File;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.molnardad.quester.QuestHolder;
import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.QuesterSign;
import com.gmail.molnardad.quester.exceptions.CustomException;
import com.gmail.molnardad.quester.exceptions.HolderException;
import com.gmail.molnardad.quester.exceptions.QuesterException;
import com.gmail.molnardad.quester.storage.ConfigStorage;
import com.gmail.molnardad.quester.storage.Storage;
import com.gmail.molnardad.quester.storage.StorageKey;
import com.gmail.molnardad.quester.strings.QuesterLang;
import com.gmail.molnardad.quester.utils.Util;

public class QuestHolderManager {

	private ProfileManager profMan = null;
	private QuestManager qMan = null;
	
	private Storage holderStorage = null;
	
	private Map<Integer, QuestHolder> holderIds = new HashMap<Integer, QuestHolder>();
	private Map<Location, QuesterSign> signs = new HashMap<Location, QuesterSign>();
	
	private int holderID = -1;
	
	public QuestHolderManager(Quester plugin) {
		this.qMan = plugin.getQuestManager();
		this.profMan = plugin.getProfileManager();
		File file = new File(plugin.getDataFolder(), "holders.yml");
		holderStorage = new ConfigStorage(file, Quester.log, null);
	}
	
	public Map<Integer, QuestHolder> getHolders() {
		return holderIds;
	}
	
	public QuestHolder getHolder(int ID) {
		return holderIds.get(ID);
	}
	
	public int getLastHolderID(){
		return holderID;
	}
	
	public int getNewHolderID() {
		holderID++;
		return holderID;
	}
	
	public void setHolderID(int newID) {
		holderID = newID;
	}
	
	public void adjustHolderID() {
		int newID = -1;
		for(int i : holderIds.keySet()) {
			if(i > newID)
				newID = i;
		}
		holderID = newID;
	}
	
	// SIGN MANIPULATION
	
	public QuesterSign getSign(Location loc) {
		return signs.get(loc);
	}
	
	public void addSign(QuesterSign sign) {
		if(sign != null && sign.getLocation() != null) {
			signs.put(sign.getLocation(), sign);
		}
	}
	
	public boolean removeSign(Location location) {
		return signs.remove(location) != null;
	}
	
	// HOLDER MANIPULATION
	
	public int createHolder(String name) {
		QuestHolder qh = new QuestHolder(name);
		int id = getNewHolderID();
		holderIds.put(id, qh);
		saveHolders();
		return id;
	}
	
	public void removeHolder(int ID) {
		holderIds.remove(ID);
		saveHolders();
	}
	
	public void addHolderQuest(String issuer, int questID, QuesterLang lang) throws QuesterException {
		QuestHolder qh = getHolder(profMan.getProfile(issuer).getHolderID());
		if(qh == null) {
			throw new HolderException(lang.ERROR_HOL_NOT_EXIST);
		}
		qh.addQuest(questID);
		saveHolders();
	}
	
	public void removeHolderQuest(String issuer, int questID, QuesterLang lang) throws QuesterException {
		QuestHolder qh = getHolder(profMan.getProfile(issuer).getHolderID());
		if(qh == null) {
			throw new HolderException(lang.ERROR_HOL_NOT_EXIST);
		}
		qh.removeQuest(questID);
		saveHolders();
	}
	
	public void moveHolderQuest(String issuer, int which, int where, QuesterLang lang) throws QuesterException {
		QuestHolder qh = getHolder(profMan.getProfile(issuer).getHolderID());
		if(qh == null) {
			throw new HolderException(lang.ERROR_HOL_NOT_SELECTED);
		}
		try {
			qh.moveQuest(which, where);
		}
		catch (IndexOutOfBoundsException e) {
			throw new CustomException(lang.ERROR_CMD_ID_OUT_OF_BOUNDS);
		}
		saveHolders();
	}
	
	public boolean selectNext(String selecter, QuestHolder holder, QuesterLang lang) throws HolderException {
		if(holder == null) {
			return false;
		}
		holder.interact(selecter);
		List<Integer> heldQuests = holder.getQuests();
		if(heldQuests.isEmpty()) {
			throw new HolderException(lang.ERROR_Q_NONE);
		}
		if(holder.getSelected(selecter) == -1) {
			holder.setSelected(selecter, 0);
			if(qMan.isQuestActive(heldQuests.get(0))) {
				return true;
			}
		}
		int i = holder.getSelected(selecter);
		int selected = i;
		boolean notChosen = true;
		while(notChosen) {
			if(i < heldQuests.size()-1)
				i++;
			else
				i = 0;
			if(qMan.isQuestActive(heldQuests.get(i))) {
				holder.setSelected(selecter, i);
				notChosen = false;
			} else if(i == selected) {
				throw new HolderException(lang.ERROR_Q_NONE_ACTIVE);
			}
		}
		return true;
	}
	
	public void checkHolders() {
		for(QuestHolder hol : holderIds.values()) {
			checkQuests(hol);
		}
	}
	
	private void checkQuests(QuestHolder holder) {
		if(holder == null) {
			return;
		}
		Iterator<Integer> iterator = holder.getQuests().iterator();
		while(iterator.hasNext()) {
			if(qMan.isQuest(iterator.next())) {
				iterator.remove();
			}
		}
	}

	public void showHolderList(CommandSender sender, QuesterLang lang) {
		sender.sendMessage(Util.line(ChatColor.BLUE, lang.INFO_HOLDER_LIST, ChatColor.GOLD));
		for(int id : getHolders().keySet()){
			sender.sendMessage(ChatColor.BLUE + "[" + id + "]" + ChatColor.GOLD + " " + getHolder(id).getName());
		}
	}

	public void showHolderInfo(CommandSender sender, int holderID, QuesterLang lang) throws QuesterException {
		QuestHolder qh;
		int id;
		if(holderID < 0) {
			id = profMan.getProfile(sender.getName()).getHolderID();
		} else {
			id = holderID;
		}
		qh = getHolder(id);
		if(qh == null) {
			if(holderID < 0)
				throw new HolderException(lang.ERROR_HOL_NOT_SELECTED);
			else
				throw new HolderException(lang.ERROR_HOL_NOT_EXIST);
		}
		sender.sendMessage(ChatColor.GOLD + "Holder ID: " + ChatColor.RESET + id);
		showQuestsModify(qh, sender);
	}
	
	public boolean showQuestsUse(QuestHolder holder, Player player) {
		if(holder == null) {
			return false;
		}
		List<Integer> heldQuests = holder.getQuests();
		int selected = holder.getSelected(player.getName());
		for(int i=0; i<heldQuests.size(); i++) {
			if(qMan.isQuestActive(heldQuests.get(i))) {
				player.sendMessage((i == selected ? ChatColor.GREEN : ChatColor.BLUE) + " - "
						+ qMan.getQuestName(heldQuests.get(i)));
			}
		}
		return true;
	}
	
	public boolean showQuestsModify(QuestHolder holder, CommandSender sender){
		if(holder == null) {
			return false;
		}
		sender.sendMessage(ChatColor.GOLD + "Holder name: " + ChatColor.RESET + holder.getName());
		List<Integer> heldQuests = holder.getQuests();
		int selected = holder.getSelected(sender.getName());
		for(int i=0; i<heldQuests.size(); i++) {
			ChatColor col = qMan.isQuestActive(heldQuests.get(i)) ? ChatColor.BLUE : ChatColor.RED;
			
			sender.sendMessage(i + ". " + (i == selected ? ChatColor.GREEN : ChatColor.BLUE) + "["
					+ heldQuests.get(i) + "] " + col + qMan.getQuestName(heldQuests.get(i)));
		}
		return true;
	}
	
	public void loadHolders() {
		// HOLDERS
		holderStorage.load();
		StorageKey holderKey = holderStorage.getKey("holders");
		QuestHolder qh;
		if(holderKey.hasSubKeys()) {
			for(StorageKey subKey : holderKey.getSubKeys()) {
				try {
					int id = Integer.parseInt(subKey.getName());
					qh = QuestHolder.deserialize(subKey);
					if(qh == null){
						throw new InvalidKeyException();
					}
					if(holderIds.get(id) != null) {
						Quester.log.info("Duplicate holder index: '" + subKey.getName() + "'");
					}
					holderIds.put(id, qh);
				} catch (NumberFormatException e) {
					Quester.log.info("Not numeric holder index: '" + subKey.getName() + "'");
				} catch (Exception e) {
					Quester.log.info("Invalid holder: '" + subKey.getName() + "'");
				}
			}
		}
		adjustHolderID();
		
		// SIGNS
		StorageKey signKey = holderStorage.getKey("signs");
		Object object = signKey.getRaw("");
		if(object != null) {
			if(object instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> list = (List<Map<String, Object>>) object;
				for(Map<String, Object> map : list) {
					QuesterSign sign = QuesterSign.deserialize(map);
					if(sign == null)
						continue;
					signs.put(sign.getLocation(), sign);
				}
			} else {
				Quester.log.info("Invalid sign list in holders.yml.");
			}
		}
		
		saveHolders();
		if(DataManager.verbose) {
			Quester.log.info(holderIds.size() + " holders loaded.");
			Quester.log.info(signs.size() + " signs loaded.");
		}
	}

	public void saveHolders() {
		holderStorage.save();
	}
	
//	@SuppressWarnings("unchecked")
//	public void loadHolders() {
//		try {
//
//			YamlConfiguration config = plugin.holderConfig.getConfig();
//			
//			// HOLDERS
//			ConfigurationSection holders = config.getConfigurationSection("holders");
//			QuestHolder qh;
//			if(holders != null) {
//				for(String key : holders.getKeys(false)) {
//					try {
//						int id = Integer.parseInt(key);
//						qh = QuestHolder.deserialize(holders.getConfigurationSection(key), plugin);
//						if(qh == null){
//							throw new InvalidKeyException();
//						}
//						if(holderIds.get(id) != null)
//							Quester.log.info("Duplicate holder index: '" + key + "'");
//						holderIds.put(id, qh);
//					} catch (NumberFormatException e) {
//						Quester.log.info("Not numeric holder index: '" + key + "'");
//					} catch (Exception e) {
//						Quester.log.info("Invalid holder: '" + key + "'");
//					}
//				}
//			}
//			adjustHolderID();
//			
//			// SIGNS
//			Object object = config.get("signs");
//			if(object != null) {
//				if(object instanceof List) {
//					List<Map<String, Object>> list = (List<Map<String, Object>>) object;
//					for(Map<String, Object> map : list) {
//						QuesterSign sign = QuesterSign.deserialize(map);
//						if(sign == null)
//							continue;
//						String s = sign.getLocation().getWorld().getName() + sign.getLocation().getBlockX() + sign.getLocation().getBlockY() + sign.getLocation().getBlockZ();
//						signs.put(s, sign);
//					}
//				} else {
//					Quester.log.info("Invalid sign list in holders.yml.");
//				}
//			}
//			
//			saveHolders();
//			if(verbose) {
//				Quester.log.info(holderIds.size() + " holders loaded.");
//				Quester.log.info(signs.size() + " signs loaded.");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
}