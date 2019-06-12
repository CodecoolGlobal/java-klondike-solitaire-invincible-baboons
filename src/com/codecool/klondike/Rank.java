package com.codecool.klondike;

public enum Rank {
    ACE(1, "ace"),
    TWO(2, "two"),
    THREE(3, "three"),
    FOUR(4, "four"),
    FIVE(5, "five"),
    SIX(6, "six"),
    SEVEN(7, "seven"),
    EIGHT(8, "eight"),
    NINE(9, "nine"),
    TEN(10, "ten"),
    JACK(11, "jack"),
    QUEEN(12, "queen"),
    KING(13, "king");

    private int rank;
    private String rankName;

    Rank(int rank, String name){
        this.rank = rank;
        this.rankName = name;
    }

    public int getRankNum(){
        return rank;
    }

    public String getName(){
        return rankName;
    }

}
