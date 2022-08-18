package mypluginfault.handlers;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.junit.runner.JUnitCore;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.SimpleType;

import visitors.AssertVisitor;
import visitors.AssignmentVisitor;
import visitors.ForVisitor;
import visitors.IfVisitor;
import visitors.MethodDeclarationVisitor;
import visitors.MethodInvocationStatementVisitor;
import visitors.ReturnVisitor;
import visitors.SimpleNameVisitor;
import visitors.SwitchVisitor;
import visitors.TryStatementVisitor;
import visitors.VariableDeclarationStatementVisitor;
import visitors.WhileVisitor;

public class FaultHandler extends AbstractHandler {
	@SuppressWarnings("unchecked")
	@Override
	//Fault Type 1 = change return value, Fault Type 2 = remove last statement, if possible
	//Classe principal do plug-in de inser��o de faltas
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Scanner s = new Scanner(System.in);
		String projectName = "Exemplo";
		System.out.println("Digite o Nome Do Metodo a ser alterado");
		String methodNameOriginal = s.nextLine();
		System.out.println("Digite a linha de codigo a ser refatorada, se a falta for altera��o de retorno");
		String line = s.nextLine();
		System.out.println("Digite 1 para falta de valor, e 2 para falta de remo��o");
		Integer fault = s.nextInt();
		IProject exampleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IJavaProject jproject = JavaCore.create(exampleProject);

