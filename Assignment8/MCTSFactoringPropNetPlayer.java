package Assignment8;

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

import Assignment7.MCTSNode;


public class MCTSFactoringPropNetPlayer extends SampleGamer {



	final int probes = 25;
	int depthChargesSent = 0;
	private long timeLimit;
	private long metaLimit;
	private boolean gameStarted = false;
	private boolean badState = false;
	double C = 20;
	private StateMachine stateMachine;
	private SamplePropNetStateMachine propNet = new SamplePropNetStateMachine();


	private MCTSNode root = null;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {

		//List<Move> moves = getStateMachine().getLegalMoves(state, role);
		List<Move> moves = propNet.getLegalMoves(state, role);
		System.out.println(moves);
		expand(root, 0);

		while (System.currentTimeMillis() < timeLimit) {
			System.out.println("Select");
			long start = System.currentTimeMillis();
			MCTSNode node = select(root);
			System.out.println(System.currentTimeMillis() - start);
			start = System.currentTimeMillis();
			System.out.println("Expand");
			expand(node, 0);
			System.out.println(System.currentTimeMillis() - start);
			start = System.currentTimeMillis();
			System.out.println("MonteCarlo");
			double score = monteCarlo(role, node.state, probes);
			System.out.println(System.currentTimeMillis() - start);
			start = System.currentTimeMillis();
			System.out.println("Backprop");
			int max_level = backpropagate(node, score, 0);
			System.out.println(System.currentTimeMillis() - start);

		}
		Move move = null;
		double score = 0;
		for (MCTSNode child: root.children) {
				if (child.visits != 0) {
					double nodeScore = child.score / child.visits;
					System.out.println("nodeScore: "+ nodeScore);
					System.out.println("score: "+ child.score);
					System.out.println("nodeVisits: "+  child.visits);

					if (nodeScore > score) {

						move = child.move;
						score = nodeScore;
					}
				}
			}
		if (score == 0) {
			System.out.println("Reverting to mobility heuristic");
			for (MCTSNode child: root.children) {
					double mobScore = propNet.getGoal(child.state, role);;
					System.out.println("mobScore: "+  mobScore);
					if (mobScore > score) {
						move = child.move;
						score = mobScore;
					}
					int flip = new Random().nextInt(2);
					if (mobScore == score && flip == 0) {
						move = child.move;
						score = mobScore;
					}
				}
			}
		MCTSNode walker = root;



		System.out.println("bestScore: " + score);
		return move;
	}
	private void TreePrint( ) {

	}

	private MCTSNode select(MCTSNode node) {
		if (System.currentTimeMillis() > timeLimit) { return node;
		}
		if (propNet.isTerminal(node.state)) { return node; }
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
		if (System.currentTimeMillis() > timeLimit) { return; }

		if (!propNet.isTerminal(node.state)) {
			List<Move> moves = propNet.getLegalMoves(node.state, getRole());
			for (int i = 0; i < moves.size(); i++) {

				if (System.currentTimeMillis() > timeLimit) { return; }
				List<List<Move>> jointMoves = propNet.getLegalJointMoves(node.state, getRole(), moves.get(i));
				for (int j = 0; j < jointMoves.size(); j++) {

					if (System.currentTimeMillis() > timeLimit) { return; }
					MachineState newState = propNet.getNextState(node.state, jointMoves.get(j));
					MCTSNode newNode = new MCTSNode(node, moves.get(i), newState);
					newNode.parent = node;
					node.children.add(newNode);
				}
			}
		}
	}



	private double monteCarlo(Role role, MachineState state, double count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double total = 0;
		for(int i = 0; i<count; i++) {
			if (System.currentTimeMillis() > timeLimit) { return total/count; }
			double score = depthCharge(role, state, 0);
			if (score == -1) {
				total = total - (C * probes);
			} else {
				total += score;
			}
			depthChargesSent += 1;
		}
		return total/count;
	}

	private double depthCharge(Role role, MachineState state, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		while (!propNet.isTerminal(state)) {
			if (System.currentTimeMillis() > timeLimit) { return 0; }
			if (level > 50) {
				System.out.println("TOO DEEP");
				return 0;
			}
			List<Move> moves = propNet.getLegalMoves(state, role);
			if ( moves.size() == 0 ) {
				//System.out.println("NO MOVEs");
				return propNet.getGoal(state, role);
			} else {
				//System.out.println("moves found");
			}
			Move move = moves.get(new Random().nextInt(moves.size()));
			List<List<Move>> jointMoves = propNet.getLegalJointMoves(state, role, move);
			List<Move> simMove = jointMoves.get(new Random().nextInt(jointMoves.size()));
			state = propNet.getNextState(state, simMove);
			level++;
		}

		return propNet.getGoal(state, role);
	}

	private int backpropagate(MCTSNode node, double score, int level) {

		while (true) {
			if (System.currentTimeMillis() > timeLimit) { return level; }
			node.visits = node.visits + 1;
			node.score = node.score + score;
			if (score == 100) { level++; }
			if (node.parent == null) { break; }
			node = node.parent;
		}
		return level;
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
		root = new MCTSNode(null, null, getCurrentState());
		// We get the current start time
		timeLimit = timeout - 3000;
		depthChargesSent = 0;

		long start = System.currentTimeMillis();
		List<Move> moves = propNet.getLegalMoves(getCurrentState(), getRole());
		if (moves.size() == 1 ) {
			long stop = System.currentTimeMillis();
			notifyObservers(new GamerSelectedMoveEvent(moves, moves.get(0), stop - start));
			return moves.get(0);
		}
		Move selection = bestMove(getRole(), getCurrentState());

		long stop = System.currentTimeMillis();
		//long start = System.currentTimeMillis();
		System.out.println(depthChargesSent);


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
		propNet.factoringDetector();

	}




}
