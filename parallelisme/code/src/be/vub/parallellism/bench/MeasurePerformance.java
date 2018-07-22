
package be.vub.parallellism.bench;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.vub.parallellism.bench.MeasureRuntimes.Strategy;
import com.sun.xml.internal.bind.v2.TODO;

public class MeasurePerformance {

	static Map<Strategy,List<Long>> runtimes;

	
	/**
	 * main method, to be used for computing performance measures.
	 * 
	 * command-line args:
	 * args[0]: preset for which to compute performance measures.
	 * args[1]: # warm-up iterations. These will be ignored (not even be loaded).
	 */

	public static void main(String[] args) {
		//process command line arguments
		if(args.length < 2){
			System.out.println("Please indicate the 'preset number' and 'warmup duration' as command line arguments.");
			return;
		}
		final int preset = Integer.parseInt(args[0]);
		final int warmup_duration = Integer.parseInt(args[1]);
		
		System.out.println("Performance measures for preset "+preset+" (considering the "+warmup_duration+" first measurements as warmup)");
		System.out.println();

		//load data (showing # repetitions for each)
		Map<Strategy,List<Long>> runtimes = load_runtimes(preset,warmup_duration);
		
		//computes and displays performance measures for this benchmark
		compute_performance_measures(runtimes);
	}

	/**
	 * Computes and displays various measures of performance for 4 different parallelization strategies
	 * - fine: static partitioning into 1 task per region
	 * - coarse: static partitioning into P tasks
	 * - region: FJ on region-level
	 * - person: FJ on person-level
	 * 
	 * @param runtimes: The run time measurements for each strategy.
	 */



	static void compute_performance_measures(Map<Strategy,List<Long>> runtimes){
		RuntimeEstimate Tseq = new RuntimeEstimate(runtimes.get(Strategy.PRE_FILTERED4_NOTHRESHOLD));
		System.out.println("Tseq: "+Tseq);
		System.out.println();
		String[] pstrategy_names = new String[]{"sequential","non-filtered","filtered"};
		for(int j = 0; j < pstrategy_names.length;j++){
			System.out.println("<"+pstrategy_names[j]+">");
			RuntimeEstimate T1 = new RuntimeEstimate(runtimes.get(Strategy.values()[2*j+1]));
			RuntimeEstimate T4 = new RuntimeEstimate(runtimes.get(Strategy.values()[2*j+2]));
			//TODO: Extend to also compute:
			double overhead = Double.NaN; 	//- overhead
			double c_speedup = Double.NaN; 	//- computational speedup
			double efficiency = Double.NaN;	//- efficiency
			double a_speedup = Double.NaN;	//- application speedup
			//(Hint: this only requires using Tseq, T1 and T4)
			//Feel free to use either mean/median runtime
			
			System.out.println("T1: "+T1);
			System.out.println("T4: "+T4);
			System.out.println("overhead: "+overhead);
			System.out.println("computational speedup: "+c_speedup);
			System.out.println("efficiency: "+efficiency);
			System.out.println("application speedup: "+a_speedup);
			System.out.println();
		}
	}

	
	/**
	 * This code loads all run time measurements for a give preset, 
	 * ignoring the warm-up first measurements for each strategy.
	 * 
	 * @param i: the index of the preset to load measurements for (runtimes_i.csv must be present!)
	 * @param warmup_duration: # first measurements to ignore as warm-up.
	 * @return #repetitions-warmup_duration run time measurements for each strategy.
	 */

	private static Map<Strategy,List<Long>> load_runtimes(int i, int warmup_duration){
		File file = new File("runtimes_"+i+".csv");
		System.out.print("loading "+file+": ");
		Map<Strategy,List<Long>> runtimes = new HashMap<Strategy,List<Long>>();
		for(Strategy s : Strategy.values()){
			runtimes.put(s, new ArrayList<Long>());
		}
		if(file.exists()){
			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			       String[] tokens = line.split(",");
			       Strategy s = Strategy.valueOf(tokens[0]);
			       for(int j = warmup_duration+1; j < tokens.length; j++){
			    	   runtimes.get(s).add(Long.valueOf(tokens[j]));
			       }
			    }
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("v ("+runtimes.get(Strategy.PRE_FILTERED4_NOTHRESHOLD).size()+" repetitions)");
		}else{
			System.out.println("x");
			System.out.println("ERROR: "+file+" doesn't exist. Run MeasureRuntimes first!");
			System.exit(-1);
		}
		System.out.println();
		return runtimes;		
	}
	
	static class RuntimeEstimate{
		double mean;
		double sd;
		double n;
		double min;
		double median;
		double max;
		
		RuntimeEstimate(List<Long> runtimes){
			mean = compute_avg(runtimes);
			sd = compute_sd(runtimes);
			n = runtimes.size();
			Collections.sort(runtimes);
			min = runtimes.get(0);
			median = runtimes.get(runtimes.size()/2);
			max = runtimes.get(runtimes.size()-1);
		}
		
		double getStandardError(){
			return sd/Math.sqrt(n);
		}
		
		public String toString(){
			return pp(mean)+" +- "+pp(1.96*getStandardError())+" [min:"+pp(min)+", median:"+pp(median)+", max:"+pp(max)+"] ms";
		}
		
		private int pp(double rt){
			return (int) Math.round(rt/1000000);
		}
		
		static private double compute_avg(List<Long> runtimes){
			double sum = 0;
			for(Long rt : runtimes){
				sum += rt;
			}
			return sum/runtimes.size();
		}
		
		static private double compute_sd(List<Long> runtimes){
			double mean = compute_avg(runtimes);
			double sum = 0;
			for(Long rt : runtimes){
				sum += (rt-mean)*(rt-mean);
			}
			return Math.sqrt(sum/runtimes.size());
		}
		
	}

}
