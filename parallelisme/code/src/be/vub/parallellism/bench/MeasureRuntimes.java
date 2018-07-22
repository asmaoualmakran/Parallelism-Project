package be.vub.parallellism.bench;
import be.vub.parallellism.data.models.Comment;
import be.vub.parallellism.data.readers.RedditCommentLoader;
import be.vub.parallellism.solutions.*;
import org.apache.log4j.BasicConfigurator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;



public class MeasureRuntimes {
	//results are stored to keep the compiler from optimising the code obtaining them away
	public static List<Float> use_results = new ArrayList<Float>(10000);
	static String brand = "BMW";
	//Identifiers for different experimental setups
	public enum Strategy{
	//	SEQ, 	//Sequential implementation


		PRE_FILTERED4_NOTHRESHOLD


	}

	static final String[] datafiles = new String[]{
			//"./code/files/dataset_1.json",
			"./code/files/dataset_2.json",

	};

	static Function<Integer, List<Comment>> getDataSet(){
		ArrayList<List<Comment>> dataSets = new ArrayList<List<Comment>>();

		for(int i = 0; i<datafiles.length; i++){
			try {
				List<Comment> comments = RedditCommentLoader.readData(datafiles[i]);
				dataSets.add(comments);
			}
			catch (IOException e) {
				System.out.println(e.toString());
			}

		}
		return (Integer index)-> dataSets.get(index);
	}

	//Actual instantiations of the different strategies compared
	static final List<Function<List<Comment>,Float>> strategies =
			Arrays.asList(

					preFiltered(brand,4,2)

			);



	/*
	 * main method, to be used to perform run time measurements
	 *
	 * command-line args:
	 * args[0]: # repetitions to perform
	 * args[1-5] (optional): indicate for which presets to measure run times (default: 1 2 3 4 5).
	 */
	public static void main(String[] args) {

//		BasicConfigurator.configure();

		if(args.length < 1){
			System.out.println("Please provide '# repetitions to perform' as commandline argument.");
			return;
		}

		Function<Integer, List<Comment>> datagetter =  getDataSet();

		int n_repetitions = Integer.parseInt(args[0]);
		System.out.println("Repeating each measurement "+n_repetitions+" times.");

		List<Integer> preset_indices;
		if(args.length == 1){
			preset_indices = Arrays.asList(0,1);
		}else{
			preset_indices= new ArrayList<Integer>(args.length-1);
			for(int i = 0; i < args.length; i++){
				preset_indices.add(Integer.parseInt(args[i]));
			}
		}

		for(int i : preset_indices){
			File res_file = new File("runtimes_"+i+".csv");
			if(res_file.exists()){
				//avoids accidentally overwriting previous results
				System.out.println(res_file+" already exists, skipping preset "+i);
				continue;
			}
			List<Comment> data = datagetter.apply(i);
			System.out.println("Measuring runtimes for dataset "+i);
			for(int j = 0; j < strategies.size(); j++){
				System.out.print(Strategy.values()[j]+": ");
				System.out.flush();
				//measure runtimes
				List<Long> rts = benchmark(data,strategies.get(j),n_repetitions);
				//write to file
				write2file(res_file,Strategy.values()[j],rts);
				System.out.println("v");
			}
		}
	}

	//writes runtimes for a given strategy to file
	static private void write2file(File f, Strategy s, List<Long> runtimes){
		PrintWriter csv_writer;
		try {
			csv_writer = new PrintWriter(new FileOutputStream(f,true));
			String line = ""+s;
			for(Long rt : runtimes){
				line += ","+rt;
			}
			csv_writer.println(line);
			csv_writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Measures the runtime(s) (in nanoseconds) for a given strategy on a given data preset for a given number of repetitions.
	 *
	 * @param preset : The data set to be filtered
	 * @param strategy : The filtering strategy, i.e. a function mapping data to count
	 * @param nrep : The number of times the experiment should be repeated consecutively
	 * @return A list of nrep runtimes (in nanoseconds), where the i'th element is the runtime of the i'th repetition
	 */
	static public List<Long> benchmark(List<Comment> preset, Function<List<Comment>, Float> strategy, int nrep){
		List<Long> runtimes = new ArrayList<Long>(nrep);
		for(int i = 0; i < nrep; i++){
			System.gc(); //a heuristic to avoid Garbage Collection (GC) to take place in timed portion of the code
			long before = System.nanoTime(); //time is measured in ns
			use_results.add(strategy.apply(preset)); //store results to avoid code getting optimised away
			runtimes.add(System.nanoTime()-before);
		}
		return runtimes;
	}

	/**
	 * @return The sequential strategy
	 */
	static Function<List<Comment>,Float> sequential() {
		System.out.printf("seq called\n");
		return (List<Comment> data) -> {
			System.out.printf("seq ended\n");
			return SequentialAnalyser.NonFilteredRun(data);
		};
	}

	/**
	 * @return
	 */
	static Function<List<Comment>, Float> sequentialFiltered(String brand){
		System.out.printf("seq_filtered called\n");
		return (List<Comment> data)->{
			System.out.printf("seq_filtered ended\n");
			return SequentialAnalyser.FilteredRun(data, brand);
		};
	}

	static Function<List<Comment>,Float> preFiltered(String brand, int p, int T){
		System.out.printf("pre_filtered called: %d %d \n", p, T );
		ForkJoinPool fjPool = new ForkJoinPool(p);
		return (List<Comment> data)-> {
			List<Comment> reducedData = fjPool.invoke(new DataReduce(brand,data,T));
			Float[] sentimentArray = fjPool.invoke(new BrandAnalyser(reducedData,T)); // hier pm vervangen voor fase 1 moet het de volledige dataset zijn geen gefilterde
			System.out.printf("pre_filtered ended: %d %d \n", p, T );
			return ParallelAnalyser.CalculateSentiment(sentimentArray,fjPool);
		};
	}


	static Function<List<Comment>,Float> nonFiltered(int p, int T){
		System.out.printf("non_filtered called: %d %d \n", p, T );
		ForkJoinPool fjPool = new ForkJoinPool(p);
		return (List<Comment> data)->{
			Float[] sentimentArray = fjPool.invoke(new BrandAnalyser(data,T));
			System.out.printf("non_filtered ended: %d %d \n", p, T );
			return ParallelAnalyser.CalculateSentiment(sentimentArray,fjPool);
		};
	}


}