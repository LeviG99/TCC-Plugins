package visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;

public class AssignmentVisitor extends ASTVisitor {
    private List<ExpressionStatement> assignments = new ArrayList<>();
    private String variableName;
    private VisitorUtil util = new VisitorUtil();
	private boolean changeFault;
	private String leftHand;

    public AssignmentVisitor(String name, boolean b, String line) {
    	super();
    	variableName = name;
    	changeFault = b;
    	leftHand = line;
    }

    public boolean visit(Assignment node) {
    	SimpleNameVisitor simpleNameVisitor = new SimpleNameVisitor();
    	
    	node.getLeftHandSide().accept(simpleNameVisitor);
    	
    	Iterator<SimpleName> namesIt = simpleNameVisitor.getNames().iterator();
    	
    	while(namesIt.hasNext()&& util.counter(node)) {
    		if(namesIt.next().toString().equals(variableName) && !(node.getParent().getParent() instanceof TryStatement)) {
    			assignments.add((ExpressionStatement)node.getParent());
    			break;
    		}
    	}
    	if(node.getLeftHandSide().toString().equals(leftHand) && changeFault) {
    		changeFault = false;
    		node.setRightHandSide(node.getAST().newNullLiteral());
        	visit(node);
    	}

    	return true;
    }

    public List<ExpressionStatement> getAssignments() {
        return assignments;
    }
}