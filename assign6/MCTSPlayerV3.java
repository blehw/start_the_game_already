package Assignment6;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayerV3 extends SampleGamer {
	final int probes = 10;
	private long timeLimit;
	private long metaLimit;
	private boolean gameStarted = false;
	private boolean badState = false;
	private boolean mobMode = false;
	//private boolean mobMode = false;
	private boolean firstMove = true;
	double C = 20;

	private MCTSNode root = null;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (firstMove) {
			root = new MCTSNode(null, null, getCurrentState());
			firstMove = false;
		}
		List<Move> moves = getStateMachine().getLegalMoves(state, role);
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
			if (System.currentTimeMillis() > time - 2000) {
				System.out.println("Child " + i);
				System.out.println("Visits: " + root.children.get(i).visits);
				if (root.children.get(i).visits != 0) {
					double nodeScore = root.children.get(i).score / root.children.get(i).visits;
					//double nodeScore = getStateMachine().findReward(role, root.children.get(i).state);
					System.out.println("nodeScore: "+ nodeScore);
					if (nodeScore > score) {
						move = root.children.get(i).move;
						score = nodeScore;
						newRoot = root.children.get(i);
					}
				}
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
				if (System.currentTimeMillis() > time - 2000) {
					double mobScore = getStateMachine().getGoal(root.children.get(i).state, role);;
					/*if (root.children.get(i).children.size() == 0) {
						if (mobMode) {
							expand(root.children.get(i), 4000);
						} else {
							expand(root.children.get(i), 1000);
						}
					}
					for (int k = 0; k < root.children.get(i).children.size(); k++) {
						double nodeScore = ((double)getStateMachine().findLegals(role, root.children.get(i).children.get(k).state).size() / getStateMachine().findActions(role).size()) * 100;
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
		if (getStateMachine().findTerminalp(node.state)) {
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
		if (!getStateMachine().findTerminalp(node.state)) {
			List<Move> moves = getStateMachine().getLegalMoves(node.state, getRole());
			for (int i = 0; i < moves.size(); i++) {
				if (System.currentTimeMillis() > time) {
					return;
				}
				List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(node.state, getRole(), moves.get(i));
				for (int j = 0; j < jointMoves.size(); j++) {
					if (System.currentTimeMillis() > time) {
						return;
					}
					MachineState newState = getStateMachine().getNextState(node.state, jointMoves.get(j));
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
			if (level < 1 && node.parent != null) {
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
			if (score == -1) {
				total = total - (C * probes);
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
			return getStateMachine().getGoal(state, role);
		}
		/*if (level > 10) {
			return getStateMachine().getGoal(state, role);
		}*/
		if (getStateMachine().findTerminalp(state)) {
			double score = getStateMachine().findReward(role, state);
			if (level < 1 && score <= 0) {
				badState = true;
				return -1;
			}
			return score;
		}
		List<Move> moves = getStateMachine().getLegalMoves(state, role);
		Move move = moves.get(new Random().nextInt(moves.size()));
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state, role, move);
		int rand = new Random().nextInt(jointMoves.size());
		List<Move> simMove = jointMoves.get(rand);
		MachineState newState = getStateMachine().getNextState(state, simMove);
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
			timeLimit = timeout - 4000;
		}

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
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
		PropNet pn;
		try {
			pn = OptimizingPropNetFactory.create(description);
			System.out.println("PropNet Size: " + pn.getSize());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

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
