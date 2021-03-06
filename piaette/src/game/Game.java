package game;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import org.newdawn.slick.Animation;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.newdawn.slick.util.ResourceLoader;

import client.GameClient;
import utilities.PlayerDefinition;

public class Game extends BasicGame {

	// Soooo many attributes
	private int width, height, dingCounter, playerId, port;
	private float explX, explY, boomX, boomY;
	private long elapsedTime, gameLength;
	private boolean boom, explode, isRunning = false;
	private Player chaser, local;
	private ArrayList<Player> players;
	private ArrayList<PlayerDefinition> initialPlayerDefs;
	private Animation boomAnimate, intro, explodeAnimate, winner;
	private Audio ding, explosion;
	private MenuButton backButton;
	private Image gameBackground;
	private GameClient gameClient;
	private InetAddress hostAddress;
	private GameContainer gc;

	public Game(ArrayList<PlayerDefinition> pDefs, int playerId,
			InetAddress hostAddress, int port) {
		super("Piaette");
		width = GameInstantiator.width;
		height = GameInstantiator.height;
//		scale = GameInstantiator.scale;
		this.playerId = playerId;
		this.initialPlayerDefs = pDefs;
		this.hostAddress = hostAddress;
		this.port = port;

		gameLength = 30000;
	}

	private void createPlayers(ArrayList<PlayerDefinition> pDefs) {
		players = new ArrayList<Player>();
		Color color;
		try {

			for (PlayerDefinition pDef : pDefs) {
				boolean first = false;
				color = Color.red;
				if (pDef.getId() == playerId) {
					color = Color.green;
					first = true;
				}

				// float x, float y, int keyUp,int keyDown,int keyLeft,int
				// keyRight,String name,Color color,boolean first,int id
				Player player = new Player(pDef.getX(), pDef.getY(),
						Input.KEY_UP, Input.KEY_DOWN, Input.KEY_LEFT,
						Input.KEY_RIGHT, pDef.getName(), color, first,
						pDef.getId());

				players.add(player);

				if (pDef.getId() == playerId)
					local = player;

			}

		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	public void updatePlayer(PlayerDefinition pDef) {
		if (pDef.getId() == playerId)
			return;

		for (Player p : players) {
			if (p.id == pDef.getId()) {
				p.updateFromServer(pDef);
			}
		}

	}

	/*
	 * Ladda upp spelet (init' = initialize)
	 * 
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	@Override
	public void init(GameContainer gc) throws SlickException {
		createPlayers(initialPlayerDefs);

		this.gc = gc;

		Graphics g = gc.getGraphics();
		gameBackground = new Image("Graphics/game/background.png");

		g.setBackground(Color.black);

		// Ladda litta ljud
		try {
			explosion = AudioLoader.getAudio("WAV",
					ResourceLoader.getResourceAsStream("sounds/smallBomb.wav"));
			ding = AudioLoader.getAudio("WAV",
					ResourceLoader.getResourceAsStream("sounds/ding.wav"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Ladda animationer
		boomAnimate = new Animation(new SpriteSheet(
				"Graphics/animations/boom.png", 128, 128, Color.black), 50);
		boomAnimate.setLooping(false);

		explodeAnimate = new Animation(new SpriteSheet(
				"Graphics/animations/explosion.png", 128, 128, Color.black), 50);
		explodeAnimate.setLooping(false);

		intro = new Animation(new SpriteSheet("Graphics/animations/intro.png",
				128, 128), 250);
		intro.setLooping(false);

		winner = new Animation(new SpriteSheet(
				"Graphics/animations/winner.png", 512, 128), 100);

		// Knapp
		Image back = new Image("Graphics/menu/exit.png");
		Image backHover = new Image("Graphics/menu/exit-hover.png");
		backButton = new MenuButton(back, backHover,
				(width - back.getWidth()) / 2, height - 100);

		try {
			this.gameClient = new GameClient(new DatagramSocket(), hostAddress,
					port, this, local);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		chaser = players.get(0);
	}

	/*
	 * Rita players
	 * 
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer,
	 * org.newdawn.slick.Graphics)
	 */
	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		// Under gameplay
		g.drawImage(gameBackground, 0, 0);
		if (elapsedTime < 4000 || isRunning) {

			// Score at the bottom of everything
			int i = 0;
			for (Player p : players) {
				g.setColor(p.color);
				g.drawRect(20, 20 + i * 30, 100, 20);
				g.fillRect(20, 20 + i * 30, (gameLength - p.score)
						/ (gameLength / 100), 20);
				g.drawString((gameLength - p.score) / 1000 + "", 125,
						23 + i * 30);
				g.drawString(p.name, 150, 23 + i * 30);
				i++;
			}

			// Rita och animera spelarna
			for (Player p : players) {
				p.draw(g, chaser);
			}

			// esssss$$$$ssssssplosions
			if (explode) {
				explodeAnimate.draw(explX, explY);
			}
			if (boom) {
				boomAnimate.draw(boomX, boomY);
			}

			// För att rita ut introt
			if (elapsedTime < 4000)
				intro.draw(width / 2 - 64, height / 2 - 64);
		}

		else { // it's over. Show dah winner

			for (Player p : players) { // Finns bara en player i loopen.
				p.draw(g, chaser);
			}

			// WINNER! animation
			winner.draw(width / 2 - 256, height / 2 - 64);

			// Behövs för att låta losern explodera
			if (explode) {
				explodeAnimate.draw(explX, explY);
			}

			g.drawImage(backButton.getImage(), backButton.getMinX(),
					backButton.getMinY());

		}
	}

	/*
	 * körs varje frame. Logik som behöver uppdateras i realtid.
	 * 
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer,
	 * int)
	 */
	@Override
	public void update(GameContainer gc, int delta) throws SlickException {

		// Räknar total tid
		elapsedTime += delta;

		// Mouse & tangentbord input
		Input i = gc.getInput();

		// Animationslogik
		boomAnimate.update(delta);
		explodeAnimate.update(delta);
		if (boomAnimate.isStopped())
			boom = false;
		if (explodeAnimate.isStopped())
			explode = false;

		// Introanimationer
		if (!isRunning && elapsedTime > 3000 && elapsedTime < 5000) {
			ding.playAsSoundEffect(2f, 0.7f, false);
			isRunning = true;
		}
		// Ljudeffekter under introt
		else if (!isRunning && elapsedTime / 1000 >= dingCounter
				&& elapsedTime < 5000) {
			dingCounter++;
			ding.playAsSoundEffect(1f, 0.5f, false);
		}

		// Låt inte spelare styra under tiden introt körs
		if (elapsedTime < 3000)
			return;

		// Set the chaser from server
		if (chaser == null || chaser.id != gameClient.getChaser()) {
			for (Player p : players) {
				if (p.id == gameClient.getChaser()) {
					// if(System.currentTimeMillis()-playerDeath > 3000){
					youreIt(p);
					// }
					break;
				}
			}
		}

		// Pjättarn förlorar poäng
		if (chaser != null && elapsedTime > 3000 && chaser.equals(local)) {
			chaser.score += delta;
			if (chaser.score > gameLength)
				chaser.score = gameLength;
		}

		// Spelares styrning
		for (Player player : players) {
			// Player-objektet hanterar input själv
			if (player.id == playerId)
				player.handleInput(i);
			// Animation
			player.playerAnimation.update(delta);
			player.updateSpeed();

			// Om spelet är över, så kolla inte efter kollisioner
			if (!isRunning)
				break;

			// När tiden rinner ut
			// System.out.println(player.id+" has score "+player.score);
			if (player.score >= gameLength) {
				System.out.println("Found dead player");
				player.die();
				explode = true;
				explodeAnimate.restart();
				explosion.playAsSoundEffect(0.5f, 0.5f, false);
				explX = player.circle.getCenterX() - explodeAnimate.getWidth()
						/ 2;
				explY = player.circle.getCenterY() - explodeAnimate.getWidth()
						/ 2;
				chaser = null;

				// Om spelaren är den sista kvar = WINNER!
				if (players.size() == 2) {
					isRunning = false;
				}
				players.remove(player);
				break;
			}
		}

		if (!isRunning) { // Game is over
			if (backButton.clicked()) {
				gc.exit();
			}
		}
	}

	// Animationer och ljud för att bli pjättad
	private void youreIt(Player player) {
		ding.playAsSoundEffect(0.5f, 0.5f, false);
		chaser = player;
		chaser.freeze();
		boom = true;
		boomAnimate.restart();
		boomX = player.circle.getCenterX() - boomAnimate.getWidth() / 2;
		boomY = player.circle.getCenterY() - boomAnimate.getWidth() / 2;
	}

	@Override
	public boolean closeRequested() {
		// Not working properly yet
		gameClient.close();
		ding.stop();
		explosion.stop();
		ding = null;
		explosion = null;
		gc.exit();
		System.out.println("Trying to clean up");
		return true;
	}

}
