package Assignment7;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public class MCTSPlayerV4 extends SampleGamer {
	final int probes = 10;
	private long timeLimit;
	private long metaLimit;
	private boolean gameStarted = false;
	private boolean badState = false;
	private boolean mobMode = false;
	private boolean firstMove = true;
	private int levelMax = 3;
	private double C = 20;
	private StateMachine stateMachine;
	private SamplePropNetStateMachine propNet = new SamplePropNetStateMachine();

	private MCTSNode root = null;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (firstMove) {
			root = new MCTSNode(null, null, getCurrentState());
			firstMove = false;
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) {
			expand(root, 0);
			MCTSNode newRoot = root.children.get(0);
			newRoot.parent = null;
			root = newRoot;
			return moves.get(0);
		}
		while (System.currentTimeMillis() < timeLimit) {
			MCTSNode node = select(root);
			expand(node, 0);
			double score = monteCarlo(role, node.state, probes);
			backpropagate(node, score, 0);
		}
		Move move = root.children.get(0).move;
		MCTSNode newRoot = root.children.get(0);
		double score = 0;
		for (int i = 0; i < root.children.size(); i++) {
			long time;
			if (gameStarted) {
				time = timeLimit;
			} else {
				time = metaLimit;
			}
			if (System.currentTimeMillis() > time - 1500) {
				System.out.println("Child " + i);
				System.out.println("Visits: " + root.children.get(i).visits);
				if (root.children.get(i).visits != 0) {
					double nodeScore = root.children.get(i).score / root.children.get(i).visits;
					//double nodeScore = stateMachine.findReward(role, root.children.get(i).state);
					System.out.println("nodeScore: "+ nodeScore);
					if (nodeScore > score) {
						move = root.children.get(i).move;
						score = nodeScore;
						newRoot = root.children.get(i);
					}
				}
			} else {
				newRoot.parent = null;
				root = newRoot;
				//System.out.println("depthCharges: " + depthCharges);
				System.out.println("bestScore: " + score);
				return move;
			}
		}

		if (score == 0) {
			System.out.println("Reverting to mobility heuristic");
			for (int i = 0; i < root.children.size(); i++) {
				long time;
				if (gameStarted) {
					time = timeLimit;
				} else {
					time = metaLimit;
				}
				if (System.currentTimeMillis() > time - 1500) {
					double mobScore = stateMachine.getGoal(root.children.get(i).state, role);;
					/*if (root.children.get(i).children.size() == 0) {
						if (mobMode) {
							expand(root.children.get(i), 4000);
						} else {
							expand(root.children.get(i), 1000);
						}
					}
					for (int k = 0; k < root.children.get(i).children.size(); k++) {
						double nodeScore = ((double)stateMachine.findLegals(role, root.children.get(i).children.get(k).state).size() / stateMachine.findActions(role).size()) * 100;
						mobScore += nodeScore;
					}*/
					//mobScore = mobScore / root.children.get(i).children.size();
					/*if (Double.isNaN(mobScore)) {
						mobScore = 0;
					}*/
					System.out.println("mobScore: "+  mobScore);
					if (mobScore > score) {
						move = root.children.get(i).move;
						score = mobScore;
						newRoot = root.children.get(i);
					}
					int flip = new Random().nextInt(2);
					if (mobScore == score && flip == 0) {
						move = root.children.get(i).move;
						score = mobScore;
						newRoot = root.children.get(i);
					}
				} else {
					newRoot.parent = null;
					root = newRoot;
					//System.out.println("depthCharges: " + depthCharges);
					System.out.println("bestScore: " + score);
					return move;
				}
			}
			mobMode = true;
		} else {
			mobMode = false;
		}
		newRoot.parent = null;
		root = newRoot;
		//System.out.println("depthCharges: " + depthCharges);
		System.out.println("bestScore: " + score);
		return move;
	}

	private MCTSNode select(MCTSNode node) {
		long time;
		if (gameStarted) {
			time = timeLimit;
		} else {
			time = metaLimit;
		}
		if (System.currentTimeMillis() > time) {
			return node;
		}
		if (propNet.isTerminal(node.state)) {
			//System.out.println("terminal");
			return node;
		}
		if (node.visits == 0) {
			return node;
		}
		for (int i = 0; i < node.children.size(); i++) {
			if (node.children.get(i).visits == 0) {
				return node.children.get(i);
			}
		}
		double score = 0;
		MCTSNode result = node;
		for (int i = 0; i < node.children.size(); i++) {
			if (System.currentTimeMillis() > time) {
				return result;
			}
			double newscore = selectfn(node.children.get(i));
			if (newscore > score) {
				score = newscore;
				result = node.children.get(i);
			}
		}
		if (result == node) {
			return node;
		}
		return select(result);
	}

	private double selectfn(MCTSNode node) {
		return node.score/node.visits + C * Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
	}

	private void expand(MCTSNode node, long extraTime) throws MoveDefinitionException, TransitionDefinitionException {
		long time;
		if (gameStarted) {
			time = timeLimit + extraTime;
		} else {
			time = metaLimit;
		}
		if (System.currentTimeMillis() > time) {
			return;
		}
		if (!propNet.isTerminal(node.state)) {
			//System.out.println("expanding");
			List<Move> moves = stateMachine.getLegalMoves(node.state, getRole());
			for (int i = 0; i < moves.size(); i++) {
				if (System.currentTimeMillis() > time) {
					return;
				}
				List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(node.state, getRole(), moves.get(i));
				for (int j = 0; j < jointMoves.size(); j++) {
					if (System.currentTimeMillis() > time) {
						return;
					}
					MachineState newState = stateMachine.getNextState(node.state, jointMoves.get(j));
					MCTSNode newNode = new MCTSNode(node, moves.get(i), newState);
					newNode.parent = node;
					node.children.add(newNode);
				}
			}
		}
	}

	private void backpropagate(MCTSNode node, double score, int level) {
		long time;
		if (gameStarted) {
			time = timeLimit;
		} else {
			time = metaLimit;
		}
		if (System.currentTimeMillis() > time) {
			return;
		}
		node.visits = node.visits + 1;
		node.score = node.score + score;
		if (node.parent != null && !badState) {
			backpropagate(node.parent, score, level + 1);
		}
		if (badState) {
			if (level < levelMax && node.parent != null) {
				backpropagate(node.parent, score, level + 1);
			} else {
				badState = false;
				if (node.parent != null) {
					backpropagate(node.parent, 0, level + 1);
				}
			}
		}
	}

	private double monteCarlo(Role role, MachineState state, double count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double total = 0;
		for(int i = 0; i<count; i++) {
			long time;
			if (gameStarted) {
				time = timeLimit;
			} else {
				time = metaLimit;
			}
			if (System.currentTimeMillis() > time) {
				return total/count;
			}
			double score = depthCharge(role, state, 0);
			if (score < 0) {
				//return -50;
				total = total - (C * probes);
				//return total;
			} else {
				total += score;
			}
			//depthCharges += 1;
		}
		return total/count;
	}

	private double depthCharge(Role role, MachineState state, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		long time;
		if (gameStarted) {
			time = timeLimit;
		} else {
			time = metaLimit;
		}
		if (System.currentTimeMillis() > time) {
			return stateMachine.getGoal(state, role);
		}
		/*if (level > 10) {
			return stateMachine.getGoal(state, role);
		}*/
		if (propNet.isTerminal(state)) {
			double score = stateMachine.findReward(role, state);
			if (level < levelMax && score <= 0) {
				badState = true;
				//System.out.println(level);
				//int neg = -1 - level;
				//return neg;
				return -1;
			}
			return score;
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		Move move = moves.get(new Random().nextInt(moves.size()));
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, role, move);
		int rand = new Random().nextInt(jointMoves.size());
		List<Move> simMove = jointMoves.get(rand);
		MachineState newState = stateMachine.getNextState(state, simMove);
		return depthCharge(role, newState, level+1);
	}

	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.
	 *
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		gameStarted = true;
		firstMove = true;
		root = new MCTSNode(null, null, getCurrentState());
		// We get the current start time

		long start = System.currentTimeMillis();
		if (!mobMode) {
			timeLimit = timeout - 3000;
		} else {
			timeLimit = timeout - 5000;
		}

		stateMachine = getStateMachine();
		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = bestMove(getRole(), getCurrentState());
		long stop = System.currentTimeMillis();

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stop and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<Gdl> description = getMatch().getGame().getRules();
		propNet.initialize(description);
		/*List<Gdl> description = getMatch().getGame().getRules();
		propNet.initialize(description);*/
		//System.out.println("PropNet Size: " + propNet.getSize());

		/*metaLimit = timeout - ((timeout - System.currentTimeMillis())/ 2);
		Role role = getRole();
		double sum = 0;
		double num = 0;
		double varSum = 0;
		root = new MCTSNode(null, null, getCurrentState());
		while (System.currentTimeMillis() < metaLimit) {
			double score = monteCarlo(role, root.state, probes);
			backpropagate(root, score, 0);
			sum += score;
			num++;
			varSum = varSum + ((score - (sum / num)) * (score - (sum / num)));
		}
		double variance = Math.sqrt(varSum / (num - 1));
		if (!Double.isNaN(variance)) {
			C = variance;
		}
		System.out.println("C: " + C);*/
	}

}