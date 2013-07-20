package com.gmail.molnardad.quester.qevents;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.commandbase.QCommand;
import com.gmail.molnardad.quester.commandbase.QCommandContext;
import com.gmail.molnardad.quester.elements.QElement;
import com.gmail.molnardad.quester.elements.Qevent;
import com.gmail.molnardad.quester.storage.StorageKey;

@QElement("MSG")
public final class MessageQevent extends Qevent {

	private final String message;
	private final String rawmessage;
	
	public MessageQevent(String msg) {
		this.rawmessage = msg;
		this.message = ChatColor.translateAlternateColorCodes('&', rawmessage).replaceAll("\\\\n", "\n");
	}
	
	@Override
	public String info() {
		return message;
	}

	@Override
	protected void run(Player player, Quester plugin) {
		player.sendMessage(message.replace("%p", player.getName()));
	}

	@QCommand(
			min = 1,
			max = Integer.MAX_VALUE,
			usage = "<message>")
	public static Qevent fromCommand(QCommandContext context) {
		String desc = "";
		for(int i = 0; i < context.length(); i++) {
			desc += context.getString(i) + " ";
		}
		
		if(context.length() > 0)
			desc = desc.substring(0, desc.length() - 1);
		
		return new MessageQevent(desc);
	}

	@Override
	protected void save(StorageKey key) {
		key.setString("message", rawmessage);
	}
	
	protected static Qevent load(StorageKey key) {
		String msg;
		
		msg = key.getString("message");
		if(msg == null) {
			return null;
		}
		
		return new MessageQevent(msg);
	}
}
