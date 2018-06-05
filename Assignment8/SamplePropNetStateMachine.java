package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	markBases(state.getContents());
    	run();
    	return propmarkp(propNet.getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	markBases(state.getContents());
    	run();
    	Set<Proposition> rewards = new HashSet<Proposition>();
    	for (Role r: getRoles()) {
          	if (r.equals(role)) {
          		rewards = propNet.getGoalPropositions().get(role);
          		break;
          	}
          }
    	for (Proposition reward : rewards) {
    		Component c =(Component) reward;
    		if (propmarkp(c)) {
    			return getGoalValue(reward);

    		}
    	}
    	return 0;
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {

    	return null;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.
        return null;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
    	markBases(state.getContents());
    	run();

    	List<Role> roles = getRoles();
    	Map<Role, Set<Proposition>> legals = new HashMap<Role, Set<Proposition>>();
    	search: {
	    	for (int i = 0; i < roles.size(); i++) {
	    		if (role.getName() == roles.get(i).getName()) {
	    			//System.out.println("Hey");
	    			legals = propNet.getLegalPropositions();
	    			break search;
	    		}
	    	}
    	}
    	List<Move> moves = new ArrayList<Move>();
    	Set<Proposition> legalMoves = new HashSet<Proposition>();
    	legalMoves = legals.get(role);
    	for (Proposition p : legalMoves) {
    		if (propmarkp(p)) { moves.add(getMoveFromProposition(p)); }
    	}
    	return moves;
    }



	@Override
	public List<List<Move>> getLegalJointMoves(MachineState state, Role role, Move move)
            throws MoveDefinitionException {
    	return super.getLegalJointMoves(state, role, move);
    }



    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	markBases(state.getContents());
    	markActions(moves);
    	run();
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p: propNet.getBasePropositions().values()) {
			if (p.getSingleInput().getValue()) {
				contents.add(p.getName());
			}
		}
    	for(Proposition p : propNet.getInputPropositions().values()) {
    		p.setValue(false);
    	}

    	return new MachineState(contents);
    }


    /**
     * Helper Functions
     *
     */


    public void markBases(Set<GdlSentence> sentences) {
    	for (GdlSentence s: propNet.getBasePropositions().keySet()) {
    		Proposition p = propNet.getBasePropositions().get(s);
    		p.setLast(p.getValue());
    		if (sentences.contains(s)) {
    			p.setValue(true);
    		} else {
    			p.setValue(false);
    		}
    	}
    }

    public void markActions(List<Move> moves) {
    	List<GdlSentence> sentences= toDoes(moves);
    	for (GdlSentence s : sentences) {
    		if(propNet.getInputPropositions().containsKey(s)) {
    			propNet.getInputPropositions().get(s).setValue(true);
    		}
    	}
    }



    private boolean propmarkp(Component c) {
    	String type = c.toString();
    	if (c instanceof Proposition) {
    		//WE HAVE A PROPOSITION
    		Proposition p = (Proposition)c;
	       	Map<GdlSentence, Proposition> basePropositions = propNet.getBasePropositions();
	    	Map<GdlSentence, Proposition> inputPropositions = propNet.getInputPropositions();
	    	if (basePropositions.containsKey(p.getName())) {
	    		//BASE
	    		return p.getValue();
	    	}
	    	if (inputPropositions.containsKey(p.getName())) {
	    		//INPUT
	    		return p.getValue();
	    	}

	    	//VIEW
	    	return propmarkp(p.getSingleInput());

    	} else {
    		//WE HAVE A CONNECTIVE
    		if (type.contains("NOT")) {	return propmarknegation(c); }

    		if (type.contains("AND")) { return propmarkconjunction(c); }

    		if (type.contains("OR")) { return propmarkdisjunction(c); }

    		if (c instanceof Constant) { return c.getValue(); }
    	}
    	return false;
    }


	private void run() {
		for (Proposition p: ordering) {
			p.setValue(p.getSingleInput().getValue());
		}

	}


    private boolean propmarknegation(Component not) {

    	Component source = not.getSingleInput();
    	return !propmarkp(source);
    }

    private boolean propmarkconjunction(Component and) {

      	Set<Component> sources = and.getInputs();
      	for (Component source: sources) {
      		if (!propmarkp(source)) { return false; }
      	}
    	return true;
    }

    private boolean propmarkdisjunction(Component or) {

      	Set<Component> sources = or.getInputs();
      	for (Component source: sources) {
      		if (propmarkp(source)) { return true; }
      	}
    	return false;
    }


    public void factoringDetector() {
    	//independent subgames
    	if (subgameDetector()) {

    	}
    	if (terminationDetector()) {

    	}
//    	if (actionDetector()) {
//
//    	}

    	//independent terminations


    	//
    }

    private boolean subgameDetector() {
    	//check over basepropositions()
    	Map<GdlSentence, Proposition> basePropsMap = propNet.getBasePropositions();
    	Collection<Proposition> baseProps = basePropsMap.values();
    	Map<GdlSentence, Proposition> inputPropsMap = propNet.getInputPropositions();
    	Collection<Proposition> inputProps = inputPropsMap.values();
    	Collection<Proposition> newBaseProps = new HashSet<Proposition>();
    	Collection<Component> viewed = new HashSet<Component>();
    	Proposition terminal = propNet.getTerminalProposition();
    	Queue<Component> q = new LinkedList<>();
    	//Add initial inputs to queue
    	for (Component input : terminal.getInputs()) {
    		q.add(input);
    	}
    	while(!q.isEmpty()) {
    		Component current = q.poll();
    		if (viewed.contains(current)) { continue; }
    		if (baseProps.contains(current)) {
    			newBaseProps.add((Proposition)current);
    		}
    		if (!inputProps.contains(current)) {
    			for (Component input : current.getInputs()) {
    				q.add(input);
    			}
    		}
    		viewed.add(current);
    	}
    	System.out.println("Done with search");
    	double sizeRatio = (propNet.getBasePropositions().values().size()/newBaseProps.size());
    	System.out.println(propNet.getBasePropositions().values().size());
    	System.out.println(newBaseProps.size());

    	if (sizeRatio > 1) {
    		return true;
    	}

    	return false;
    }


    private boolean terminationDetector() {
    	//NOT SURE ABOUT THIS IMPLEMENTATION
    	Proposition terminal = propNet.getTerminalProposition();
    	System.out.println(terminal.getInputs());
    	for (Component input : terminal.getInputs()) {
    		String type = input.toString();
     		if (type.contains("AND")) {
    			//conjunction
     			System.out.println("Conjunction");
     			return true;
    		}
    		if (type.contains("OR")) {
    			//disjunction
     			System.out.println("Disjunction");
     			return true;
    		}
    	}

    	return false;
    }

    private boolean actionDetector() {
    	return false;
    }



    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.
        //steps:
        	// 1. remove init proposition
        	// 2. remove base and input propositions
        	// 3. order the rest of components

    	components.remove(propNet.getInitProposition());

        for (Proposition p : propNet.getBasePropositions().values()) { components.remove(p); }

        for (Proposition p : propNet.getInputPropositions().values()) { components.remove(p); }

		while (!components.isEmpty()) {

			List<Component> current = new ArrayList<Component>(components);
			//loop over all the components
			for ( Component c : components ) {
				//Check if the inputs have been added
				boolean inputs_done = true;
				for (Component input: c.getInputs()) {
					if (components.contains(input)) {
						inputs_done = false;
						break;
					}
				}

				if (inputs_done) {
					//We have handled all the inputs
					current.remove(c);
					if (propositions.contains(c))
						order.add((Proposition) c);
				}
			}
			components = current;
		}
		return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBases()
    {
    	Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values()) {
        	boolean update = p.getSingleInput().getValue();
        	p.setValue(update);
	        if (propmarkp(p))
	        {
	               contents.add(p.getName());
	        }
          }
          return new MachineState(contents);
    }


}