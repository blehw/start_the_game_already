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
	final int maxLevel = 1;
	private long timeLimit;

	private MCTSNode root;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		root = new MCTSNode(null, null, getCurrentState());
		expand(root);
		while (System.currentTimeMillis() < timeLimit) {
			//System.out.println(root.children.get(0).visits);
			System.out.println("before select");
			MCTSNode node = select(root);
			System.out.println("after select");
			if (node != root) {
				if (!getStateMachine().findTerminalp(node.parent.state)) {
					System.out.println("Oh no");
					expand(node);
					System.out.println("before montecarlo");
					double score = monteCarlo(role, node.state, probes);
					System.out.println("after montecarlo");
					backpropagate(node, score);
				}
			} else {
				System.out.println("Oh no");
				expand(node);
				System.out.println("before montecarlo");
				double score = monteCarlo(role, node.state, probes);
				System.out.println("after montecarlo");
				backpropagate(node, score);
			}
		}
		Move move = root.children.get(0).move;
		double score = 0;
		System.out.println("hello");
		for (int i = 0; i < root.children.size(); i++) {
			System.out.println(root.children.get(i).parent.visits);
			if (root.children.get(i).visits != 0) {
				System.out.println("hi");
				System.out.println(root.children.get(i).score / root.children.get(i).visits);
				if (root.children.get(i).score / root.children.get(i).visits > score) {
					move = root.children.get(i).move;
					score = (root.children.get(i).score) / (root.children.get(i).visits);
					System.out.println(score);
				}
			}
		}
		//System.out.println(score);
		return move;
	}

	private MCTSNode select(MCTSNode node) {
		if (System.currentTimeMillis() > timeLimit) {
			return null;
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
				if (!getStateMachine().findTerminalp(node.children.get(i).state)) {
					score = newscore;
					result = node.children.get(i);
				}
			}
		}
		return select(result);
	}

	private double selectfn(MCTSNode node) {
		int C = 20;
		return node.score/node.visits + C * Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
	}

	private void expand(MCTSNode node) throws MoveDefinitionException, TransitionDefinitionException {
		if (System.currentTimeMillis() > timeLimit) {
			return;
		}
		if (getStateMachine().findTerminalp(node.state)) {
			return;
		}
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
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

	private void backpropagate(MCTSNode node, double score) {
		System.out.println("p");
		if (System.currentTimeMillis() > timeLimit) {
			return;
		}
		node.visits = node.visits + 1;
		System.out.println(node.visits);
		//System.out.println(score);
		node.score = node.score + score;
		if (node.parent != null) {
			//System.out.println("hello");
			backpropagate(node.parent, score);
		}
	}

	private double monteCarlo(Role role, MachineState state, double count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double total = 0;
		for(int i = 0; i<count; i++) {
			total += depthCharge(role, state);
		}
		return total/count;
	}

	private double depthCharge(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (getStateMachine().findTerminalp(state)) {
			//System.out.println("State: " + state);
			//System.out.println("Role: " + role);
			double score = getStateMachine().findReward(role, state);
			//System.out.println("Score: " + score);
			return score;
		}

		List<Move> moves = getStateMachine().getLegalMoves(state, role);
		Move move = moves.get(new Random().nextInt(moves.size()));
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state, role, move);
		List<Move> simMove = jointMoves.get(new Random().nextInt(jointMoves.size()));
		MachineState newState = getStateMachine().getNextState(state, simMove);
		if (getStateMachine().findTerminalp(newState)) {
			double score = getStateMachine().findReward(role, state);
			//System.out.println("Score: " + score);
			return score;
		}
		return depthCharge(role, newState);
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
		//long start = System.currentTimeMillis();

		timeLimit = timeout - 3000;

		//List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		Move selection = bestMove(getRole(), getCurrentState());

		//long stop = System.currentTimeMillis();

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stop and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		//notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

}
