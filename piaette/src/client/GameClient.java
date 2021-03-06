package client;

import game.Game;
import game.Player;

import java.net.DatagramSocket;
import java.net.InetAddress;

import utilities.PlayerDefinition;

public class GameClient {
	private DatagramSocket socket;
	private InetAddress hostAddress;
	private int hostPort, piaette;
	private Game game;
	private Player player;
	private GameDownStream down;
	private GameUpStream up;
	private PlayerDefinition pDef;

	public GameClient(DatagramSocket socket, InetAddress hostAddress,
			int hostPort, Game game, Player player) {

		this.game = game;
		this.player = player;
		this.pDef = new PlayerDefinition(player.name, player.id);

		this.socket = socket;
		this.hostAddress = hostAddress;
		this.hostPort = hostPort;

		down = new GameDownStream(this);
		up = new GameUpStream(this);

		down.start();
		up.start();
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public InetAddress getHostAddress() {
		return hostAddress;
	}

	public int getHostPort() {
		return hostPort;
	}

	public void updatePlayer(int id, float x, float y, float r, long timer) {
		PlayerDefinition pDef = new PlayerDefinition(null, id);
		pDef.updateX(x);
		pDef.updateY(y);
		pDef.updateRotation(r);
		pDef.updateTimer(timer);
		game.updatePlayer(pDef);
	}

	public PlayerDefinition getPlayerInfo() {
		pDef.updateX(player.getX());
		pDef.updateY(player.getY());
		pDef.updateRotation(player.getDirection());
		pDef.updateTimer(player.score);
		return pDef;
	}

	public void setChaser(int piaetteId) {
		this.piaette = piaetteId;
	}

	public int getChaser() {
		return piaette;
	}

	public void close() {
		down.interrupt();
		up.interrupt();
	}
}