		try {
			IProject project = jproject.getProject();
			

			IFolder folder = project.getFolder("test");
			IPackageFragmentRoot packageFragmenteRoot = jproject.getPackageFragmentRoot(folder);
			IPackageFragment packageFragment = packageFragmenteRoot.createPackageFragment("exemplo", true, null);
			//Classe
			ICompilationUnit unit = packageFragment.getCompilationUnit("Example2.java");

			ASTParser parser = ASTParser.newParser(AST.JLS14);
			parser.setResolveBindings(true);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setBindingsRecovery(true);
			parser.setSource(unit);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

			MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
			astRoot.accept(methodVisitor);

			List<MethodDeclaration> methods = methodVisitor.getMethods();

			AST ast = astRoot.getAST();
			ASTRewrite rewriter = ASTRewrite.create(ast);

			TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
			ListRewrite listMethodRewrite = rewriter.getListRewrite(typeDecl,
					TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			for (int i = methods.size() - 1; i >= 0; i--) {
				MethodDeclaration testMethod = methods.get(i);
				String testName = testMethod.getName().toString();
				if(methodNameOriginal.equals(testName)) {
				List<ASTNode> nodesA = new ArrayList<ASTNode>();
				nodesA.add(testMethod.getBody());
				Iterator<ASTNode> assertIt = nodesA.iterator();

				MethodDeclaration newMethod = astRoot.getAST().newMethodDeclaration();
				newMethod.setBody(astRoot.getAST().newBlock());
				SingleVariableDeclaration p;
				SimpleType e;
				for(int eindex = 0;eindex < testMethod.thrownExceptionTypes().size();eindex++) {
					e = (SimpleType) (testMethod.thrownExceptionTypes().get(eindex));
					newMethod.thrownExceptionTypes().add(astRoot.getAST().newSimpleType(astRoot.getAST().newSimpleName(e.getName().toString())));
				}
				for(int pindex = 0; pindex < testMethod.parameters().size();pindex++) {
					p = (SingleVariableDeclaration) testMethod.parameters().get(pindex);
					SingleVariableDeclaration np = astRoot.getAST().newSingleVariableDeclaration();
					np.setInitializer(p.getInitializer());
					if(p.getType().isPrimitiveType())
						np.setType(astRoot.getAST().newPrimitiveType(((PrimitiveType) p.getType()).getPrimitiveTypeCode()));
					else
					np.setType(astRoot.getAST().newSimpleType(astRoot.getAST().newSimpleName(p.getType().toString())));
					np.setName(astRoot.getAST().newSimpleName(p.getName().toString()));
					newMethod.parameters().add(np);
				}
				if(testMethod.getReturnType2() != null) {
				if(testMethod.getReturnType2().isPrimitiveType()) {
					if(testMethod.getReturnType2().toString().equals("int")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.INT));		
					}else if(testMethod.getReturnType2().toString().equals("char")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.CHAR));		
					}else if(testMethod.getReturnType2().toString().equals("byte")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.BYTE));		
					}else if(testMethod.getReturnType2().toString().equals("float")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT));		
					}else if(testMethod.getReturnType2().toString().equals("long")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.LONG));		
					}else if(testMethod.getReturnType2().toString().equals("boolean")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.BOOLEAN));	
					}else if(testMethod.getReturnType2().toString().equals("double")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.DOUBLE));		
					}else if(testMethod.getReturnType2().toString().equals("short")) {
						newMethod.setReturnType2(astRoot.getAST().newPrimitiveType(PrimitiveType.SHORT));	
					}
				}else {
					Type type = testMethod.getReturnType2();
					newMethod.setReturnType2(astRoot.getAST().newSimpleType(astRoot.getAST().newSimpleName(testMethod.getReturnType2().toString())));
					
				}
				}
				Modifier m;
				MarkerAnnotation n;
				for(int mindex = 0; mindex < testMethod.modifiers().size();mindex++) {
					if(testMethod.modifiers().get(mindex)instanceof MarkerAnnotation) {
						n = (MarkerAnnotation) testMethod.modifiers().get(mindex);
						System.out.println(n.getTypeName());
						MarkerAnnotation nnew = astRoot.getAST().newMarkerAnnotation();
						nnew.setTypeName(n.getAST().newName(n.getTypeName().toString()));
						newMethod.modifiers().add(nnew);

					}
					else if(testMethod.modifiers().get(mindex)instanceof Modifier) {
					m = (Modifier) testMethod.modifiers().get(mindex);
					newMethod.modifiers().add(astRoot.getAST().newModifier(m.getKeyword()));
					}
				}
				newMethod.setConstructor(false);
				newMethod.setName(astRoot.getAST().newSimpleName(testName));
				while (assertIt.hasNext()) {
					Set<ASTNode> newTestNodes = new HashSet<ASTNode>();

					ASTNode assertStatement = assertIt.next();

					SimpleNameVisitor simpleNameVisitor = new SimpleNameVisitor();
					assertStatement.accept(simpleNameVisitor);

					List<SimpleName> simpleNames = simpleNameVisitor.getNames();
					int index = 0;

					while (index < simpleNames.size()) {
						SimpleName simpleName = simpleNames.get(index);
						
						String variableName = simpleName.toString();

						VariableDeclarationStatementVisitor declarations = new VariableDeclarationStatementVisitor(
								variableName);
						testMethod.accept(declarations);
						AssignmentVisitor assigmentVisitor = new AssignmentVisitor(variableName,fault==3,line);
						testMethod.accept(assigmentVisitor);
						MethodInvocationStatementVisitor methodInvocationVisitor = new MethodInvocationStatementVisitor(variableName);
						testMethod.accept(methodInvocationVisitor);
						IfVisitor ifVisitor = new IfVisitor(variableName);
						testMethod.accept(ifVisitor);
						WhileVisitor whileVisitor = new WhileVisitor(variableName);
						testMethod.accept(whileVisitor);
						ForVisitor forVisitor = new ForVisitor(variableName);
						testMethod.accept(forVisitor);
						ReturnVisitor returns = new ReturnVisitor(variableName,fault==1, line + "\n", testMethod.getReturnType2());
						testMethod.accept(returns);
						SwitchVisitor switchs = new SwitchVisitor();
						testMethod.accept(switchs);
						List<ASTNode> nodes = new ArrayList();
						nodes.addAll(declarations.getDeclarations());
						nodes.addAll(assigmentVisitor.getAssignments());
						nodes.addAll(methodInvocationVisitor.getMethodInvocations());
						nodes.addAll(ifVisitor.getNames());
						nodes.addAll(whileVisitor.getNames());
						nodes.addAll(forVisitor.getNames());
						nodes.addAll(returns.getNames());
						nodes.addAll(switchs.getSwitchStatements());
						for (ASTNode node : nodes) {
							newTestNodes.add(node);

							SimpleNameVisitor simpleNameVisitor2 = new SimpleNameVisitor();
							node.accept(simpleNameVisitor2);

							for (SimpleName name2 : simpleNameVisitor2.getNames()) {
								if (!simpleNames.contains(name2)) {
									simpleNames.add(name2);
								}
							}
						}
						index++;
					}
					List<ASTNode> nodesOrdered = newTestNodes.stream().collect(Collectors.toList());
					Collections.sort(nodesOrdered, (o1, o2) -> o1.getStartPosition() - o2.getStartPosition());
					if (nodesOrdered.size() > 0) {
						

						Block block = newMethod.getBody();
						ListRewrite listBlockRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
					
						for (int nindex = 0;nindex < nodesOrdered.size();nindex++) {
							if(fault != 2) {
								listBlockRewrite.insertLast(nodesOrdered.get(nindex),null);
							}
							else if(nindex < nodesOrdered.size() - 1) {
							listBlockRewrite.insertLast(nodesOrdered.get(nindex), null);
							}
							else if((nodesOrdered.get(nindex) instanceof ReturnStatement)) {
								if(nodesOrdered.get(nindex-1) instanceof VariableDeclarationStatement) {
									String fragment = ((VariableDeclarationStatement) nodesOrdered.get(nindex-1)).fragments().get(0).toString();
									int fragi = fragment.indexOf("=");
									String fragsub = fragment.substring(0, fragi);
									String expressionStr = ((ReturnStatement) nodesOrdered.get(nindex)).getExpression().toString();
									if(expressionStr.equals(fragsub)) {
										listBlockRewrite.insertLast(nodesOrdered.get(nindex), null);
										break;
									}
								}
								listBlockRewrite.remove(nodesOrdered.get(nindex-1), null);
								listBlockRewrite.insertLast(nodesOrdered.get(nindex), null);
							}
						}
												
					}
				}
				
				listMethodRewrite.insertAfter(newMethod, testMethod, null);

				// ---------------------------------

				listMethodRewrite.remove(testMethod, null);

			}
				
			}
			TextEdit edits = rewriter.rewriteAST();

			// apply the text edits to the compilation unit
			Document document = new Document(unit.getSource());

			edits.apply(document);

			// this is the code for adding statements
			unit.getBuffer().setContents(document.get());

	        unit.applyTextEdit(edits, new NullProgressMonitor());
	        
			parser.setSource(unit);

			astRoot = (CompilationUnit) parser.createAST(null);
			
			typeDecl = (TypeDeclaration) astRoot.types().get(0);
			for (int index = 0; index < typeDecl.getMethods().length; index ++) {
				MethodDeclaration methodReodered = typeDecl.getMethods()[index];
				if(methodReodered.getName().toString().equals(methodNameOriginal)) {
					String methodName = methodReodered.getName().toString().replace("reordered", "");

					Block block = methodReodered.getBody();

					int initial = 0;

					int length = 0;

					int methodExtractedIndex = 0;

					List statements = block.statements();
					
					parser.setSource(unit);

					astRoot = (CompilationUnit) parser.createAST(null);
					
					typeDecl = (TypeDeclaration) astRoot.types().get(0);
				}
			}
			
			ast = astRoot.getAST();
			
			rewriter = ASTRewrite.create(ast);
			
			listMethodRewrite = rewriter.getListRewrite(typeDecl,
					TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					
			
			edits = rewriter.rewriteAST();

			// apply the text edits to the compilation unit
			document = new Document(unit.getSource());

			edits.apply(document);

			// this is the code for adding statements
			unit.getBuffer().setContents(document.get());

	        unit.applyTextEdit(edits, new NullProgressMonitor());
			
		} catch (CoreException e) {
			projectName = "CoreException";
			e.printStackTrace();
		} catch (Exception e) {
			projectName = "Exception";
			e.printStackTrace();
		}
		MessageDialog.openInformation(window.getShell(), "Plugin","Codigo executado");

		return null;
}
	
	protected void checkPreconditions(final ICompilationUnit unit, final Refactoring refactoring) throws Exception {
		IProgressMonitor pm = new NullProgressMonitor();
		checkPreconditions(refactoring, pm).isOK();
	}

	protected RefactoringStatus checkPreconditions(Refactoring refactoring, IProgressMonitor pm) throws CoreException {
		CheckConditionsOperation op = new CheckConditionsOperation(refactoring, getCheckingStyle());
		op.run(pm);
		return op.getStatus();
	}

	protected void performRefactor(Refactoring refactoring) throws CoreException {
		PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring, getCheckingStyle());
		JavaCore.run(op, new NullProgressMonitor());
	}

	protected int getCheckingStyle() {
		return CheckConditionsOperation.ALL_CONDITIONS;
	}
	//Desnecess�rio.
	protected URLClassLoader getProjectClassLoader(IJavaProject javaProject) throws CoreException, MalformedURLException {
		String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		List<URL> urlList = new ArrayList<URL>();
		for (int i = 0; i < classPathEntries.length; i++) {
		 String entry = classPathEntries[i];
		 IPath path = new Path(entry);
		 URL url = url = path.toFile().toURI().toURL();
		 urlList.add(url);
		}
		
		ClassLoader parentClassLoader = javaProject.getClass().getClassLoader();
		URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
		URLClassLoader classLoader = new URLClassLoader(urls, parentClassLoader);
		
		return classLoader;
	}
}
