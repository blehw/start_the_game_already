package Assignment6;

import java.util.ArrayList;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;


public class MCTSNode {
	public double score;
	public int visits;
	public Move move;
	public MachineState  state;
	public ArrayList<MCTSNode> children;
	public MCTSNode parent;

	public MCTSNode(MCTSNode parent) {
		this.parent = parent;
	    this.children = new ArrayList<MCTSNode>();
	    this.visits = 0;
	    this.score = 0;
	}

	public MCTSNode(MCTSNode parent, Move move, MachineState  state) {
		this.parent = parent;
	    this.children = new ArrayList<MCTSNode>();
	    this.visits = 0;
	    this.move = move;
	    this.state = state;
	    this.score = 0;
	}
}


