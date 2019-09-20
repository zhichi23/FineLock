package rwlockrefactoring.analysis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;


import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

import rwlockrefactoring.util.Exclusions;

public class MakeCallGraph {
	
	static String file;
	static File exFile;
	ClassHierarchy cha;
	PointerAnalysis<InstanceKey> pointer;
	
	public MakeCallGraph(String file) {
		// TODO Auto-generated constructor stub
		this.file=file;
		
	}
	public void dosome() {
		try {
			callgraph();
		} catch (ClassHierarchyException | IllegalArgumentException | CallGraphBuilderCancelException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public  CallGraph callgraph()
			throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException {
		//File exFile = new FileProvider().getFile("./dat/ExclusionsFile.txt");
		// 将分析域存到文件
		//AnalysisScope scope=AnalysisScopeReader.makeJavaBinaryAnalysisScope(file+"/bin", null);
		AnalysisScope scope = null;
		ClassLoader javaLoader = MakeCallGraph.class.getClassLoader();
		scope = AnalysisScopeReader.readJavaScope("rwlock//lib//scope.txt", null, javaLoader);
		scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(Exclusions.EXCLUSIONS.getBytes("UTF-8"))));
		
		//add objective project to analyze
		ClassLoaderReference walaLoader = scope.getLoader(Atom.findOrCreateUnicodeAtom("Application"));
	    FileProvider fp = new FileProvider();
	    File bd = fp.getFile(file, javaLoader);
	    scope.addToScope(walaLoader, new BinaryDirectoryTreeModule(bd));
		
		// 构建ClassHierarchy，
		cha = ClassHierarchyFactory.make(scope);

		Iterable<Entrypoint> entrypoints = null;
		entrypoints=Util.makeMainEntrypoints(scope, cha);
		//entrypoints = new AllApplicationEntrypoints(scope, cha);

		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

		CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(Language.JAVA,options, new AnalysisCacheImpl(), cha, scope,
				null, null);
		CallGraph cg = builder.makeCallGraph(options, null);
		pointer=builder.getPointerAnalysis();
		return cg;
	
	}
	public PointerAnalysis<InstanceKey> pointer(){
		return pointer;
	}
	public ClassHierarchy cha() {
		return cha;
	}
}