package de.blockbuild.musikbot.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import de.blockbuild.musikbot.Main;
import de.blockbuild.musikbot.core.MBCommand;
import de.blockbuild.musikbot.core.TrackScheduler;

public class PlayCommand extends MBCommand {

	public PlayCommand(Main main) {
		super(main);
		this.name = "play";
		this.aliases = new String[] { "p" };
		this.help = "Shows current track or plays given track";
		this.arguments = "[URL|title]";
		this.joinOnCommand = true;
		this.category = MUSIC;
	}

	@Override
	protected void doCommand(CommandEvent event) {
		TrackScheduler trackScheduler = main.getBot().getScheduler();
		AudioPlayer player = trackScheduler.getPlayer();
		if (event.getArgs().isEmpty()) {
			StringBuilder builder = new StringBuilder();
			if (player.getPlayingTrack() == null) {
				builder.append(event.getClient().getWarning()).append(" `Queue is empty`");
			} else {
				builder.append(event.getClient().getSuccess()).append(" Playing: `")
						.append(player.getPlayingTrack().getInfo().title).append("`. Left time: `")
						.append(getTime(
								player.getPlayingTrack().getDuration() - player.getPlayingTrack().getPosition()))
						.append("` Minutes.");
			}
			event.reply(builder.toString());
		} else {
			AudioPlayerManager playerManager = main.getBot().getPlayerManager();
			playerManager.loadItem(event.getArgs(), new ResultHandler(trackScheduler, event));
		}
	}

	private String getTime(long lng) {
		return (new SimpleDateFormat("mm:ss")).format(new Date(lng));
	}

	private class ResultHandler implements AudioLoadResultHandler {

		private TrackScheduler ts;
		private CommandEvent event;

		public ResultHandler(TrackScheduler trackScheduler, CommandEvent event) {
			this.ts = trackScheduler;
			this.event = event;
		}

		@Override
		public void trackLoaded(AudioTrack track) {
			ts.playTrack(track, event);
		}

		@Override
		public void playlistLoaded(AudioPlaylist playlist) {
			AudioTrack firstTrack = playlist.getSelectedTrack();

			if (firstTrack == null) {
				firstTrack = playlist.getTracks().get(0);
			}
			ts.playTrack(firstTrack, event);
		}

		@Override
		public void noMatches() {
			StringBuilder builder = new StringBuilder(event.getClient().getError());
			builder.append(" No result found: ").append(event.getArgs());
			event.reply(builder.toString());
			System.out.println("no results found: " + event.getArgs());
		}

		@Override
		public void loadFailed(FriendlyException throwable) {
			StringBuilder builder = new StringBuilder(event.getClient().getError());
			builder.append(" faild to load ").append(event.getArgs());
			event.reply(builder.toString());
			System.out.println("faild to load: " + event.getArgs());
		}
	}
}
