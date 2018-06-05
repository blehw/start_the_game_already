package Assignment6;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayerV3 extends SampleGamer {
	final int probes = 10;
	private long timeLimit;
	private boolean badState = false;
	private boolean mobMode = false;
	private int levelMax = 3;
	private double C = 20;
	private int charges = 0;
	private long clock;
	private MCTSNode backprop;
	private StateMachine stateMachine;

	private MCTSNode root = null;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		charges = 0;
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) {
			expand(root);
			MCTSNode newRoot = root.children.get(0);
			newRoot.parent = null;
			root = newRoot;
			return moves.get(0);
		}
		MCTSNode nod = select(root);
		expand(nod);
		for (int i = 0; i < nod.children.size(); i++) {
			expand(nod.children.get(i));
		}
		while (System.currentTimeMillis() < timeLimit) {
			MCTSNode node = select(root);
			expand(node);
			double score = monteCarlo(role, node, probes);
			backpropagate(backprop, score, 0);
		}
		Move move = root.children.get(0).move;
		MCTSNode newRoot = root.children.get(0);
		double numSearched  = 0;
		double score = 0;
		double allMoves = root.children.size();
		for (int i = 0; i < root.children.size(); i++) {
			if (System.currentTimeMillis() > timeLimit - 2500) {
				System.out.println("Child " + i);
				System.out.println("Visits: " + root.children.get(i).visits);
				if (root.children.get(i).visits != 0) {
					numSearched++;
					double nodeScore = (root.children.get(i).score / root.children.get(i).visits);
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
				System.out.println("Charges: " + charges);
				System.out.println("bestScore: " + score);
				return move;
			}
		}

		if (score == 0 || (2 * numSearched < root.children.size())) {
			score = 0;
			System.out.println("Reverting to mobility heuristic");
			for (int i = 0; i < root.children.size(); i++) {
				long time;
				if (mobMode) {
					time = timeLimit + (clock / 2) - 2500;
				} else {
					time = timeLimit + 500;
				}
				if (System.currentTimeMillis() < time) {
					double moveP = 0;
					MCTSNode parent = root.children.get(i);
					moveP += parent.children.size();
					moveP = moveP / (allMoves * allMoves);
					double reward = stateMachine.findReward(role, root.children.get(i).state);
					double mobScore =  reward + moveP;
					System.out.println("mobScore " + i + ": " + mobScore);
					if (mobScore > score) {
						move = parent.move;
						score = mobScore;
						newRoot = parent;
					}
					int flip = new Random().nextInt(2);
					if (mobScore == score && flip == 0) {
						move = parent.move;
						score = mobScore;
						newRoot = parent;
					}
				} else {
					newRoot.parent = null;
					root = newRoot;
					mobMode = true;
					System.out.println("Charges: " + charges);
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
		System.out.println("Charges: " + charges);
		System.out.println("bestScore: " + score);
		return move;
	}

	private MCTSNode select(MCTSNode node) {
		if (System.currentTimeMillis() > timeLimit) {
			return node;
		}
		if (stateMachine.findTerminalp(node.state)) {
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
			if (System.currentTimeMillis() > timeLimit) {
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

	private void expand(MCTSNode node) throws MoveDefinitionException, TransitionDefinitionException {
		if (System.currentTimeMillis() > timeLimit) {
			return;
		}
		if (node.children.size() != 0) {
			return;
		}
		if (!getStateMachine().findTerminalp(node.state)) {
			List<Move> moves = stateMachine.getLegalMoves(node.state, getRole());
			for (int i = 0; i < moves.size(); i++) {
				if (System.currentTimeMillis() > timeLimit) {
					return;
				}
				List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(node.state, getRole(), moves.get(i));
				for (int j = 0; j < jointMoves.size(); j++) {
					if (System.currentTimeMillis() > timeLimit) {
						return;
					}
					MachineState newState = stateMachine.getNextState(node.state, jointMoves.get(j));
					MCTSNode newNode = new MCTSNode(node, moves.get(i), newState);
					newNode.parent = node;
					node.children.add(newNode);
					node.moves = stateMachine.getLegalMoves(newState, getRole());
					node.jointMoves = jointMoves;
				}
			}
		}
	}

	private void backpropagate(MCTSNode node, double score, int level) {
		if (System.currentTimeMillis() > timeLimit) {
			return;
		}
		node.visits = node.visits + 1;
		node.score = node.score + score;
		if (badState) {
			if (level < levelMax && node.parent != null) {
				backpropagate(node.parent, score, level + 1);
			} else {
				badState = false;
				if (node.parent != null) {
					backpropagate(node.parent, 0, level + 1);
				}
			}
		} else {
			if (node.parent != null) {
				backpropagate(node.parent, score, level + 1);
			}
		}
	}

	private double monteCarlo(Role role, MCTSNode node, double count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double total = 0;
		for(int i = 0; i<count; i++) {
			if (System.currentTimeMillis() > timeLimit) {
				return total/count;
			}
			double score = depthCharge(role, node, 0);
			charges++;
			if (score < 0) {
				total = total - 100;
			} else {
				total += score;
			}
		}
		return total/count;
	}

	private double depthCharge(Role role, MCTSNode node, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (System.currentTimeMillis() > timeLimit) {
			backprop = node;
			return stateMachine.getGoal(node.state, role);
		}
		if (stateMachine.findTerminalp(node.state)) {
			double score = stateMachine.findReward(role, node.state);
			if (level < levelMax && score <= 0) {
				badState = true;
				backprop = node;
				return -1;
			}
			backprop = node;
			return score;
		}
		if (node.moves == null) {
			List<Move> moves = stateMachine.getLegalMoves(node.state, role);
			Move move = moves.get(new Random().nextInt(moves.size()));
			List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(node.state, role, move);
			List<Move> simMove = jointMoves.get(new Random().nextInt(jointMoves.size()));
			MachineState newState = stateMachine.getNextState(node.state, simMove);
			MCTSNode newNode = new MCTSNode(node, move, newState);
			newNode.parent = node;
			node.children.add(newNode);
			node.moves = stateMachine.getLegalMoves(newState, getRole());
			node.jointMoves = jointMoves;
			return depthCharge(role, newNode, level+1);
		} else {
			int r = new Random().nextInt(node.moves.size());
			Move move = node.moves.get(r);
			for (int i = 0; i < node.children.size(); i++) {
				if (node.moves.get(r).equals(move)) {
					return depthCharge(role, node.children.get(i), level+1);
				}
			}
		}
		backprop = node;
		return -1;
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
		root = new MCTSNode(null, null, getCurrentState());
		// We get the current start time

		long start = System.currentTimeMillis();
		clock = timeout - start;
		if (!mobMode) {
			timeLimit = timeout - 3000;
		} else {
			timeLimit = timeout - (clock / 2);
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
		timeLimit = timeout - 3000;
		root = new MCTSNode(null, null, getCurrentState());
		stateMachine = getStateMachine();
		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		if (moves.size() == 1) {
			expand(root);
			MCTSNode newRoot = root.children.get(0);
			newRoot.parent = null;
			root = newRoot;
		}
		MCTSNode nod = select(root);
		expand(nod);
		for (int i = 0; i < nod.children.size(); i++) {
			expand(nod.children.get(i));
		}
		while (System.currentTimeMillis() < timeLimit) {
			MCTSNode node = select(root);
			expand(node);
			double score = monteCarlo(getRole(), node, probes);
			backpropagate(node, score, 0);
		}
		Move move = root.children.get(0).move;
		MCTSNode newRoot = root.children.get(0);
		double score = 0;
		for (int i = 0; i < root.children.size(); i++) {
			if (System.currentTimeMillis() > timeLimit - 2500) {
				System.out.println("Child " + i);
				System.out.println("Visits: " + root.children.get(i).visits);
				if (root.children.get(i).visits != 0) {
					double nodeScore = (root.children.get(i).score / root.children.get(i).visits);
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
				System.out.println("Charges: " + charges);
				System.out.println("bestScore: " + score);
			}
		}
	}

}
