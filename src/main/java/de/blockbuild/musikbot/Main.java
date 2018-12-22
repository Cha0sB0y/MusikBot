package de.blockbuild.musikbot;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	private Bot bot;

	@Override
	public void onEnable() {
		try {
			start();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@Override
	public void onDisable() {
		if (bot != null) {
			if (bot.stop()) {
			}
		}
	}

	private void start() {
		// initInstances();
		Bukkit.getScheduler().runTaskLater(this, () -> {
			if (true /* load config */ ) {
				System.out.println("Music Bot get started");
				bot = new Bot(this);
			} else {
				this.getServer().shutdown();
			}
		}, 1L);
	}

	public Bot getBot() {
		return bot;
	}

	public void setBot(Bot bot) {
		this.bot = bot;
	}
}
