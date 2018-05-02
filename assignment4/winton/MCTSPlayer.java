package Assignment4;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends SampleGamer {
	final int probes = 4;
	private long timeLimit;

	private MCTSNode root;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		root = new MCTSNode(null, null, getCurrentState());
		while (System.currentTimeMillis() < timeLimit) {
			MCTSNode node = select(root);
			expand(node);
			double score = monteCarlo(role, node.state, probes);
			backpropagate(node, score);
		}
		Move move = root.children.get(0).move;
		double score = 0;
		for (int i = 0; i < root.children.size(); i++) {
			System.out.println("Visits: " + root.children.get(i).visits);
			if (root.children.get(i).visits != 0) {
				double nodeScore = root.children.get(i).score / root.children.get(i).visits;
				System.out.println("nodeScore: "+ nodeScore);
				if (nodeScore > score) {
					move = root.children.get(i).move;
					score = nodeScore;
				}
			}
		}
		System.out.println("bestScore: " + score);
		return move;
	}

	private MCTSNode select(MCTSNode node) {
		if (System.currentTimeMillis() > timeLimit) {
			return null;
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
		double C = 50;
		return node.score/node.visits + C * Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
	}

	private void expand(MCTSNode node) throws MoveDefinitionException, TransitionDefinitionException {
		if (System.currentTimeMillis() > timeLimit) {
			return;
		}
		if (!getStateMachine().findTerminalp(node.state)) {
			List<Move> moves = getStateMachine().getLegalMoves(node.state, getRole());
			for (int i = 0; i < moves.size(); i++) {
				List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(node.state, getRole(), moves.get(i));
				for (int j = 0; j < jointMoves.size(); j++) {
					MachineState newState = getStateMachine().getNextState(node.state, jointMoves.get(j));
					MCTSNode newNode = new MCTSNode(node, moves.get(i), newState);
					newNode.parent = node;
					node.children.add(newNode);
				}
			}
		}
	}

	private void backpropagate(MCTSNode node, double score) {
		if (System.currentTimeMillis() > timeLimit) {
			return;
		}
		node.visits = node.visits + 1;
		node.score = node.score + score;
		if (node.parent != null) {
			backpropagate(node.parent, score);
		}
	}

	private double monteCarlo(Role role, MachineState state, double count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double total = 0;
		for(int i = 0; i<count; i++) {
			double score = depthCharge(role, state, 0);
			if (score == -1) {
				total = total - (probes * 100);
			} else {
				total += score;
			}
		}
		return total/count;
	}

	private double depthCharge(Role role, MachineState state, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (getStateMachine().findTerminalp(state)) {
			double score = getStateMachine().findReward(role, state);
			if (level < 1 && score <= 0) {
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
		// We get the current start time

		timeLimit = timeout - 3000;

		Move selection = bestMove(getRole(), getCurrentState());

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stop and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		return selection;
	}

}
