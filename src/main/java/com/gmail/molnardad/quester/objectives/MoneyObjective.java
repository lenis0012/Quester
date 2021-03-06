package com.gmail.molnardad.quester.objectives;

import org.bukkit.entity.Player;

import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.commandbase.QCommand;
import com.gmail.molnardad.quester.commandbase.QCommandContext;
import com.gmail.molnardad.quester.commandbase.exceptions.QCommandException;
import com.gmail.molnardad.quester.elements.Objective;
import com.gmail.molnardad.quester.elements.QElement;
import com.gmail.molnardad.quester.storage.StorageKey;

@QElement("MONEY")
public final class MoneyObjective extends Objective {

	private final double amount;
	
	public MoneyObjective(double amt) {
		amount = amt;
	}
	
	@Override
	public int getTargetAmount() {
		return 1;
	}
	
	@Override
	protected String show(int progress) {
		if(Quester.vault) {
			return "Get " + amount + " " + Quester.econ.currencyNamePlural();
		}
		else {
			return "Economy support disabled. (Completed)";
		}
	}
	
	@Override
	protected String info() {
		return String.valueOf(amount);
	}

	@Override
	public boolean tryToComplete(Player player) {
		if(!Quester.vault) {
			return true;
		}
		double money = Quester.econ.getBalance(player.getName());
		if(money >= amount) {
			if(Quester.vault) {
				Quester.econ.withdrawPlayer(player.getName(), amount);
			}
			return true;
		}
		return false;
	}
	
	@QCommand(
			min = 1,
			max = 1,
			usage = "<amount>")
	public static Objective fromCommand(QCommandContext context) throws QCommandException {
		double amt = context.getDouble(0);
		return new MoneyObjective(amt);
	}

	@Override
	protected void save(StorageKey key) {
		key.setDouble("amount", amount);
	}
	
	protected static Objective load(StorageKey key) {
		double amt = 0;
		amt = key.getDouble("amount", 0);
		if(amt <= 0) {
			return null;
		}
		return new MoneyObjective(amt);
	}
	
	//Custom methods
	
	public double takeMoney(double amt) {
		return amt - amount;
	}
}
