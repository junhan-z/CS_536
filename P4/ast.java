import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a Mini program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);
    
    /**
       This is the method in ASTnode used to check declaration in the Symbol table
    */
    protected void tableCheck(IdNode id, Sym s, SymTable st){

	String idName;
	if(id.getSym().isStruct())
	    idName = "struct " + id.getIDName();
	else
	    idName = id.getIDName();

	try{
	    st.addDecl(idName, s);

	}catch(DuplicateSymException e){
	    // debug
	    // System.out.print("Symbol Table");
	    // st.print();
	    // System.err.println("Declaration: "+ s.getType() + " " + id + ";");
	    id.errorReport("Multiply declared identifier");
	}catch(EmptySymTableException e){
	    id.errorReport("Error scope in varDeclNode");
	}
	// return s;
    }

    /** Error Message Handle*/
    protected void dead(String msg){
	System.err.println(msg);
	ErrMsg.setIsAnalyzePassed(false);
    }
    public boolean isPassedNameAnalyze(){
	return ErrMsg.isAnalyzePassed();
    }

    /** Verbose Info */
    protected void echo(String msg){
	System.out.println(msg);
    }

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }
}

// **********************************************************************
// ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;

	/* initialize a new symbol table start from the program node
	   IDEA: 
	   pass an empty from the root (i.e. the program node) to its leaves.
	   add scope and declaration accordingly and check the declaration at the same time.
	*/
	st = new SymTable(); 
    }

    public void nameAnalyze(){
	myDeclList.nameAnalyze(st);
	// echo("<<\nName anaylze finished");
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    // 2 kids
    private SymTable st;
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public void nameAnalyze(SymTable st){
	Iterator<DeclNode> it = myDecls.iterator();
        try {
            while (it.hasNext()) {
		DeclNode td = it.next();
		td.nameAnalyze(st);
	    }

        } catch (NoSuchElementException ex) {
            dead("unexpected NoSuchElementException in DeclListNode.nameAnalyze");
        }
    }
    /**
       This method should only be called via struct node, which is used to generate all the field of one specific struct declaration
       @return HashMap<String, Sym> a hashmap of all the fields of a struct
    */

    public HashMap<String, Sym> getStructVars(SymTable st){
	HashMap<String, Sym> h = new HashMap<String, Sym>();
    	Iterator<DeclNode> it = myDecls.iterator();

	st.addScope();	
    	while(it.hasNext()){
	    VarDeclNode td = (VarDeclNode)(it.next());
	    // check if the declare itself is legal
	    td.nameAnalyze(st);

	    IdNode ID = td.getID();
	    // System.out.println("structID: " + structID.getIDName());

	    String id = ID.getIDName();
	    String declType = td.getType().getTypeName();

	    Sym s;
	    // if it is a struct declaration inside a struct
	    if(ID.getSym().isStruct()){
	    	s = new Sym(declType);
	    	s.setStructMap(ID.getSym().getStructVars());
	    	s.setStruct(true);
	    }
	    else{ // a normal type
	    	s = new Sym(declType);
	    }

	    // check the declaration inside struct declaration
	    // actually just put will be fine as each id has been checked before (td.nameAnalyze(st))
	    if(! h.containsKey(id)){
	    	h.put(id, s);
	    }

    	}
	// remove this scope, no further useage..
	try{
	    st.removeScope();
	}catch(EmptySymTableException e){
	    dead("Error in DeclListNode, Symbol Table is empty already:");
	}

    	return h;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            dead("unexpected NoSuchElementException in DeclListNode.print");
        }
    }

    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public void nameAnalyze(SymTable st){
	st.addScope(); // add a scope for current function
	Iterator<FormalDeclNode> it = myFormals.iterator();
	try {
            while (it.hasNext()) {
		FormalDeclNode tf = it.next();
		tf.nameAnalyze(st);
	    }

        } catch (NoSuchElementException ex) {
            dead("unexpected NoSuchElementException in DeclListNode.nameAnalyze");
        }

    }

    public List<String> getFormalListSyms(){
	List<String> fl = new ArrayList<String>();
	Iterator<FormalDeclNode> it = myFormals.iterator();
	try {
            while (it.hasNext()) {
		FormalDeclNode tf = it.next();
		String t = tf.getType().getTypeName();
		fl.add(t);
	    }

        } catch (NoSuchElementException ex) {
            dead("unexpected NoSuchElementException in DeclListNode.nameAnalyze");
        }

	return fl;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void nameAnalyze(SymTable st){
	try{
	    // check declaration
	    myDeclList.nameAnalyze(st);
	    // check usage
	    myStmtList.nameAnalyze(st);
	    // remove this scope (no need in future)
	    st.removeScope();
	}catch(EmptySymTableException e ){
	    st.print();
	    dead("Error in Function body, Symbol Table is empty already:");
	}
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void nameAnalyze(SymTable st){
	Iterator<StmtNode> it = myStmts.iterator();
	try {
            while (it.hasNext()) {
		StmtNode ts = it.next();
		ts.nameAnalyze(st);
	    }
        } catch (NoSuchElementException ex) {
            dead("unexpected NoSuchElementException in DeclListNode.nameAnalyze");
        }
	
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }
    
    public void nameAnalyze(SymTable st){
	Iterator<ExpNode> it = myExps.iterator();
	try {
            while (it.hasNext()) {
		ExpNode te = it.next();
		te.nameAnalyze(st);
	    }
        } catch (NoSuchElementException ex) {
            dead("unexpected NoSuchElementException in ExpListNode.nameAnalyze");
        }
    }


    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    // protected Sym declSymbol;
    abstract public void nameAnalyze(SymTable st);
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void nameAnalyze(SymTable st){
	Sym varSym; 
	String t = myType.getTypeName();
	if(t.equals("int")|| t.equals("bool") ){ 	//  it is a normal type
	    varSym = new Sym(t);
	    myId.setSym(varSym);
	    tableCheck(myId, varSym, st);

	}else if(t.equals("void")){
	    myId.errorReport("Non-function declared void");

	}else{	    // it is a struct type
	    // check if the struct type exists
	    IdNode structID = ((StructNode)myType).getStructNodeID();
	    String structName = "struct " + structID.getIDName();
	    Sym structSym = st.lookupGlobal(structName);
	    // st.print();
	    if(structSym == null){
		myId.errorReport("Invalid name of struct type");

	    }else{
		structID.setSym(structSym); // link to struct itself
		varSym = new Sym(structID.getIDName()); // struct aa b; b's type is aa

		// create the symbol and link
		varSym.setStruct(true); // this identifier is a struct
		varSym.setStructMap(structSym.getStructVars()); // share a hashmap with struct 
		myId.setSym(varSym);

		// check if this variable has been multiply declared
		try{
		    // echo("storing..."+myId);
		    st.addDecl(myId.getIDName(), varSym);
		    // echo("after store");
		    // st.print();
		}catch(DuplicateSymException e){
		    myId.errorReport("Multiply declared identifier");

		}catch(EmptySymTableException e){
		    dead("Error scope in varDeclNode, Symbol Table:");
		    st.print();
		}

	    }

	}

    }

    public TypeNode getType(){
	return myType;
    }

    public IdNode getID(){
	return myId;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }

    // 3 kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    private void addFnDeclToSymTable(SymTable st, String name, Sym fnSym){
	try{
	    st.addDecl(name, fnSym);
	}catch(DuplicateSymException e){
	    
	}catch(EmptySymTableException e){
	
	}
    }


    public void nameAnalyze(SymTable st){
	// step 1: check if there is already a same name as the function, same as vardecl
	String lookupName = myId.getIDName();
        Sym sFnNew = new Sym(myType.getTypeName());
	// echo("in fndecl:" + sFnNew.getType());
	List<String> fl = myFormalsList.getFormalListSyms();
	// preparation
	sFnNew.setFunc(true);
	sFnNew.setFormalList(fl);

	Sym sFnOld = st.lookupLocal(lookupName);

	if(sFnOld != null){
	    if(sFnOld.isFunc()){

		// OK contine check formal list
		List<String> oldfl = sFnOld.getFormalListVars();
		if(oldfl.size() == fl.size()){ // suspecious
		    boolean isDup = true;
		    Iterator<String> oldIt = oldfl.iterator();
		    Iterator<String> it = fl.iterator();
		    while(oldIt.hasNext()){
			if(!(oldIt.next().equals(it.next()))){
			    isDup = false;
			    break;
			}
		    }
		    // all argument types are the same, it's a duplication
		    if(isDup){
			myId.errorReport("Multiply declared identifier");

		    }
		}else{ // different number of arguments
		    addFnDeclToSymTable(st, lookupName, sFnNew);
		}

	    }else{
		// fnDecl VS non-fnDecl, illegal, considered as duplication
		myId.errorReport("Multiply declared identifier");

	    }
	}else{
	    addFnDeclToSymTable(st, lookupName, sFnNew);
	}
	// link to that entry
	myId.setSym(sFnNew);

	// check the name use in the formal list
	myFormalsList.nameAnalyze(st);
	myBody.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }

    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    public void nameAnalyze(SymTable st){
	Sym s; 
	
	if(myType.getTypeName().equals("void")){
	    myId.errorReport("Non-function declared void");

	}else{
	    s = new Sym(myType.getTypeName());
	    myId.setSym(s);
	    tableCheck(myId, s, st);

	}

    }

    public TypeNode getType(){
	return myType;
    }


    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }
    /** TODO */
    public void nameAnalyze(SymTable st){
	// check declaration and the primitive define type should be 'struct'
	String structName = "struct " + myId.getIDName();
	Sym s = new Sym("struct");
	s.setStruct(true);
	s.setStructMap(myDeclList.getStructVars(st));	// add struct fields
	myId.setSym(s);
	tableCheck(myId, s, st);

    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
	myId.unparse(p, 0);
	p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");
    }

    // 2 kids
    private IdNode myId;
    private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {

    abstract String getTypeName();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
    
    public String getTypeName(){
	return "int";
    }

}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }

    public String getTypeName(){
	return "bool";
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }

    public String getTypeName(){
	return "void";
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
	myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
	myId.unparse(p, 0);
    }

    public String getTypeName(){
	return myId.getIDName();
    }	

    public IdNode getStructNodeID(){
	return myId;
    }
    // 1 kid
    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalyze(SymTable St);
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    public void nameAnalyze(SymTable st){
	myAssign.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    // 1 kid
    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, -1);
        p.println("++;");
    }

    // 1 kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, -1);
        p.println("--;");
    }

    // 1 kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, -1);
        p.println(";");
    }

    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, -1);
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
	// add if's scope
	st.addScope();
	myDeclList.nameAnalyze(st);
	myStmtList.nameAnalyze(st);
	// quit if's scope, invalidate its scope
	try{
	    st.removeScope();
	}catch(EmptySymTableException e){
	    dead("Error in IfStmtNode, Symbol Table is empty already:");
	}
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, -1);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // e kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
	// if's scope
	st.addScope();
	myThenDeclList.nameAnalyze(st);
	myThenStmtList.nameAnalyze(st);

	try{
	    st.removeScope();
	}catch(EmptySymTableException e){
	    dead("Error in IfElseStmtNode: if, Symbol Table is empty already:");
	}
	// else's scope
	st.addScope();
	myElseDeclList.nameAnalyze(st);
	myElseStmtList.nameAnalyze(st);

	try{
	    st.removeScope();
	}catch(EmptySymTableException e){
	    dead("Error in IfElseStmtNode: else, Symbol Table is empty already:");
	}

    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, -1);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");        
    }

    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    /**TODO: other situation need handle*/
    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);

	st.addScope();
	myDeclList.nameAnalyze(st);
	myStmtList.nameAnalyze(st);

	try{
	    st.removeScope();
	}catch(EmptySymTableException e){
	    dead("Error in WhileStmtNode: else, Symbol Table is empty already:");
	}
    }
	
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, -1);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    /**TODO: other situation need handle*/
    public void nameAnalyze(SymTable st){
	myCall.nameAnalyze(st); 
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    // 1 kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /**TODO: other situation need handle*/
    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, -1);
        }
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {

    public void nameAnalyze(SymTable st){
    }

    public boolean isLoc(){
	return false;
    }
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
	
    }

    public void nameAnalyze(SymTable st){
	mySym = st.lookupLocal(myStrVal);
    	if(mySym == null){
	    mySym =  st.lookupGlobal(myStrVal);
	    if(mySym == null)
		errorReport("Undeclared identifier");
    	}
	
    }

    public void warnReport(String msg){
	ErrMsg.warn(myLineNum, myCharNum, msg);
    }

    public void errorReport(String msg){
	ErrMsg.fatal(myLineNum, myCharNum, msg);
    }

    public void unparse(PrintWriter p, int indent) {
	// indent is useless here, used to indicate the usage
	// -2: for func unparse -1: use 0: declare
	switch(indent){
	case -1:
	    p.print(myStrVal);        
	    if(mySym != null)
		p.print("(" + mySym.getType() +")");
	    break;
	case -2:
	    if(mySym != null)
		p.print(mySym.getType());
	    break;
	default:
	    p.print(myStrVal);
	}

    }

    public String getIDName(){
	return myStrVal;
    }

    public void setIDName(String s){
	myStrVal = s;
    }

    public Sym getSym(){
	return mySym; // called when this id is used
    }

    public void setSym(Sym s){
	mySym = s; // set during declaration
    }

    // public String toString(){
    // 	// for debug
    // 	HashMap<String, Sym> h; 
    // 	if(mySym != null)
    // 	    if((h = mySym.getStructVars() )!= null)
    // 		return myStrVal + ": " + h.toString();
	
    // 	return myStrVal + ": " + "***null***";
    // 	// return myStrVal + ":\n" + mySym.getStructVars().toString();
    // }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    // used for name anaylze
    private Sym mySym;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;	
        myId = id;
    }
    
    public void nameAnalyze(SymTable st){
	// e.g a.b.c;	
	// st.print();

	Sym s = unrollingLoc(st, myLoc, myId);
	myId.setSym(s);
	// echo("");
    }

    private Sym unrollingLoc(SymTable st, ExpNode loc, IdNode rhs){
	// 	// System.out.println("LHS Loc: " + loc.isLoc() + " RHS:" + rhs.getIDName() + " -Loc: " +rhs.isLoc());
    	if(loc.isLoc()){ // need go to next level 
    	    DotAccessExpNode lhs = (DotAccessExpNode)loc;
    	    ExpNode newLoc = lhs.dot_GetMyLoc();
    	    IdNode newID = lhs.dot_GetMyID();

    	    Sym s = unrollingLoc(st, newLoc, newID);
	    newID.setSym(s);
	    String id;
	    if(s != null){
		if(s.isStruct()){
		    id = "struct " + s.getType(); // make to primitive name
		    IdNode LOC = new IdNode(0,0,id);
		    LOC.setSym(s);
		    s = unrollingLoc(st, LOC, rhs);
		    rhs.setSym(s);
		}
		else{
		    newID.errorReport("Dot-access of non-struct type");
		}
	    }else{
		newID.errorReport("Dot-access of non-struct type");

	    }

	    return s;

    	}else{ //loc is also an IDNode
    	    // step 1 check if this id is a struct 
	    // actually it is impossible
	    // if(loc == null){
	    // 	return null;
	    // }

    	    IdNode lhs = (IdNode)loc;

	    // echo("when dot access" + lhs.getIDName());
	    // st.print();
    	    Sym s = st.lookupGlobal(lhs.getIDName());
	    lhs.setSym(s);
	    // echo(lhs.getIDName());
	    // echo("reach to leftmost identifier, start unrolling");
	    // echo("access  " + lhs.getIDName() +"." + rhs.getIDName());

    	    if(s == null){// || (s.getType().equals("struct") == false)){
		lhs.errorReport("Undeclared identifier");

    	    }else if(!s.isStruct()){
    	    	// System.out.println(s);
		lhs.errorReport("Dot-access of non-struct type");

    	    }else{
		// check rhs
		HashMap<String, Sym> h = s.getStructVars();
		// System.out.println("Hashhhhhhh " + h.toString());
		if(!h.containsKey(rhs.getIDName())){
		    rhs.errorReport("Invalid struct field name");

		}
		return h.get(rhs.getIDName());
	    }
	    return null;
    	}
    }

    /**override */
    public boolean isLoc(){
	return true;
    }

    public ExpNode dot_GetMyLoc(){
	return myLoc;
    }

    public IdNode dot_GetMyID(){
	return myId;
    }


    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myLoc.unparse(p, -1);
	// p.print(").");
	p.print(".");
	myId.unparse(p, -1);
    }

    // 2 kids
    private ExpNode myLoc;	
    private IdNode myId;
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    public void nameAnalyze(SymTable st){
	myLhs.nameAnalyze(st);
	myExp.nameAnalyze(st);
    }

    public void unparse(PrintWriter p, int indent) {
	// if (indent != -1)  p.print("(");
	myLhs.unparse(p, -1);
	p.print(" = ");
	myExp.unparse(p, -1);
	// if (indent != -1)  p.print(")");
    }

    // 2 kids
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    public void nameAnalyze(SymTable st){
	// check the use of the function name itself
	// need check global as it is 
	// returnType = checkUseGlobal(myId.getIDName(), st);
	returnType = st.lookupGlobal(myId.getIDName());
	if(returnType == null)
	    myId.errorReport("Undeclared identifier");

	// check the use of all the arguments this function calls
	myExpList.nameAnalyze(st);
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent) {
	myId.unparse(p, 0);
	// add (formals -> return type)
	p.print("(");
	myExpList.unparse(p, -2);
	p.print("->");
	p.print(returnType.getType());
	p.print(")");


	p.print("(");
	if (myExpList != null) {
	    myExpList.unparse(p, -1);
	}
	p.print(")");
    }

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
    private Sym returnType;
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }
    public void nameAnalyze(SymTable st){
	myExp.nameAnalyze(st);
    }

    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }
    public void nameAnalyze(SymTable st){
	myExp1.nameAnalyze(st);
	myExp2.nameAnalyze(st);
    }

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(-");
	p.print("-");
	myExp.unparse(p, -1);
	// p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(!");
	p.print("!");
	myExp.unparse(p, -1);
	// p.print(")");
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" + ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, 0);
	p.print(" - ");
	myExp2.unparse(p, 0);
	// p.print(")");
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" * ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" / ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" && ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" || ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" == ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" != ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" < ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" > ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" <= ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	// p.print("(");
	myExp1.unparse(p, -1);
	p.print(" >= ");
	myExp2.unparse(p, -1);
	// p.print(")");
    }
}
