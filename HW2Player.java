package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class HW2Player extends SampleGamer {

	/**
	 * All we have to do here is call the Player's initialize method with
	 * our name as the argument so that the Player GUI knows which name to
	 * have selected at startup. This is all you need in the main method
	 * of your own player.
	 */
	public static void main(String[] args) {
		Player.initialize(new HW2Player().getName());
	}

	/**
	 * Currently, we can get along just fine by using the Prover State Machine.
	 * We will implement a more optimized PropNet State Machine later. The Cached
	 * State Machine is a wrapper that reduces the number of calls to the Prover
	 * State Machine by returning results of method calls that have been made previously.
	 * (e.g. getNextState calls or getLegalMoves for the same combination of parameters)
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	/**
	 * If we wanted to use the metagame (or start) clock to compute something
	 * about the game (or explore the game tree), we could do so here. Since
	 * this is just a legal player, there is no need for such computation.
	 */



	/**
	 * Where your player selects the move they want to play. In-line comments
	 * explain each line of code. Your goal essentially boils down to returning the best
	 * move possible.
	 *
	 * The current state for the player is updated between moves automatically for you.
	 *
	 * The value of the timeout variable is the UNIX time by which you need to submit your move.
	 * You can determine how much time your player has left (in milliseconds) by using the following line of code:
	 * long timeLeft = timeout - System.currentTimeMillis();
	 *
	 * Make sure to submit your move before this time runs out. It's also a good
	 * idea to leave a couple seconds (2-4) as buffer for network lag/spikes and
	 * so that you don't overrun your time thus timing out (which plays
	 * a random move for you and counts as an error -- two very bad things).
	 */
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//Gets our state machine (the same one as returned in getInitialStateMachine)
		//This State Machine simulates the game we are currently playing.
		StateMachine machine = getStateMachine();
		//Gets the current state we're in (e.g. move 2 of a game of tic tac toe where X just played in the center)
		MachineState state = getCurrentState();
		//Gets our role (e.g. X or O in a game of tic tac toe)
		Role role = getRole();
		//Gets all legal moves for our player in the current state
		return bestMove(role, state, machine);
	}

	public Move bestMove(Role role, MachineState state, StateMachine machine)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		List<Move> legalMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		System.out.println(legalMoves);
		Move currentMove = legalMoves.get(0);
		int score = 0;
		for (int i = 0; i < legalMoves.size(); i++) {
			List<Move> new_move = new ArrayList<Move>();
			new_move.add(legalMoves.get(i));
			MachineState new_state = machine.getNextState(state, new_move);
			int result = maxScore(role, new_state, machine);
			System.out.println(result);
			if (result > score) {
				score = result;
				currentMove = legalMoves.get(i);
			}
		}
		return currentMove;
	}

	public int maxScore(Role role, MachineState state, StateMachine machine)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if (machine.findTerminalp(state)) {
			return machine.findReward(role, state);
		}
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < legalMoves.size(); i++) {
			List<Move> new_move = new ArrayList<Move>();
			new_move.add(legalMoves.get(i));
			MachineState new_state = machine.getNextState(state, new_move);
			int result = maxScore(role, new_state, machine);
			if (result > score) {
				score = result;
			}
		}
		return score;
	}

	/**
	 * Can be used for cleanup at the end of a game, if it is needed.
	 */
	@Override
	public void stop() {
		return;
	}

	/**
	 * Can be used for cleanup in the event a game is aborted while
	 * still in progress, if it is needed.
	 */
	@Override
	public void abort() {
		return;
	}

	/**
	 * Returns the name of the player.
	 */
	@Override
	public String getName() {
		return "player";
	}



}
