import java.util.TreeMap;

public class CFRPoker{
	public static void main(String[] args){
		int num_cards = 13;
		int iter = 125_000_000;
		PokerTrainer pt = new PokerTrainer(num_cards);
		pt.train(iter);
	}
}

class PokerTrainer{
	private final int NUM_ACTIONS = 2; //number of legal actions in many cases
	private final int NUM_ACTIONS_FACING_BET = 3; //number of legal actions in special case where player is facing a bet
	private final double ANTE_SIZE = 1.0, BET_SIZE = 2.0, RAISE_SIZE = 6.0;
	private final String[] action_set = new String[]{"p", "b", "r"}; //set of all possible actions (note: pass=fold/check, bet=bet/call)
	private TreeMap<String, Node> node_map; //dictionary mapping game information sets to strategy nodes
	private int[] cards;
	
	public PokerTrainer(int num_cards){
		node_map = new TreeMap<String, Node>();
		cards = new int[num_cards];

		//initialize deck of cards, which starts at 2
		for (int i = 2; i <= num_cards+1; i++){
			cards[i-2] = i;
		}
	}
	
	//plays against itself, updating strategy for given number of iterations
	public void train(int iterations){
		double utility = 0;
		
		for (int i = 0; i < iterations; i++){
			//randomize the cards
			shuffle_cards();
			//start a new hand with an empty history and both player's probabilities initialized to 1
			utility += cfr(cards, "", 1, 1);
		}
		
		//print the average strategy of each node, which converges to Nash equilibrium as number of iterations increases
		for (Node n : node_map.values()){
			System.out.println(n);
		}
		
		System.out.println("Game value for player 0 (out of position): " + utility/iterations);
	}
	
	//shuffles the deck of cards
	private void shuffle_cards(){
		for (int c1 = cards.length - 1; c1 > 0; c1--) { 
                int c2 = (int)(Math.floor(Math.random() * (c1 + 1))); //random value between 0 and c1
                int temp = cards[c1];
                cards[c1] = cards[c2];
                cards[c2] = temp;
        }
	}
	
	
	//parameters: 
		//cards: the shuffled deck of cards. cards[0] is player 0's card, card[1] is player 1's card.
		//history: the game history at current point in the hand. empty if the hand has just started.
		//p0: probability of reaching this node in the game tree based on player 0's actions.
		//p1: probability of reaching this node based on player 1's actions
	//returns: the average value of the player reaching that node
	private double cfr(int[] cards, String history, double p0, double p1){
		int num_plays = history.length(); //number of actions that have been played in total so far
		int player = num_plays % 2; //player 0 = out of position (first to act), player 1 = in position (second to act)
		int opponent = 1 - player; //opponent of current player
		boolean double_bet, terminal_pass, raise_call, player_card_higher;
		
		//check to see if its a terminal node (hand has ended according to the rules of poker)
		if (num_plays > 1){ //hand can only end if both players have acted
			double_bet = history.substring(num_plays-2).equals("bb"); //hand ends if there is a bet followed by a bet
			terminal_pass = history.charAt(num_plays-1) == 'p'; //hand ends if both players acted and last action was a pass
			raise_call = history.substring(num_plays-2).equals("rb"); //hand ends if there was a raise followed by a bet
			player_card_higher = cards[player] > cards[opponent];
			
			if (double_bet){
				//showdown, highest card wins
				return player_card_higher ? ANTE_SIZE + BET_SIZE : -1 * (ANTE_SIZE + BET_SIZE);
			} else if (terminal_pass){
				if (history.equals("pp")){
					//showdown
					return player_card_higher ? ANTE_SIZE : -1 * ANTE_SIZE;
				} else if (history.equals("pbp")){ 
					//ip player wins
					return player == 1 ? ANTE_SIZE : -1 * ANTE_SIZE;
				} else if (history.equals("bp")) {
					//oop player wins
					return player == 0 ? ANTE_SIZE : -1 * ANTE_SIZE;
				} else if (history.equals("pbrp")){
					//oop player wins
					return player == 0 ? (ANTE_SIZE + BET_SIZE) : -1 * (ANTE_SIZE + BET_SIZE);
				} else {
					assert(history.equals("brp"));
					//ip player wins
					return player == 1 ? (ANTE_SIZE + BET_SIZE) : -1 * (ANTE_SIZE + BET_SIZE);
				}
			} else if (raise_call){
				//showdown
				return player_card_higher ? (ANTE_SIZE + RAISE_SIZE) : -1 * (ANTE_SIZE + RAISE_SIZE);	
			}
			
		}
		
		//if we reach this point, we are not at terminal node
		//try to grab the strategy node for the current hand history
		String info_set = Integer.toString(cards[player]) + history;
		Node node = node_map.get(info_set);
		
		//if node doesn't exist, create one and add it to dictionary
		if (node == null){
			if (info_set.charAt(info_set.length()-1) == 'b'){
				//3 choices when facing a bet (pass, bet, raise)
				node = new Node(info_set, NUM_ACTIONS_FACING_BET);
			} else {
				//2 actions when facing a non-bet (pass, bet)
				node = new Node(info_set, NUM_ACTIONS);
			}
			node_map.put(info_set, node);
		}
		
		double node_utility = 0; //will hold the average utility (weighted sum) of reaching node
		double[] utility = new double[node.ACTIONS]; //will hold utility of performing each action
		double[] regrets = new double[node.ACTIONS]; //will hold regret of performing each action
		double[] strategy = player == 0 ? node.get_strategy(p0) : node.get_strategy(p1); //current strategy being played by the node
		double opp_prob = opponent == 0 ? p0 : p1; //probability of reaching this node based on opponents previous strategies
		String new_action; //current action being considered
		double threshold = 0.01; //threshold for calculating the utility of an action

		//calculate utility for node
		for (int action = 0; action < node.ACTIONS; action++){
			//choose a new action
			new_action = action_set[action];
			
			//easy optimization:
			//if probability of taking an action is below threshold, keep utility for that action at 0
			if (strategy[action] < threshold){
				continue;
			} 
			//traverse down the subtree to determine the utility of our action
			//note: multiply by -1 because next node visited is from the opponent's perspective, so it will return the opponent's utility
			else if (player == 0){
				utility[action] = -1 * cfr(cards, history + new_action, p0 * strategy[action], p1);
			} else {		
				utility[action] = -1 * cfr(cards, history + new_action, p0, p1 * strategy[action]);
			}

			//update weighted utility average for current node
			node_utility += strategy[action] * utility[action];
		}
		
		//calculate regrets for current node
		//note: these regrets are weighted by the probability of getting to this node based on the opponent's previous actions
		for (int action = 0; action < node.ACTIONS; action++){
			regrets[action] = (utility[action] - node_utility) * opp_prob;
		}
		
		//give current node regrets of choosing each action
		node.give_regrets(regrets);
		
		//return value of node
		return node_utility;
	}
}

