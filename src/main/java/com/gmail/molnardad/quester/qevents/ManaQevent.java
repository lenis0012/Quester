package com.gmail.molnardad.quester.qevents;

import net.tweetcraft.bukkit.core.TweetCraft;
import net.tweetcraft.bukkit.core.TweetPlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.commandbase.QCommand;
import com.gmail.molnardad.quester.commandbase.QCommandContext;
import com.gmail.molnardad.quester.elements.QElement;
import com.gmail.molnardad.quester.elements.Qevent;
import com.gmail.molnardad.quester.storage.StorageKey;

@QElement("MANA")
public final class ManaQevent extends Qevent {
	private int amount;
	
	public ManaQevent(int amount) {
		this.amount = amount;
	}
	
	@Override
	protected String info() {
		return String.valueOf(amount);
	}

	@Override
	protected void run(Player player, Quester plugin) {
		if(!Bukkit.getPluginManager().isPluginEnabled("TweetCraft")) {
			Quester.log.info("Failed to process event: TweetCraft was not found!");
			return;
		}
		
		TweetCraft tc = TweetCraft.getInstance();
		TweetPlayer tp = tc.getPlayer(player.getName());
		tp.setMana(tp.getMana() + amount);
	}
	
	@QCommand(
			min = 1,
			max = 1,
			usage = "<amount>")
	public static Qevent fromCommand(QCommandContext context) {
		return new ManaQevent(context.getInt(0));
	}
	

	@Override
	protected void save(StorageKey key) {
		key.setInt("amount", amount);
	}
	
	protected static Qevent load(StorageKey key) {
		int amount;
		
		amount = key.getInt("amount", 0);
		if(amount == 0) {
			return null;
		}
		
		return new ManaQevent(amount);
	}
}