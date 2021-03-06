package de.blockbuild.musikbot.configuration;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import de.blockbuild.musikbot.Bot;
import de.blockbuild.musikbot.core.GuildMusicManager;

public class GuildConfiguration extends ConfigurationManager {
	private final GuildMusicManager musicManager;
	private String guildName;
	private int volume;
	private List<Long> blacklist, whitelist;
	private Boolean disconnectIfAlone, disconnectAfterLastTrack, useWhitelist;
	private Map<String, Object> autoConnect, defaultTextChannel, defaultVoiceChannel;
	private static String header;

	public GuildConfiguration(Bot bot, GuildMusicManager musicManager) {
		super(new File(bot.getMain().getDataFolder(), "/Guilds/" + musicManager.getGuild().getId() + ".yml"));
		this.musicManager = musicManager;
		this.guildName = musicManager.getGuild().getName();

		StringBuilder builder = new StringBuilder();
		builder.append("MusikBot by Block-Build\n");
		builder.append("+=====================+\n");
		builder.append("| GUILD CONFIGURATION |\n");
		builder.append("+=====================+\n");
		builder.append("\n");
		header = builder.toString();

		readConfig();
		writeConfig();
	}

	public boolean writeConfig() {
		try {
			YamlConfiguration config = new YamlConfiguration();

			config.set("Guild_Name", this.guildName);
			config.set("Volume", this.volume);
			config.set("Whitelist_Enabled", this.useWhitelist);
			config.set("Whitelist", this.whitelist);
			config.set("Blacklist", this.blacklist);
			config.set("Auto_Disconnect_If_Alone", this.disconnectIfAlone);
			config.set("Auto_Disconnect_After_Last_Track", this.disconnectAfterLastTrack);

			this.phraseMap(config.createSection("Auto_Connect_On_Startup"), this.autoConnect, "Enabled",
					"VoiceChannelId", "Track");
			this.phraseMap(config.createSection("Default_TextChannel"), this.defaultTextChannel, "Enabled",
					"TextChannelId");
			this.phraseMap(config.createSection("Default_VoiceChannel"), this.defaultVoiceChannel, "Enabled",
					"VoiceChannelId");

			return this.saveConfig(config, header);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean readConfig() {
		try {
			YamlConfiguration config = this.loadConfig();
			ConfigurationSection c;

			config.addDefault("Guild_Name", this.guildName);
			config.addDefault("Volume", 100);
			config.addDefault("Whitelist_Enabled", "");
			config.addDefault("Whitelist", null);
			config.addDefault("Blacklist", null);
			config.addDefault("Auto_Disconnect_If_Alone", false);
			config.addDefault("Auto_Disconnect_After_Last_Track", false);

			c = this.addDefaultSection(config, "Auto_Connect_On_Startup");
			c.addDefault("Enabled", false);
			c.addDefault("VoiceChannelId", 0L);
			c.addDefault("Track", "");

			c = this.addDefaultSection(config, "Default_TextChannel");
			c.addDefault("Enabled", false);
			c.addDefault("TextChannelId", 0L);

			c = this.addDefaultSection(config, "Default_VoiceChannel");
			c.addDefault("Enabled", false);
			c.addDefault("VoiceChannelId", 0L);

			this.volume = !(config.getInt("Volume") < 1) && !(config.getInt("Volume") > 100) ? config.getInt("Volume")
					: 100;
			this.useWhitelist = config.getBoolean("Whitelist_Enabled");
			this.whitelist = config.getLongList("Whitelist");
			this.blacklist = config.getLongList("Blacklist");
			this.disconnectIfAlone = config.getBoolean("Auto_Disconnect_If_Alone");
			this.disconnectAfterLastTrack = config.getBoolean("Auto_Disconnect_After_Last_Track");

			this.autoConnect = config.getConfigurationSection("Auto_Connect_On_Startup").getValues(false);
			this.defaultTextChannel = config.getConfigurationSection("Default_TextChannel").getValues(false);
			this.defaultVoiceChannel = config.getConfigurationSection("Default_VoiceChannel").getValues(false);

			initConfig();
			return true;
		} catch (Exception e) {
			System.out.println("Couldn't read GuildConfiguration!");
			e.printStackTrace();
			return false;
		}
	}

	private void initConfig() {
		musicManager.getAudioPlayer().setVolume(this.volume);

		if (isAutoConnectEnabled() && getAutoConnectVoiceChannelId() == 0L)
			setAutoConnectEnabled(false);

		if (isDefaultTextChannelEnabled() && getDefaultTextChannel() == 0L)
			setDefaultTextChannelEnabled(false);

		if (isDefaultVoiceChannelEnabled() && getDefaultVoiceChannel() == 0L)
			setDefaultVoiceChannelEnabled(false);
	}

	public Boolean isDisconnectIfAloneEnabled() {
		return disconnectIfAlone;
	}

	public Boolean isDisconnectAfterLastTrackEnabled() {
		return disconnectAfterLastTrack;
	}

	public void setDisconnectIfAlone(Boolean bool) {
		disconnectIfAlone = bool;
	}

	public void setDisconnectAfterLastTrack(Boolean bool) {
		disconnectAfterLastTrack = bool;
	}

	public Boolean isWhitelistEnabled() {
		return useWhitelist;
	}

	public void setWhitelistEnabled(Boolean bool) {
		useWhitelist = bool;
	}

	public Boolean isAutoConnectEnabled() {
		return (Boolean) autoConnect.get("Enabled");
	}

	public long getAutoConnectVoiceChannelId() {
		return Long.parseLong(autoConnect.get("VoiceChannelId").toString());
	}

	public String getAutoConnectTrack() {
		return (String) autoConnect.get("Track");
	}

	public void setAutoConnectEnabled(Boolean bool) {
		autoConnect.replace("Enabled", bool);
	}

	public void setAutoConnectVoiceChannelId(Long voiceChannelId) {
		autoConnect.replace("VoiceChannelId", voiceChannelId);
	}

	public void setAutoConnectTrack(String track) {
		autoConnect.replace("Track", track);
	}

	public int getVolume() {
		return this.volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public boolean isBlockedUser(long ID) {
		return this.blacklist.contains(ID);
	}

	public void blacklistAdd(Long id) {
		blacklist.add(id);
	}

	public void blacklistRemove(Long id) {
		blacklist.remove(id);
	}

	public void blacklistClear() {
		blacklist.clear();
	}

	public List<Long> getBlacklist() {
		return blacklist;
	}

	public boolean isWhitelistedUser(long ID) {
		return this.whitelist.contains(ID);
	}

	public void whitelistAdd(Long id) {
		whitelist.add(id);
	}

	public void whitelistRemove(Long id) {
		whitelist.remove(id);
	}

	public void whitelistClear() {
		whitelist.clear();
	}

	public List<Long> getWhitelist() {
		return whitelist;
	}

	public boolean isDefaultTextChannelEnabled() {
		return (Boolean) defaultTextChannel.get("Enabled");
	}

	public void setDefaultTextChannelEnabled(boolean bool) {
		defaultTextChannel.replace("Enabled", bool);
	}

	public long getDefaultTextChannel() {
		return (long) defaultTextChannel.get("TextChannelId");
	}

	public void setDefaultTextChannel(long id) {
		defaultTextChannel.replace("TextChannelId", id);
	}

	public boolean isDefaultVoiceChannelEnabled() {
		return (Boolean) defaultVoiceChannel.get("Enabled");
	}

	public void setDefaultVoiceChannelEnabled(boolean bool) {
		defaultVoiceChannel.replace("Enabled", bool);
	}

	public long getDefaultVoiceChannel() {
		return (long) defaultVoiceChannel.get("VoiceChannelId");
	}

	public void setDefaultVoiceChannel(long id) {
		defaultVoiceChannel.replace("VoiceChannelId", id);
	}
}
