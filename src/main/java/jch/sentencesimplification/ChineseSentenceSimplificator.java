package jch.sentencesimplification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.semgrex.SemgrexMatcher;
import edu.stanford.nlp.semgraph.semgrex.SemgrexPattern;
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalRelations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

public class ChineseSentenceSimplificator {

	public static void main(String [] args)
	{
//		Sample input:
//		String text = "你知道美国总统是谁吗";//O
//      String text = "他說你喜歡游泳";//O
//      String text = "你和他知道他妈妈是谁吗";//O
//      String text = "你知道苹果最新发表的电脑吗";//O
		BufferedReader buf = new BufferedReader(new InputStreamReader(System.in)); 
		ChineseSentenceSimplificator css  = new ChineseSentenceSimplificator();
		System.out.print("請輸入您的問句: ");
		String text = "";
		try {
			text = buf.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		List<String> dissentences = css.ProcessDepency(text);
		System.out.println("子句: "+dissentences);
	}
	
	private Set<IndexedWord> rootsubgraph = null;
	private List<String> ccompsentences = new ArrayList<String>();
	private List<String> nsubjsentences = new ArrayList<String>();
	private List<String> dobjsentences = new ArrayList<String>();
	private List<String> depsentences = new ArrayList<String>();

	public List<String> ProcessDepency(String text)
	{
//		SentenceSimplificator sentencesimplification = new SentenceSimplificator();
        StanfordCoreNLP pipeline = new StanfordCoreNLP("StanfordCoreNLP-chinese.properties");
        
        
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        List<String> dissentences = new ArrayList<String>();
        
        for (CoreMap sentence: sentences) 
        {
//        	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
        	SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
        	System.out.println("輸入文字: "+text);
        	System.out.println("SemanticGraph:");
        	System.out.println(dependencies);
        	IndexedWord root = dependencies.getFirstRoot();
        	
        	rootsubgraph = dependencies.getSubgraphVertices(root);       	
//        	
        	int layersnum=0;
        	
        	this.ccompAnalyze(dependencies, root, layersnum);
        	this.nsubjAnalyze(dependencies, root, layersnum);
        	this.dobjAnalyze(dependencies, root, layersnum);
        	this.depAnalyze(dependencies, root, layersnum);
        	
//        	System.out.println("rootsubgraph "+rootsubgraph);
    		Iterator<IndexedWord> iterrootsubgraph = rootsubgraph.iterator();
    		String rootsentence = "";
    		while(iterrootsubgraph.hasNext())
        	{
    			rootsentence = rootsentence+iterrootsubgraph.next().word();
        	}
    		
    		rootsentence = "0."+rootsentence;
//    		System.out.println("rootsentence "+rootsentence);
    		
    		dissentences.add(rootsentence);
    		dissentences.addAll(nsubjsentences);
    		dissentences.addAll(ccompsentences);
    		dissentences.addAll(dobjsentences);
    		dissentences.addAll(depsentences);	
        }
        return dissentences;
	}
	
	public Set<IndexedWord> nsubjAnalyze(SemanticGraph dependencies,IndexedWord node,int layersnum)
	{
		SemgrexPattern pattern = SemgrexPattern.compile("{word:"+node.word()+";tag:"+node.tag()+"}=A >nsubj {}=B");
    	SemgrexMatcher matcher = pattern.matcher(dependencies);
    	
    	Set<IndexedWord> nsubjsubgraph = null;
    	while(matcher.find())
    	{
    		IndexedWord nsubjnode = matcher.getNode("B");
//    		System.out.println(matcher.getNode("A") + " >nsubj " +nsubjnode);
            
//            System.out.println("nsubjsubgraph "+nsubjsubgraph);

            SemgrexPattern pattern1 = SemgrexPattern.compile("{word:"+nsubjnode.word()+";tag:"+nsubjnode.tag()+"}=A >relcl {}=B");
            SemgrexPattern pattern2 = SemgrexPattern.compile("{word:"+nsubjnode.word()+";tag:"+nsubjnode.tag()+"}=A >acl {}=B");
            SemgrexMatcher matcher1 = pattern1.matcher(dependencies);
            SemgrexMatcher matcher2 = pattern2.matcher(dependencies);
    		
    		while(matcher1.find() ||matcher2.find())
    		{
    			layersnum++;

    			nsubjsubgraph = dependencies.getSubgraphVertices(nsubjnode);
//    			System.out.println("nsubjsubgraph "+nsubjsubgraph);
    			
    			rootsubgraph.removeAll(nsubjsubgraph);
    			
    			Set<IndexedWord> nsubjccompsub = this.ccompAnalyze(dependencies, nsubjnode, layersnum);
	    		Set<IndexedWord> nsubjnsubjsub = this.nsubjAnalyze(dependencies, nsubjnode, layersnum);
	    		Set<IndexedWord> nsubjdobjsub = this.dobjAnalyze(dependencies, nsubjnode, layersnum);
	    		Set<IndexedWord> nsubjdepsub = this.depAnalyze(dependencies, nsubjnode, layersnum);
	    		
	    		if(nsubjccompsub!=null)
	    		{
	    			nsubjsubgraph.removeAll(nsubjccompsub);
	    		}
	    		if(nsubjnsubjsub!=null)
	    		{
	    			nsubjsubgraph.removeAll(nsubjnsubjsub);
	    		}
	    		if(nsubjdobjsub!=null)
	    		{
	    			nsubjsubgraph.removeAll(nsubjdobjsub);
	    		}
	    		if(nsubjdepsub!=null)
	    		{
	    			nsubjsubgraph.removeAll(nsubjdepsub);
	    		}
		    		
	    			Iterator<IndexedWord> iternsubjsubgraph = nsubjsubgraph.iterator();
	        		String nsubjsentence = layersnum+". ";
	        		while(iternsubjsubgraph.hasNext())
	            	{
	        			nsubjsentence = nsubjsentence+iternsubjsubgraph.next().word();
	            	}
	        		nsubjsentences.add(nsubjsentence);
	    	}
	    }
	    return nsubjsubgraph;
	}

	public Set<IndexedWord> dobjAnalyze(SemanticGraph dependencies,IndexedWord node,int layersnum)
	{
		SemgrexPattern pattern = SemgrexPattern.compile("{word:"+node.word()+";tag:"+node.tag()+"}=A >dobj {}=B");
    	SemgrexMatcher matcher = pattern.matcher(dependencies);
    	
    	Set<IndexedWord> dobjsubgraph = null;
    	while(matcher.find())
    	{
    		IndexedWord dobjnode = matcher.getNode("B");

//    		System.out.println(matcher.getNode("A") + " >dobj " +dobjnode);
            
//            System.out.println("dobjsubgraph "+dobjsubgraph);

            SemgrexPattern pattern1 = SemgrexPattern.compile("{word:"+dobjnode.word()+";tag:"+dobjnode.tag()+"}=A >relcl {}=B");
        	SemgrexMatcher matcher1 = pattern1.matcher(dependencies);
        
        	SemgrexPattern pattern2 = SemgrexPattern.compile("{word:"+dobjnode.word()+";tag:"+dobjnode.tag()+"}=A >acl {}=B");
        	SemgrexMatcher matcher2 = pattern2.matcher(dependencies);
        
        	
//    		Set<IndexedWord> relclnodes = dependencies.getChildrenWithReln(dobjnode,EnglishGrammaticalRelations.RELATIVE_CLAUSE_MODIFIER);
    		
    		while(matcher1.find() || matcher2.find())
    		{
    			layersnum++;
    			dobjsubgraph = dependencies.getSubgraphVertices(dobjnode);
//    			System.out.println("dobjsubgraph "+dobjsubgraph);
    			
    			rootsubgraph.removeAll(dobjsubgraph);
    			
    			Set<IndexedWord> dobjccompsub = this.ccompAnalyze(dependencies, dobjnode, layersnum);
	    		Set<IndexedWord> dobjnsubjsub = this.nsubjAnalyze(dependencies, dobjnode, layersnum);
	    		Set<IndexedWord> dobjdobjsub = this.dobjAnalyze(dependencies, dobjnode, layersnum);
	    		Set<IndexedWord> dobjdepsub = this.depAnalyze(dependencies, dobjnode, layersnum);
	    		
	    		if(dobjccompsub!=null)
	    		{
	    			dobjsubgraph.removeAll(dobjccompsub);
	    		}
	    		if(dobjnsubjsub!=null)
	    		{
	    			dobjsubgraph.removeAll(dobjnsubjsub);
	    		}
	    		if(dobjdobjsub!=null)
	    		{
	    			dobjsubgraph.removeAll(dobjdobjsub);
	    		}
	    		
	    		if(dobjdepsub!=null)
	    		{
	    			dobjsubgraph.removeAll(dobjdepsub);
	    		}
	    		
	    		Iterator<IndexedWord> iterdobjsubgraph = dobjsubgraph.iterator();
        		String dobjsentence = layersnum+". ";
        		while(iterdobjsubgraph.hasNext())
            	{
        			dobjsentence = dobjsentence+iterdobjsubgraph.next().word();
            	}
//        		System.out.println("dobjsentence "+dobjsentence);
        		dobjsentences.add(dobjsentence);
    		}
		}
    	return dobjsubgraph; 		
	}
	
	public Set<IndexedWord> ccompAnalyze(SemanticGraph dependencies,IndexedWord node,int layersnum)
	{
		SemgrexPattern pattern = SemgrexPattern.compile("{word:"+node.word()+";tag:"+node.tag()+"}=A >ccomp {}=B");
    	SemgrexMatcher matcher = pattern.matcher(dependencies);
    	
    	Set<IndexedWord> ccompsubgraph = null;
    	while(matcher.find())
    	{
    		layersnum++;

    		IndexedWord ccompnode = matcher.getNode("B");

    		ccompsubgraph = dependencies.getSubgraphVertices(ccompnode);
//    		System.out.println(matcher.getNode("A") + " >ccomp " +ccompnode);
            
//            System.out.println("ccompsubgraph "+ccompsubgraph);
    		rootsubgraph.removeAll(ccompsubgraph);
    		
    		Set<IndexedWord> ccompccompsub = this.ccompAnalyze(dependencies, ccompnode, layersnum);
    		Set<IndexedWord> ccompnsubjsub = this.nsubjAnalyze(dependencies, ccompnode, layersnum);
    		Set<IndexedWord> ccompdobjsub = this.dobjAnalyze(dependencies, ccompnode, layersnum);
    		Set<IndexedWord> ccompdepsub = this.depAnalyze(dependencies, ccompnode, layersnum);
    		
    		if(ccompccompsub!=null)
    		{
    			ccompsubgraph.removeAll(ccompccompsub);
    		}
    		if(ccompnsubjsub!=null)
    		{
    			ccompsubgraph.removeAll(ccompnsubjsub);
    		}
    		if(ccompdobjsub!=null)
    		{
    			ccompsubgraph.removeAll(ccompdobjsub);
    		}
    		if(ccompdepsub!=null)
    		{
    			ccompsubgraph.removeAll(ccompdepsub);
    		}
    		
    		Iterator<IndexedWord> iterccompsubgraph = ccompsubgraph.iterator();
    		String ccompsentence = layersnum+". ";
    		while(iterccompsubgraph.hasNext())
        	{
    			ccompsentence = ccompsentence+iterccompsubgraph.next().word();
        	}
    		
//    		System.out.println("ccompsentence "+ccompsentence);
    		ccompsentences.add(ccompsentence);
    	}
    	return ccompsubgraph;
	}
	
	public Set<IndexedWord> depAnalyze(SemanticGraph dependencies,IndexedWord node,int layersnum)
	{
		SemgrexPattern pattern = SemgrexPattern.compile("{word:"+node.word()+";tag:"+node.tag()+"}=A >dep {}=B");
    	SemgrexMatcher matcher = pattern.matcher(dependencies);

    	Set<IndexedWord> depsubgraph = null;
    	while (matcher.find()) //must have!!
    	{
    		layersnum++;
    		IndexedWord depnode = matcher.getNode("B");
    		
    		depsubgraph = dependencies.getSubgraphVertices(depnode);
    		
//            System.out.println(matcher.getNode("A") + " >dep " +depnode);
            
//            System.out.println("depsubgraph "+depsubgraph);
			
            rootsubgraph.removeAll(depsubgraph);
            
			Set<IndexedWord> depccompsub = this.ccompAnalyze(dependencies, depnode, layersnum);
    		Set<IndexedWord> depnsubjsub = this.nsubjAnalyze(dependencies, depnode, layersnum);
    		Set<IndexedWord> depdobjsub = this.dobjAnalyze(dependencies, depnode, layersnum);
    		Set<IndexedWord> depdepsub = this.depAnalyze(dependencies, depnode, layersnum);
    		
    		if(depccompsub!=null)
    		{
//    			System.out.println("depccompsub:"+depccompsub);
    			depsubgraph.removeAll(depccompsub);
    		}
    		if(depnsubjsub!=null)
    		{
    			depsubgraph.removeAll(depnsubjsub);
    		}
    		if(depdobjsub!=null)
    		{
    			depsubgraph.removeAll(depdobjsub);
    		}
    		if(depdepsub!=null)
    		{
    			depsubgraph.removeAll(depdepsub);
    		}
    		
    		Iterator<IndexedWord> iterdepsubgraph = depsubgraph.iterator();
    		String depsentence = layersnum+". ";
    		while(iterdepsubgraph.hasNext())
        	{
    			depsentence = depsentence+iterdepsubgraph.next().word();
        	}
//    		System.out.println("depsentence "+depsentence);
    		depsentences.add(depsentence);
    		
//        	String dep = matcher.getMatch().word();
//        	String dep = matcher.getNode("dep").word();？
    	}
    	return depsubgraph;
	}
}
