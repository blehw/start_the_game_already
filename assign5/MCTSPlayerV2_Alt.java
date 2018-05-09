package Assignment4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends SampleGamer {
	final int probes = 10;
	private long timeLimit;
	private long metaLimit;
	private boolean gameStarted = false;
	double C = 20;

	//TEMP
	private int num_probes = 0;
	//

	private MCTSNode root;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {

		root = new MCTSNode(null, null, getCurrentState());
		double best = 0;
		while (System.currentTimeMillis() < timeLimit) {
			MCTSNode node = select(root);
			expand(node);
			double score = monteCarlo(role, node.state, probes);
			backpropagate(node, score);
			// PRINT SCORE UPDATE
			if (score > best) {best = score;}
			System.out.println("Current Score with  " +  Long.toString(timeLimit - System.currentTimeMillis()) + "left" );
			System.out.println(best);
			//
		}
		Move move = root.children.get(0).move;
		double score = 0;
		for (MCTSNode node: root.children) {
			if (getStateMachine().getGoal(node.state, getRole()) == 100) {
				return node.move;
			} //Updated this to return a winning move if we have noe

			if (node.visits != 0) {
				double nodeScore = node.score / node.visits;
				System.out.println("nodeScore: "+ nodeScore);
				if (nodeScore > score) {
					move = node.move;
					score = nodeScore;
				}
			}
		}

		if (score == 0) {
			System.out.println("Reverting to mobility heuristic");
			for (int i = 0; i < root.children.size(); i++) {
				double nodeScore = ((double)getStateMachine().findLegals(role, root.children.get(i).state).size() / getStateMachine().findActions(role).size()) * 100;
				System.out.println("mobScore: "+ nodeScore);
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
		long time;
		if (gameStarted) {
			time = timeLimit;
		} else {
			time = metaLimit;
		}
		if (System.currentTimeMillis() > time) {
			return null;
		}

		if (node.visits == 0) {
			return node;
		}
		double score = 0;
		MCTSNode result = node;

		for (int i = 0; i < node.children.size(); i++) {
			if (System.currentTimeMillis() > timeLimit) {
				return result;
			}
			if (node.children.get(i).visits == 0) {
				return node.children.get(i);
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
		long time;
		if (gameStarted) {
			time = timeLimit;
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



	private void trimTree(MCTSNode node) {
		//for children of node, apply a dropout on a certain amount of nodes
		//

	}



	private void backpropagate(MCTSNode node, double score) {
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
		if (node.parent != null) {
			backpropagate(node.parent, score);
		}
	}

	private double monteCarlo(Role role, MachineState state, double count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double total = 0;
		for(int i = 0; i<count; i++) {
			//TEMP

			num_probes++;

			//TEMP

			long time;
			if (gameStarted) {
				time = timeLimit;
			} else {
				time = metaLimit;
			}
			if (System.currentTimeMillis() > time) {
				return total/i; //I updated this to be /i, as we have iterated i times at this point
			}
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

		long time;
		if (gameStarted) {
			time = timeLimit;
		} else {
			time = metaLimit;
		}
		if (System.currentTimeMillis() > time) {
			return getStateMachine().getGoal(state, role);
		}

		if (getStateMachine().findTerminalp(state)) {
			double score = getStateMachine().findReward(role, state);
			/*if (level < 1 && score <= 0) {
				return -1;
			}*/
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
		// We get the current start time
		//TEMP
		num_probes = 0;
		//TEMP
		long start = System.currentTimeMillis();
		timeLimit = timeout - 3000;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		System.out.println(moves);
		Move selection = moves.get(0); //Select the first move as default
		if (moves.size() != 1) { selection = bestMove(getRole(), getCurrentState()); }
		long stop = System.currentTimeMillis();
		System.out.println("Probes sent:");
		System.out.println(num_probes);
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
		metaLimit = timeout - 5000;
		List<Double> scores = new ArrayList<>();
		Role role = getRole();
		root = new MCTSNode(null, null, getCurrentState());
		while (System.currentTimeMillis() < metaLimit) {
			double score = monteCarlo(role, root.state, probes);
			backpropagate(root, score);
			scores.add(score);
		}
		double sum = 0;
		for (int i = 0; i < scores.size(); i++) {
			sum += scores.get(i);
		}
		double avg = sum / scores.size();
		double varSum = 0;
		for (int i = 0; i < scores.size(); i++) {
			varSum = varSum + ((scores.get(i) - avg) * (scores.get(i) - avg));
		}
		double variance = Math.sqrt(varSum / (scores.size() - 1));
		if (!Double.isNaN(variance)) {
			C = Math.sqrt(varSum / (scores.size() - 1));
		}
		System.out.println("C: " + C);
	}

}
