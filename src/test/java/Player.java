import java.util.*;
import java.io.*;
import java.math.*;

class Coordinates
{
	int row, col;
	Coordinates(int col, int row)
	{
		this.row = row;
		this.col = col;
	}
	
	@Override
	public String toString() 
	{
		return col + " " + row;
	}
	
	Coordinates sum(Coordinates coordinates)
	{
		return new Coordinates(col + coordinates.col, row + coordinates.row);
	}

	Coordinates multiply(double constant)
	{
		return new Coordinates((int)(constant * col), (int)(constant * row));
	}

	// (col - i.row) * (cos + i.sin) = (col * cos + row * sin) - i(row * cos - sin)
	Coordinates rotate(double angle)
	{
		double cos = Math.cos(angle),
			sin = Math.sin(angle);
		return new Coordinates((int)(row * cos - sin * col), (int)(col * cos + row * sin));
	}
	
	Coordinates vector(Coordinates target)
	{
		return new Coordinates(target.col - col, target.row - row);
	}

	int scalar(Coordinates coordinates)
	{
		return row * coordinates.row + col * coordinates.col;
	}
	
	int square()
	{
		return scalar(this);
	}
	
	int squaredDistance(Coordinates coordinates)
	{
		return this.vector(coordinates).square();
	}
	
	int distance(Coordinates coordinates)
	{
		return (int)Math.sqrt(this.squaredDistance(coordinates));
	}
}

abstract class Action
{
	String text;
	Action(String text)
	{
		this.text = text;
	}
}

class Wait extends Action
{
	Wait(String text)
	{
		super(text);
	}
	
	@Override
	public String toString() 
	{
		return "WAIT";
	}
}

class Wind extends Action
{
	Coordinates target;
	final static int 
		COST = 10,
		RANGE = 1280,
		EFFECT = 2200;

	Wind(Coordinates target, String text)
	{
		super(text);
		this.target = target; 
	}
	
	@Override
	public String toString() 
	{
		return "SPELL WIND " + target + " " + text;
	}
}

class Shield extends Action
{
	int id;
	final static int 
		COST = 10,
		RANGE = 2200,
		COOLDOWN = 12;

	Shield(int id, String text)
	{
		super(text);
		this.id = id; 
	}
	
	@Override
	public String toString() 
	{
		return "SPELL SHIELD " + id + " " + text;
	}
}

class Control extends Action
{
	int id;
	Coordinates target;
	final static int 
		COST = 10,
		RANGE = 2200;

	Control(int id, Coordinates target, String text)
	{
		super(text);
		this.id = id;
		this.target = target; 
	}
	
	@Override
	public String toString() 
	{
		return "SPELL CONTROL " + id + " " + target.toString() + " " + text;
	}
}


class Move extends Action
{
	Coordinates target;
	
	Move(Coordinates target, String text)
	{
		super(text);
		this.target = target;
	}
	
	@Override
	public String toString() 
	{
		return "MOVE " + target.toString() + " " + text;
	}
}

abstract class Item
{
	int id;
	Coordinates position;
	int lastTurnUpdate = 1;
	boolean isControlled;
	
	Item(int id, int turn, Coordinates position, boolean isControlled)
	{
		 this(id, position, isControlled);
		 this.lastTurnUpdate = turn;	
	 }
	 
	Item(int id,  Coordinates position, boolean isControlled)
	{
		 this.id = id;
		 this.position = position;
		 this.isControlled = isControlled;
	}
	 
	void update(int turn, Coordinates position, boolean isControlled)
	{
		this.lastTurnUpdate = turn;
		this.position = position;
		this.isControlled = isControlled;
	}
}

class Spider extends Item 
{
	Coordinates direction;
	boolean nearBase;
	int threatFor, health, shieldLife;
	public static final int THREAT_NONE = 0,
			THREAT_FOR_ME = 1,
			THREAT_FOR_OPPONENT = 2,
			SPEED = 400,
			TARGET_BASE_RANGE = 5000,
			DAMAGE_BASE_RANGE = 300,
			DAMAGE = 1;
	
	Spider(int id, int turn, Coordinates position, Coordinates direction, boolean nearBase, int threatFor, int health, int shieldLife, boolean isControlled)
	{
		super(id, turn, position, isControlled);
		this.direction = direction;
		this.nearBase = nearBase;
		this.threatFor = threatFor;
		this.health = health;
		this.shieldLife = shieldLife;
		
	}
	
	void update(int turn, Coordinates position, Coordinates direction, boolean nearBase, int threatFor, int health, int shieldLife, boolean isControlled)
	{
		super.update(turn, position, isControlled);
		this.nearBase = nearBase;
		this.threatFor = threatFor;
		this.health = health;
		this.shieldLife = shieldLife;
	}
	
