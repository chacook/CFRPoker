# CFRPoker
Learning a game theory optimal strategy for 1 card poker using counterfactual regret minimization

### Counterfactual regret
We're playing Rock, Paper, Scissors. I choose rock and you choose paper, so I lose. Looking back, I can consider the other actions I could have taken. I have some counterfactual regret (say 1) that I didn't play paper because I would have tied by playing paper. But I have even more counterfactual regret (say 2) that I didn't play scissors because playing scissors would have won me the game. (Counterfactual regrets can also be negative: if I play scissors and win, I'm glad that I didn't play rock or paper.) We will see that counterfactual regrets can be used to inform our future play.

### Optimal strategy
I can use a strategy by determining in advance how I will play. There are pure strategies and mixed strategies. A pure strategy in Rock, Paper, Scissors could be playing paper every time. A mixed strategy could be playing rock 20%, paper 30%, and scissors 50%. In Rock, Paper, Scissors, the optimal strategy is an equal mixed strategy: playing rock, paper, and scissors each 1/3 of the time. In this case, the opponent cannot make a strategy change that will increase their chances of winning a game. It is considered a defensive strategy, since its aim is to prevent being exploited, rather than trying to maximally exploit their opponent's strategy.

### Using counterfactual regret to create an optimal strategy
Counterfactual regret minimization can be used to closely approximate an optimal strategy. The idea is to play games against yourself, keep track of your positive and negative regrets, and update your strategy before every game. A new strategy is determined in proportion to positive regrets. For instance, if your total regrets for rock, paper, and scissors are 2, 3, and 1 respectively, your strategy for the next game will be to play rock 2/6, paper 3/6, and scissors 1/6 of the time. This way the actions you regret playing the most will be played most often. If you have no positive regrets whatsoever, then play an equal mixed strategy for the next game. Following this procedure, both versions of yourself will continually improve to keep up with the other. At the end of self-play, you take an average of all the strategies you played. As the number of games played approaches infinity, that average will approach an optimal strategy.

### 1 card poker
In this variant, the deck contains only 13 cards (2 through Ace, single suit). Each of the two players is dealt a single card and antes 1 chip at the start of each hand. There is a two bet maximum, meaning that once a bet (2 chips) has been raised (6 chips), no more raising can occur. Hands can end in three main ways: one player bets and the other folds, both players check and the hand goes to showdown, or a bet is called and the hand goes to showdown. At showdown, the player with the highest card wins. For an example hand, suppose the out of position player (OOP) is dealt an Ace and the in position player (IP) is dealt a King. OOP checks, IP bets, OOP raises, and IP calls. Now the cards are compared and OOP wins since Ace is higher than King.

However, unlike Rock, Paper, Scissors, poker is a sequential game. Players alternate turns until the hand is over. At each decision point, players have an information set available to them. For poker, the information set is just the list of actions that have already occured in the hand, plus the player’s private card. Just like with Rock, Paper, Scissors, I can use self-play to improve my strategy in poker, the only difference is I need a strategy for each decision point in the game tree. (For instance, I  need a strategy for facing a bet with a Jack, for facing a check with a Queen, and so on). But the basic idea is the same: I can make a choice, track my counterfactual regrets, and use those regrets to come up with a new strategy.

Most times in poker, it's hard to say whether some decision was good or bad because the hand doesn’t immediately end and there are still lots of ways it might play out. But this is where self-play becomes especially important. When I’m playing myself, I can calculate the probability and value of each outcome to improve my strategy. If I repeat this process enough, I will have an optimal strategy for each decision point in the game tree. I’ll call this set of strategies a game plan.

The game tree for 1 card poker is below. White nodes in the game tree are decision nodes, where a player must make a decision. The grey nodes are terminal nodes, where the hand ends and one player wins the pot.

![Image of Game Tree](https://github.com/chacook/CFRPoker/blob/master/game_tree.png)

### Interpreting the results of the code
Unlike Rock, Paper, Scissors where there is just one optimal strategy, for 1 card poker there are infinitely many optimal game plans. When we run the code, we will get an approximation of one of them. The optimal game plan will consist of a strategy for each information set, which corresponds to a decision node in the game tree.

There are 5 legal actions in poker: we can fold, check, bet, call, and raise. For simplicity, in the code, these have been combined into 3 actions: pass (which combines check and fold), bet (which combined bet and call), and raise. Each possible decision node will have a strategy dictating how often to pass, bet, and raise (if facing a bet).

The sample output below tells us that, according to this optimal game plan, when we are dealt a 2 and are facing a pass (check), we should pass (check behind) about 3.5% of the time and bet about 96.5% of the time.
>2p: pass:[0.034064], bet:[0.965936]

If we decide to bet with a 2 and then get raised, we look to the strategy for the corresponding information set, which tells us that we should almost always pass (fold).
>2pbr: pass:[0.990224], bet:[0.009776]

This makes sense. If we have a 2, our opponent must have a card higher than 2, calling the raise would always lose us the hand. (Note: with a larger training sample, the betting frequency would be even closer to 0. It is also possible to "purify" strategies by changing any sufficiently small probabilities to 0 and then normalizing.)

It's worth noting that, if we always follow our optimal game plan, there may be decision nodes in the game tree that we will never actually reach. For example, when we are first to act with an Ace (14), the strategy from this game plan is to always pass (check).
>14: pass:[1.000000], bet:[0.000000]

So, if we follow this game plan, we will never be in a situation where we are dealt an Ace, bet, and then get raised. This means the strategy for that information set will never actually be needed.
>14br: pass:[0.500000], bet:[0.500000]

This is important to be able to make sense of the game plan. At times the strategy for a particular information set can seem irrational, like the one above where we should seemingly be willing to fold the best possible hand.

By following the optimal game plan from the output of the program, we play a defensive strategy that limits the amount our opponent can exploit us. But this does not mean playing the strategy for a single information set will prevent us from being exploited. The counterfactual regret minimization algorithm provides us with a holistic system: we only play soundly from a game theory point of view by following the game plan entirely.

Source: An Introduction to Counterfactual Regret Minimization by Todd W. Neller and Marc Lanctot.
