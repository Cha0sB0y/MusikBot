package de.blockbuild.musikbot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import de.blockbuild.musikbot.Listener.MessageListener;
import de.blockbuild.musikbot.Listener.VoiceChannelListener;
import de.blockbuild.musikbot.commands.ChooseCommand;
import de.blockbuild.musikbot.commands.AutoConnectCommand;
import de.blockbuild.musikbot.commands.AutoDisconnectCommand;
import de.blockbuild.musikbot.commands.FlushQueue;
import de.blockbuild.musikbot.commands.InfoCommand;
import de.blockbuild.musikbot.commands.JoinCommand;
import de.blockbuild.musikbot.commands.NextCommand;
import de.blockbuild.musikbot.commands.PauseCommand;
import de.blockbuild.musikbot.commands.PingCommand;
import de.blockbuild.musikbot.commands.PlayCommand;
import de.blockbuild.musikbot.commands.PlaylistCommand;
import de.blockbuild.musikbot.commands.QueueCommand;
import de.blockbuild.musikbot.commands.QuitCommand;
import de.blockbuild.musikbot.commands.RadioBobCommand;
import de.blockbuild.musikbot.commands.RadioBonnRheinSiegCommand;
import de.blockbuild.musikbot.commands.RadioMnmCommand;
import de.blockbuild.musikbot.commands.RautemusikCommand;
import de.blockbuild.musikbot.commands.ResumeCommand;
import de.blockbuild.musikbot.commands.ConfigCommand;
import de.blockbuild.musikbot.commands.DefaultTextChannelCommand;
import de.blockbuild.musikbot.commands.DefaultVoiceChannelCommand;
import de.blockbuild.musikbot.commands.BlacklistCommand;
import de.blockbuild.musikbot.commands.ShuffleCommand;
import de.blockbuild.musikbot.commands.SkipCommand;
import de.blockbuild.musikbot.commands.StopCommand;
import de.blockbuild.musikbot.commands.VersionCommand;
import de.blockbuild.musikbot.commands.VolumeCommand;
import de.blockbuild.musikbot.commands.WhitelistCommand;
import de.blockbuild.musikbot.commands.YTAutoPlayCommand;
import de.blockbuild.musikbot.configuration.BotConfiguration;
import de.blockbuild.musikbot.core.GuildMusicManager;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Game.GameType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class Bot {
	public final static Permission[] RECOMMENDED_PERMS = new Permission[] { Permission.MESSAGE_READ,
			Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
			Permission.MESSAGE_EMBED_LINKS, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.MESSAGE_TTS };
	public final static Permission[] REQUIRED_PERMS = new Permission[] { Permission.MESSAGE_READ,
			Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS,
			Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.MESSAGE_TTS };
	private final Main main;
	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	private JDA jda;
	public final BotConfiguration config;

	public Bot(Main main) {
		System.out.println("[" + main.getName() + "] Get started...");

		this.main = main;
		musicManagers = new HashMap<>();
		playerManager = new DefaultAudioPlayerManager();
		config = new BotConfiguration(this);

		if (start()) {
			initListeners();
			initCommandClient();
			System.out.println("[" + main.getName() + "] Started successfully");
		} else {
			System.out.println("[" + main.getName() + "] Shut down");
			main.onDisable();
		}
	}

	public boolean start() {
		try {
			String token = config.getToken();
			if (token == null || token.isEmpty()) {
				System.out.println("No token was provided. Please provide a vaild token.");
				System.out.println("Without a token the Bot will not be able to start.");
				return false;
			} else if (token.equals("Insert Token here")) {
				System.out.println("Token was left at default. Please provide a vaild token.");
				System.out.println("Without a token the Bot will not be able to start.");
				return false;
			}
			jda = new JDABuilder(AccountType.BOT).setToken(token).setGame(Game.of(GameType.DEFAULT, "starting..."))
					.setAudioEnabled(true).setStatus(OnlineStatus.DO_NOT_DISTURB).build();
			jda.awaitReady();
		} catch (LoginException e) {
			System.out.println("Invaild bot Token");
			return false;
		} catch (InterruptedException e) {
			// Should never triggered!
			e.printStackTrace();
			return false;
		}

		try {
			jda.getSelfUser().getManager().setAvatar(Icon.from(main.getResource("64.png"))).queue();
		} catch (IOException e) {
			System.err.println(e);
		}

		jda.getPresence().setPresence(OnlineStatus.ONLINE, Game.of(GameType.DEFAULT, config.getGame()));
		if (!jda.getSelfUser().getName().equalsIgnoreCase("MusikBot")) {
			jda.getSelfUser().getManager().setName("MusikBot").queue();
		}

		// Print invite token to console
		System.out.println("Invite Token:");
		String inviteURL = jda.asBot().getInviteUrl(Bot.RECOMMENDED_PERMS);
		System.out.println(inviteURL);
		config.setInviteLink(inviteURL);

		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);

		jda.getGuilds().forEach((guild) -> {
			getGuildAudioPlayer(guild);
		});
		return true;
	}

	public void stop() {
		if (!(jda == null)) {
			jda.shutdown();
			jda = null;
		}
	}

	public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		GuildMusicManager musicManager = musicManagers.get(guild.getIdLong());

		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager, guild, this);
			musicManagers.put(guild.getIdLong(), musicManager);

			guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
		}
		return musicManager;
	}

	public void initListeners() {
		jda.addEventListener(new MessageListener(this));
		jda.addEventListener(new VoiceChannelListener());
	}

	public void initCommandClient() {
		String ownerID = config.getOwnerID();
		String trigger = config.getTrigger();
		CommandClientBuilder ccb = new CommandClientBuilder();
		ccb.setOwnerId(ownerID);
		ccb.setCoOwnerIds("240566179880501250");
		ccb.useHelpBuilder(true);
		ccb.setEmojis(config.getSuccess(), config.getWarning(), config.getError());
		ccb.setPrefix(trigger);
		registerCommandModule(ccb,
				//Music
				new PlayCommand(this), 
				new QueueCommand(this),
				new NextCommand(this), 
				new SkipCommand(this),
				new ChooseCommand(this),
				new FlushQueue(this),
				new ShuffleCommand(this),
				new PlaylistCommand(this),
				new YTAutoPlayCommand(this),
  
				//Radio
				new RadioBonnRheinSiegCommand(this), 
				new RautemusikCommand(this), 
				new RadioBobCommand(this),
				new RadioMnmCommand(this),
				
				new VolumeCommand(this),
				new InfoCommand(this),  
				new PauseCommand(this),
				new ResumeCommand(this),
				
				//Connection
				new JoinCommand(this), 
				new QuitCommand(this),
				new StopCommand(this), 
				new PingCommand(this),
				
				//Setup
				new BlacklistCommand(this),
				new WhitelistCommand(this),
				new AutoDisconnectCommand(this),
				new AutoConnectCommand(this),
				new DefaultTextChannelCommand(this),
				new DefaultVoiceChannelCommand(this),
				new ConfigCommand(this),
				new VersionCommand(this));
    
		jda.addEventListener(ccb.build());

		/*
		 * missing commands:
		 * #Playback commands##
		 * jump to time?
		 * 
		 * ##setup commands##
		 * defaultPlaylist?
		 * setIcon?
		 */
	}

	public void registerCommandModule(CommandClientBuilder ccb,Command... commands) {
		for (Command c : commands) {
			ccb.addCommand(c);
		}
	}

	public boolean joinDiscordVoiceChannel(Guild guild) {
		for (int i = 0; i < guild.getVoiceChannels().size(); i++) {
			try {
				guild.getAudioManager().openAudioConnection(guild.getVoiceChannels().get(i));
				return true;
			} catch (IllegalArgumentException e) {
				System.out.println("no VoiceChannel");
			} catch (InsufficientPermissionException e) {
				System.out.println("Missing permission: " + e.getPermission() + " to join '"
						+ guild.getVoiceChannels().get(i).getName() + "'");
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		return false;
	}

	public boolean joinDiscordVoiceChannel(Guild guild, Long id) {
		try {
			guild.getAudioManager().openAudioConnection(guild.getVoiceChannelById(id));
			return true;
		} catch (IllegalArgumentException e) {
			System.out.println(id + " is not a VoiceChannel");
		} catch (InsufficientPermissionException e) {
			System.out.println("Missing permission: " + e.getPermission() + " to join '" + id + "'");
		} catch (Exception e) {
			System.out.println(id + " isn't a vaild VoiceChannel");
		}
		return false;
	}

	public boolean joinDiscordVoiceChannel(Guild guild, String name) {
		try {
			guild.getAudioManager().openAudioConnection((VoiceChannel) guild.getVoiceChannelsByName(name, true).get(0));
			return true;
		} catch (IllegalArgumentException e) {
			System.out.println(name + " is not a VoiceChannel");
		} catch (InsufficientPermissionException e) {
			System.out.println("Missing permission: " + e.getPermission() + " to join '"
					+ guild.getVoiceChannels().get(0).getName() + "'");
		} catch (Exception e) {
			System.out.println(name + " isn't a vaild VoiceChannel");
		}
		return false;
	}

	public JDA getJda() {
		return jda;
	}

	public Main getMain() {
		return main;
	}

	public AudioPlayerManager getPlayerManager() {
		return playerManager;
	}

	public User getUserById(String id) {
		return this.jda.getUserById(id);
	}

	public User getUserById(Long id) {
		return this.jda.getUserById(id);
	}

	public String getUserNameById(String id) {
		User user = getUserById(id);
		if (user == null) {
			return "UNKNOWN";
		} else {
			return user.getName();
		}
	}

	public String getUserNameById(Long id) {
		User user = getUserById(id);
		if (user == null) {
			return "UNKNOWN";
		} else {
			return user.getName();
		}
	}

	public TextChannel getTextChannelById(Long id) {
		return this.jda.getTextChannelById(id);
	}

	public VoiceChannel getVoiceChannelById(Long id) {
		return this.jda.getVoiceChannelById(id);
	}

	public Guild getGuildById(String id) {
		try {
			return this.jda.getGuildById(id);
		} catch (Exception e) {
			return null;
		}
	}

	public Guild getGuildById(Long id) {
		try {
			return this.jda.getGuildById(id);
		} catch (Exception e) {
			return null;
		}
	}
}