	@Override
	public String toString() 
	{
		return "" + id + ", near = " + nearBase + ", threat = " + threatFor; 
	}
}

class Hero extends Item
{
	Action action;
	boolean attack;
	Coordinates waitPosition;
	public static final int
		NB = 3,
		SPEED = 800,
		DAMAGE = 2;	
	
	Hero(int id, Coordinates position, boolean isControlled)
	{
		super(id, position, isControlled);
	}
}

class Game
{
	Player me, opponent;
	Map<Integer, Spider> spiders = new HashMap<>();
	int turn = 1;

	private Hero farthestHero(List<Hero> heroes, Coordinates position)
	{
		Optional<Hero> farthest = heroes.stream()
				.filter((hero) -> position.distance(hero.position) <= Control.RANGE)
				.min((hero1, hero2) -> hero1.position.squaredDistance(me.base) - hero1.position.squaredDistance(me.base));
		if (farthest.isPresent())
		{
			Hero hero = farthest.get();
			heroes.remove(hero);
			return hero;
		}
		return null;
	}

	
	private void playControl(List<Hero> heroes, List<Spider> spiders)
	{
		List<Spider> spidersTemp = new ArrayList<>(spiders);
		List<Hero> opponents = new ArrayList<>(opponent.heroes.values());
		Collections.shuffle(opponents);
		while(me.hasMana() && !opponents.isEmpty() && !heroes.isEmpty())
		{
			Hero opponentHero = opponents.get(0);
			if (opponent.base.distance(opponentHero.position) <= Spider.TARGET_BASE_RANGE)
			{
				Hero hero = farthestHero(heroes, opponentHero.position);
				if (hero != null)
				{
					hero.action = new Control(opponentHero.id, new Coordinates(Player.HEIGHT/2, Player.WIDTH/2), "Imperius !");
					me.mana -= Control.COST;
					break;
				}
			}
			opponents.remove(opponentHero);
		}
		while(me.hasMana() && !spidersTemp.isEmpty() && !heroes.isEmpty())
		{
			Spider spider = spidersTemp.get(0);
			if (spider.shieldLife == 0 
				&& spider.threatFor != Spider.THREAT_FOR_OPPONENT)
				if (spider.position.sum(spider.direction).distance(me.base) <= 3 * Spider.DAMAGE_BASE_RANGE
					|| spider.position.distance(opponent.base) < Spider.TARGET_BASE_RANGE + 2 * Spider.SPEED
					|| (me.mana > 100 && spider.position.distance(me.base) > Spider.TARGET_BASE_RANGE))
				{
					Hero hero = farthestHero(heroes, spider.position);
					if (hero != null)
					{
						hero.action = new Control(spider.id, opponent.base, "Controlling " + spider.id);
						me.mana -= Control.COST;
						spiders.remove(spider);
					}
				}
			spidersTemp.remove(spider);
		}
	}

	private Hero windableHero(List<Hero> heroes, Spider spider)
	{
		Coordinates position = spider.position;
		for (Hero hero : heroes)
		{
			if (hero.position.distance(position) <= Wind.RANGE)				 
			{
				heroes.remove(hero);
				return hero;
			}
		}
		return null;
	}

	private void playWind(List<Hero> heroes, List<Spider> spiders)
	{
		List<Spider> spidersTemp = new ArrayList<>(spiders);
		while(me.hasMana() && !spidersTemp.isEmpty() && !heroes.isEmpty())
		{
			Spider spider = spidersTemp.get(0);
			if (spider.shieldLife == 0 && spider.nearBase) 
			{	
				Hero hero = windableHero(heroes, spider);
				if (hero != null)
				{
					hero.action = new Wind(opponent.base, "Winding " + spider.id);
					me.mana -= Wind.COST;
					spiders.remove(spider);
				}
			}
			spidersTemp.remove(spider);
		}
	}

	private Hero closestHero(List<Hero> heroes, Spider spider)
	{
		Coordinates position = spider.position.sum(spider.direction);
		Optional<Hero> closest = heroes.stream()
			.min((hero1, hero2) -> hero1.position.squaredDistance(position) - hero2.position.squaredDistance(position));
		if (!closest.isPresent()) 
			return null;
		Hero hero = closest.get();
		if (spider.threatFor == Spider.THREAT_FOR_ME)
		{
			if (hero.attack 
				&& !spider.nearBase)
				return null;
		}
		else
		{
			if (!hero.attack 
				&& spider.position.distance(me.base) > spider.position.distance(opponent.base))
				return null;
			if (hero.attack
				&& spider.position.distance(me.base) <= spider.position.distance(opponent.base))
				return null;
		}
		heroes.remove(hero);
		return hero;
	}
	
