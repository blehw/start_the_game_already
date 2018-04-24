package Assignment3;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class minimax_depth extends SampleGamer {

	long timeLimit;

	final int limit = 6;

	private int maxScore(Role role, MachineState state, StateMachine machine, int alpha, int beta, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (machine.findTerminalp(state)) {
			return machine.getGoal(state, role);
		}
		if (level > limit) {
			return mobility(role, state, machine);
		}
		List<Move> moves =	machine.getLegalMoves(state, role);
		for (Move move : moves) {
			if (System.currentTimeMillis() > timeLimit) { return alpha; }
			alpha = Math.max(alpha, minScore(role, move, state, machine,  alpha,  beta, level));
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private int minScore(Role role, Move move, MachineState state, StateMachine machine, int alpha, int beta, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
		for (List<Move> moves: jointMoves) {
			if (System.currentTimeMillis() > timeLimit) { return beta; }
			MachineState newState = machine.getNextState(state, moves);
			beta = Math.min(beta, maxScore(role, newState, machine, alpha, beta,level + 1));
			if (beta <= alpha) {
				return alpha;
			}
		}
		return beta;
	}

	private Move bestMove(Role role, MachineState state, StateMachine machine) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		List<Move> moves = machine.getLegalMoves(state, role);
		Move action = moves.get(0);
		int score = 0;
		for (Move move : moves) {
			if (System.currentTimeMillis() > timeLimit) { return action; }
			int result = minScore(role, move, state, machine,  0, 100, 0);
			if (result > score) {
				score = result;
				action = move;
			}
		}
		return action;
	}

	private int mobility(Role role, MachineState state, StateMachine machine) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		List<Move> moves = machine.getLegalMoves(state, role);
		List<Move> actions = machine.findActions(role);
		return ((moves.size() * 100) / actions.size());
	}

	private int reward(Role role, MachineState state, StateMachine machine) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		return machine.getGoal(state, role);
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
		long start = System.currentTimeMillis();

		timeLimit = timeout - 50;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = bestMove(getRole(), getCurrentState(), getStateMachine());

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
}
