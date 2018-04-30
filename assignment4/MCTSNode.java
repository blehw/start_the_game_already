package Assignment4;

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
	      this.visits = 0;
	      this.score = 0;
	    }


	public MCTSNode(MCTSNode parent, Move move, MachineState  state) {
	      this.parent = parent;
	      this.visits = 0;
	      this.move = move;
	      this.state = state;
	      this.score = 0;
	    }

	private double selectfn(MCTSNode node) {
		return node.score/node.visits
		+ Math.sqrt(2*Math.log(node.parent.visits)/node.visits);

	}


	public MCTSNode selectChild(MCTSNode node) {
		double score = 0;
		MCTSNode select = node.children.get(0);
		for(MCTSNode child : node.children) {
			if (child.visits == 0) {
				return child;
			}
			double selectVal = selectfn(child);
			if (selectVal > score) {
				score = selectVal;
				select = child;
			}
		}
		return select;
	}


	public void backpropagate(MCTSNode node, double score) {
		if (this.parent == null) { return; }
		this.score += score;
		this.visits++;
		backpropagate(node.parent, score);
	}



}