	private void playHeroes(List<Hero> heroes, List<Spider> spiders)
	{
		while(!spiders.isEmpty() && !heroes.isEmpty())
		{
			Spider spider = spiders.get(0);
			spiders.remove(spider);
			if (spider.threatFor != Spider.THREAT_FOR_OPPONENT)
			{
				Hero hero = closestHero(heroes, spider);
				if (hero != null)
					hero.action = new Move(spider.position, "Targeting " + spider.id);
			}
		}
	}

	private Hero shieldableHero(List<Hero> heroes, Coordinates position)
	{
		for (Hero hero : heroes)
		{
			if (hero.position.distance(position) < Shield.RANGE)
			{
				heroes.remove(hero);
				return hero;
			}
		}
		return null;
	}

	private void playShield(List<Hero> heroes, List<Spider> spiders)
	{
		List<Spider> spidersTemp = new ArrayList<>(spiders);
		while(me.hasMana() && !spidersTemp.isEmpty() && !heroes.isEmpty())
		{
			Spider spider = spidersTemp.get(0);
			if (spider.shieldLife == 0 
				&& spider.threatFor == Spider.THREAT_FOR_OPPONENT
				&& spider.position.distance(opponent.base) < Spider.SPEED * Shield.COOLDOWN)
			{	
				Hero hero = shieldableHero(heroes, spider.position);
				if (hero != null)
				{
					hero.action = new Shield(spider.id, "Shield " + spider.id);
					me.mana -= Shield.COST;
					spiders.remove(spider);
				}
			}
			spidersTemp.remove(spider);
		}
	}	

	private void playWait(List<Hero> heroes)
	{
		for (Hero hero : heroes)
			hero.action = new Move(hero.waitPosition, "Waiting");
	}
	
	void play()
	{
		me.clearActions();
		List<Spider> spiders = new ArrayList<>(this.spiders.values());
		List<Hero> heroes = new ArrayList<>(me.heroes.values());
		Coordinates[] defaultCoordinates = 
			{opponent.base.multiply(0.6).sum(new Coordinates(Player.WIDTH/2, me.base.row).multiply(0.4)),
			me.base.multiply(0.55).sum(new Coordinates(opponent.base.col, (int)(Player.HEIGHT/2.5)).multiply(0.45)), 
			me.base.multiply(0.5).sum(new Coordinates(Player.WIDTH/3, opponent.base.row).multiply(0.5)) 
					};
		int index = 0;
		for (Hero hero : heroes)
		{
			hero.attack = index == 0; 
			hero.waitPosition = defaultCoordinates[index++];
		}
		Collections.sort(spiders, (spider, other) -> 
		{
			if (spider.nearBase != other.nearBase)
				return (spider.nearBase ? -1 : 0) - (other.nearBase ? -1 : 0);
			// TODO commencer par les boucliers
			if (spider.threatFor != other.threatFor)
				return (spider.threatFor == Spider.THREAT_FOR_ME ? -1 : 0) - (other.threatFor == Spider.THREAT_FOR_ME ? -1 : 0);
			return me.base.squaredDistance(spider.position) - me.base.squaredDistance(other.position);
		}
		);
		playShield(heroes, spiders);
		playControl(heroes, spiders);
		playWind(heroes, spiders);
		playHeroes(heroes, spiders);
		playWait(heroes);
	}
}

class Player
{
	final public static int
			WIDTH = 17630,
			HEIGHT = 9000; 
	
	boolean me;
	int health, mana;
	Map<Integer, Hero> heroes = new HashMap<>();
	Coordinates base;
	
    static Scanner in = new Scanner(System.in);

	Player(boolean me, int health, int mana, Coordinates base)
	{
		this.me = me;
		this.health = health;
		this.mana = mana;
		this.base = base;
	}
	
	void update(int health, int mana)
	{
		this.mana = mana;
		this.health = health;
	}
	
	void clearActions()
	{
		for(Hero hero : heroes.values())
			hero.action = null;
	}
	
	boolean hasMana()
	{
		return mana >= Wind.COST;
	}
	
	String getActions()
	{
		String actions = "";
		for (Hero hero : heroes.values())
			actions += hero.action.toString() + "\n" ;
		return actions;
	}
	
