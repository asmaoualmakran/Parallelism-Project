package be.vub.parallellism.solutions;


import be.vub.parallellism.data.models.Comment;
import be.vub.parallellism.data.readers.RedditCommentLoader;
import com.vader.sentiment.analyzer.SentimentAnalyzer;
import com.vader.sentiment.util.ScoreType;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class ParallelAnalyser {



    public static void main(String[] args) {
        BasicConfigurator.configure();
        int P = 4;
        int T = 1000;
        String brand = "BMW";

        try {
            // Multiple files can be provided. These will be read one after the other.
            String[] data = new String[]{
                    "./code/files/dataset_1.json"
            };



            List<Comment> comments = RedditCommentLoader.readData(data);
            float sentimentSubreddit = SentimentSubreddit(comments, P,T);
            System.out.printf("The sentiment value in this subreddit is: %f \n", sentimentSubreddit);


           float sentimentBrand = SentimentBrand(brand, comments, P, T);
           System.out.printf("The sentiment of the brand " + brand + " is: %f \n", sentimentBrand);


        }
        catch (IOException e) {
            System.out.println(e.toString());
        }

    }

    public  static Float CalculateSentiment(Float[] sentimentArray,ForkJoinPool pool){

        PrefixSumSentiment.TempTreeNode rootNode = pool.invoke(new PrefixSumSentiment.BuildNodeTask(sentimentArray,0, sentimentArray.length));
        Float[] resultArray = new Float[sentimentArray.length];
        float initial = 0;
        pool.invoke(new PrefixSumSentiment.SumTask(rootNode,initial,sentimentArray,resultArray));
        float sentiment = resultArray[resultArray.length-1];
        float length = resultArray.length;
        float mean = sentiment/length;
        return mean;
    }

    public static Float SentimentBrand(String brand,List<Comment> comments, int P, int T){

        ForkJoinPool fjPool = new ForkJoinPool(P);
        List<Comment>reducedData = fjPool.invoke(new DataReduce(brand,comments,T));

        Float[] sentimentArray = fjPool.invoke(new BrandAnalyser(reducedData,T));

        return CalculateSentiment(sentimentArray,fjPool);


    }

    public static Float SentimentSubreddit(List<Comment> comments, int P, int T){

        ForkJoinPool fjPool = new ForkJoinPool(P);

        Float[] sentimentArray = fjPool.invoke(new BrandAnalyser(comments,T));

       return CalculateSentiment(sentimentArray,fjPool);

    }


}
