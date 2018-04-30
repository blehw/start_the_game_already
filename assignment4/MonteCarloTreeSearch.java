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

public class MonteCarloTreeSearch extends SampleGamer {
	final int probes = 4;
	final int maxLevel = 1;
	private long timeLimit;

	private MCTSNode root;

	private Move bestMove(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		root = new MCTSNode(null);
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move action = moves.get(0);
		if(moves.size() == 1)
			return moves.get(0);

		for (Move move: moves) {
			if (System.currentTimeMillis() > timeLimit) { return action; }
			//generate child for every move
			MCTSNode child = new MCTSNode(root,move,state);
			child.visits++;
			child.score = monteCarlo(role, state, probes);
			root.children.add(child);
		}
		System.out.println(root.children);

		while(System.currentTimeMillis() > timeLimit) {
			MCTSNode start = explore(root); //now i have bottom node
			expandNode(start); //now i have expanded node with children
			simulate(start, role);
		}

		return root.bestChild(root).move;

	}


	private MCTSNode explore(MCTSNode node) {
		MCTSNode curr = node;
		while(!curr.children.isEmpty()) {
			curr = curr.selectChild(curr);
		}
		return curr;
	}

	private void expandNode(MCTSNode node) throws MoveDefinitionException, TransitionDefinitionException {
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(node.state, getRole(), node.move);
		for(List<Move> move: jointMoves) {
				MachineState newState = getStateMachine().getNextState(node.state, move);
				MCTSNode child = new MCTSNode(node, move.get(0), newState);
				node.children.add(child);
		}
	}

	private void simulate(MCTSNode node, Role role) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		for (MCTSNode child: node.children) {
			child.score = monteCarlo(role, child.state, probes);
			child.backpropagate(child, child.score);
		}
	}




	private int monteCarlo(Role role, MachineState state, int count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		int total = 0;
		for(int i = 0; i<count; i++)
		{
			total += depthCharge(role, state);
		}

		return total/count;
	}

	private int depthCharge(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		if (getStateMachine().findTerminalp(state))
			return getStateMachine().getGoal(state, role);

		List<Role> roles = getStateMachine().getRoles();
		List<Move> moves = new ArrayList<Move>();;

		for(int i = 0; i<roles.size(); i++)
		{
			List<Move> options = getStateMachine().getLegalMoves(state, roles.get(i));
			moves.add(options.get(new Random().nextInt(options.size())));
		}
		MachineState newState = getStateMachine().getNextState(state, moves);
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
		long start = System.currentTimeMillis();

		timeLimit = timeout - 2000;

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

}