	static Game inputFirstTurn()
	{
		Game game = new Game();
        int baseX = in.nextInt(); // The corner of the map representing your base
        int baseY = in.nextInt();
        int heroesPerPlayer = in.nextInt(); // Always 3
        int my_health = in.nextInt(); // Your base health
        int my_mana = in.nextInt(); // Ignore in the first league; Spend ten mana to cast a spell
        game.me = new Player(true, my_health, my_mana, new Coordinates(baseX, baseY));
        int opponent_health = in.nextInt(); // Your base health
        int opponent_mana = in.nextInt(); // Ignore in the first league; Spend ten mana to cast a spell
        game.opponent = new Player(false, opponent_health, opponent_mana, new Coordinates(WIDTH - baseX, HEIGHT - baseY));
        int entityCount = in.nextInt(); // Amount of heros and monsters you can see
        for (int i = 0; i < entityCount; i++) {
            int id = in.nextInt(); // Unique identifier
            int type = in.nextInt(); // 0=monster, 1=your hero, 2=opponent hero
            int x = in.nextInt(); // Position of this entity
            int y = in.nextInt();
            int shieldLife = in.nextInt(); // Ignore for this league; Count down until shield spell fades
            boolean isControlled = in.nextInt() == 1; // Ignore for this league; Equals 1 when this entity is under a control spell
            int health = in.nextInt(); // Remaining health of this monster
            int vx = in.nextInt(); // Trajectory of this monster
            int vy = in.nextInt();
            int nearBase = in.nextInt(); // 0=monster with no target yet, 1=monster targeting a base
            int threatFor = in.nextInt(); // Given this monster's trajectory, is it a threat to 1=your base, 2=your opponent's base, 0=neither
            Coordinates position = new Coordinates(x, y);
            switch (type)
            {
            case 0 : game.spiders.put(id, new Spider(id, 1, position, new Coordinates(vx, vy), nearBase == 1, threatFor, health, shieldLife, isControlled));
            	break;
            case 1 : game.me.heroes.put(id, new Hero(id, position, isControlled));
            	break;
            case 2 : game.opponent.heroes.put(id, new Hero(id, position, isControlled));
        		break;
            }
        }
        return game;
	}
	
	static void inputOtherTurns(Game game)
	{
        int my_health = in.nextInt(); // Your base health
        int my_mana = in.nextInt(); // Ignore in the first league; Spend ten mana to cast a spell
        game.me.update(my_health, my_mana);
        int opponent_health = in.nextInt(); // Your base health
        int opponent_mana = in.nextInt(); // Ignore in the first league; Spend ten mana to cast a spell
        game.opponent.update(opponent_health, my_mana);
        int entityCount = in.nextInt(); // Amount of heros and monsters you can see
        for (int i = 0; i < entityCount; i++) {
            int id = in.nextInt(); // Unique identifier
            int type = in.nextInt(); // 0=monster, 1=your hero, 2=opponent hero
            int x = in.nextInt(); // Position of this entity
            int y = in.nextInt();
            int shieldLife = in.nextInt(); // Ignore for this league; Count down until shield spell fades
            boolean isControlled = in.nextInt() == 1; // Ignore for this league; Equals 1 when this entity is under a control spell
            int health = in.nextInt(); // Remaining health of this monster
            int vx = in.nextInt(); // Trajectory of this monster
            int vy = in.nextInt();
            int nearBase = in.nextInt(); // 0=monster with no target yet, 1=monster targeting a base
            int threatFor = in.nextInt(); // Given this monster's trajectory, is it a threat to 1=your base, 2=your opponent's base, 0=neither
            Coordinates position = new Coordinates(x, y);
            switch (type)
            {
            case 0 : 
            	if (game.spiders.containsKey(id))
            		game.spiders.get(id).update(game.turn, position, new Coordinates(vx, vy), nearBase == 1, threatFor, health, shieldLife, isControlled);
            	else
            		game.spiders.put(id, new Spider(id, game.turn, position, new Coordinates(vx, vy), nearBase == 1, threatFor, health, shieldLife, isControlled));
            	break;
            case 1 : game.me.heroes.get(id).update(game.turn, position, isControlled);
            	break;
            case 2 : if (game.opponent.heroes.containsKey(id))
            		game.opponent.heroes.get(id).update(game.turn, position, isControlled);
	            else
	            	game.opponent.heroes.put(id, new Hero(id, position, isControlled));
        		break;
            }
        }
        new ArrayList<>(game.spiders.values()).stream()
        	.filter((spider) -> spider.lastTurnUpdate != game.turn)
        	.forEach((spider) -> game.spiders.remove(spider.id));
	}
	
    public static void main(String args[]) 
    {
    	Game game = inputFirstTurn();
        // game loop
        while (true) {
        	game.play();
    		System.out.print(game.me.getActions());
    		game.turn++;
            inputOtherTurns(game);
        }
    }
}