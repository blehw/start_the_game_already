package Assignment3;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DepthPlayer extends SampleGamer {

	final int limit = 12;

	private int maxScore(Role role, MachineState state, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role, state);
		}
		if (level >= limit) {
			return 0;
		}
		List<Move> moves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			int result = minScore(role, moves.get(i), state, level);
			if (result > score) {
				score = result;
			}
		}
		return score;
	}

	private int minScore(Role role, Move move, MachineState state, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state, role, move);
		int score = 100;
		for (int i = 0; i < jointMoves.size(); i++) {
			MachineState newState = getStateMachine().getNextState(state, jointMoves.get(i));
			int result = maxScore(role, newState, level + 1);
			if (result < score) {
				score = result;
			}
		}
		return score;
	}

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move action = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			int result = minScore(role, moves.get(i), state, 0);
			if (result > score) {
				score = result;
				action = moves.get(i);
			}
		}
		return action;
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

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

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