class Node{
	public final int ACTIONS; //number of legal actions at this node
	private String info_set; //informaton available at this node
	private double[] strategy_sum; //cumulative sum of all strategies played
	private double[] regret_sum; //cumulative sum of regrets for each action
	
	public Node(String info_set, int num_actions){
		this.info_set = info_set;
		ACTIONS = num_actions;
		strategy_sum = new double[ACTIONS];
		regret_sum = new double[ACTIONS];
	}
	
	//realization_weight: the probability of reaching this node based on this player's previous actions
	//returns: the next strategy to be played by the node
	public double[] get_strategy(double realization_weight){
		double normalizing_sum = 0;
		double[] strategy = new double[ACTIONS];
		
		//new strategy only takes into account positive regrets
		for (int i = 0; i < ACTIONS; i++){
			strategy[i] = regret_sum[i] > 0 ? regret_sum[i] : 0;
			normalizing_sum += strategy[i];
		}
		
		//normalize the strategy (or if normalizing sum is non-positive, choose an even mixed strategy)
		for (int i = 0; i < ACTIONS; i++){
			if (normalizing_sum > 0){
				strategy[i] /= normalizing_sum;
			} else {
				strategy[i] = 1.0 / ACTIONS;
			}
			
			//weight the strategy sum by the probability of this player's actions getting us to this node in the game tree
			strategy_sum[i] += strategy[i] * realization_weight;
		}
		
		return strategy;
	}
	
	//returns: the average strategy of all strategies played (approaches Nash equilibrium as number of training iterations increase)
	public double[] get_avg_strategy(){
		double normalizing_sum = 0;
		double[] avg_strategy = new double[ACTIONS];
		
		for (int i = 0; i < ACTIONS; i++){
			avg_strategy[i] += strategy_sum[i];
			normalizing_sum += avg_strategy[i];
		}
		
		//normalize the strategy sum
		for (int i = 0; i < ACTIONS; i++){
			if (normalizing_sum > 0){
				avg_strategy[i] /= normalizing_sum;
			} else {
				avg_strategy[i] = 1.0 / ACTIONS;
			}
		}
		
		return avg_strategy;
	}
	
	//update cumulative regrets for each action
	public void give_regrets(double[] regrets){
		for (int action = 0; action < ACTIONS; action++){
			regret_sum[action] += regrets[action];
		}
	}
	
	//printing the node will display the information set with the average strategy
	public String toString(){
		double[] avg_strategy = this.get_avg_strategy();
		String summary;
		
		summary = String.format("%s: pass:[%f], bet:[%f]", info_set, avg_strategy[0], avg_strategy[1]); 

		if (ACTIONS == 3) {
			summary += String.format(", raise:[%f]", avg_strategy[2]);
		}
		
		return summary;
	}
}
