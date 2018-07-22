package be.vub.parallellism.data.models;

import com.google.gson.annotations.SerializedName;


public class Comment {
    @SerializedName("author")
    public String author;

    @SerializedName("body")
    public String body;

    @SerializedName("score")
    public Integer score = 0;

    @SerializedName("subreddit")
    public String subreddit;

    @SerializedName("id")
    public String id;

    @Override
    public String toString() {
        return "Comment{" +
                "author='" + author + '\'' +
                ", body='" + body + '\'' +
                ", score=" + score +
                ", subreddit='" + subreddit + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}