package com.codecool.klondike;

import com.sun.javafx.scene.control.LabeledText;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.util.*;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    //Initializing restart button
    Button restartButton = new Button("restart");

    private static final double STOCK_GAP = 1;
    private static final double FOUNDATION_GAP = 0;
    private static final double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };


    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        ObservableList<Card> draggedPile = card.getContainingPile().getCards();

        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        draggedCards.add(card);
        try {
            for (int i = draggedPile.indexOf(card) + 1; i < draggedPile.size(); i++) {
                draggedCards.add(activePile.getCards().get(i));

            }
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("Out of index");
        }

        for (Card draggedCard : draggedCards){

        draggedCard.getDropShadow().setRadius(20);
        draggedCard.getDropShadow().setOffsetX(10);
        draggedCard.getDropShadow().setOffsetY(10);

        draggedCard.toFront();
        draggedCard.setTranslateX(offsetX);
        draggedCard.setTranslateY(offsetY);
        }

    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        Pile pileFoundation = getValidIntersectingPile(card, foundationPiles);
        //TODO
        if (pile != null) {
            isMoveValid(card, pile);
            handleValidMove(card, pile);
            autoFlip(tableauPiles);
        } else if (pileFoundation != null) {
            isMoveValid(card, pileFoundation);
            handleValidMove(card, pileFoundation);
            autoFlip(tableauPiles);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };


    //Restart buttons's event handler
    private EventHandler<MouseEvent> restartGame = e -> {
        getChildren().clear();
        tableauPiles.clear();
        deck = Card.createNewDeck();
        initPiles();
        Collections.shuffle(deck);
        dealCards();
        System.out.println("You've restarted the game!");
    };

    public boolean isGameWon() {
        if(stockPile.isEmpty() ||
            discardPile.isEmpty() ||
            tableauPiles.isEmpty()){
            return true; }
        return false;
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        Collections.shuffle(deck);
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        while (!discardPile.isEmpty()) {
            Card temp = discardPile.getTopCard();
            temp.flip();
            temp.moveToPile(stockPile);
        }
        System.out.println("Stock refilled from discard pile.");
    }


    public boolean isMoveValid(Card card, Pile destPile) {
        //TODO
        if (destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.TABLEAU))
            if (card.getRank().equals(Rank.KING)) {
                return true;
            }

        if (!destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (Card.isOppositeColor(card, destPile.getTopCard())
                    && destPile.getTopCard().getRank().getRankNum() == card.getRank().getRankNum() + 1)
                return true;
        }
        if (destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (card.getRank().equals(Rank.ACE)) {
                return true;
            }
        }
        if (!destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (Card.isSameSuit(card, destPile.getTopCard())
                    && destPile.getTopCard().getRank().getRankNum() == card.getRank().getRankNum() - 1) {
                return true;
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);


        //Positioning restart button
        restartButton.setLayoutX(20);
        restartButton.setLayoutY(20);
        getChildren().add(restartButton);
        restartButton.setOnMouseClicked(restartGame);



        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();
        for (int i = 0; i < tableauPiles.size(); i++) {
            Pile pile = tableauPiles.get(i);
            if (i > 0) {
                for (int j = 0; j < i + 1; j++) {
                    Card cardBeingPlaced = deckIterator.next();
                    pile.addCard(cardBeingPlaced);
                    addMouseEventHandlers(cardBeingPlaced);
                    cardBeingPlaced.setContainingPile(pile);
                    if (j == i) {
                        cardBeingPlaced.flip();
                    }
                    getChildren().add(cardBeingPlaced);
                }
            } else {
                Card cardBeingPlaced = deckIterator.next();
                pile.addCard(cardBeingPlaced);
                addMouseEventHandlers(cardBeingPlaced);
                cardBeingPlaced.setContainingPile(pile);
                cardBeingPlaced.flip();
                getChildren().add(cardBeingPlaced);
            }
        }
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    private void autoFlip(List<Pile> piles) {
        for (Pile pile : piles) {
            Card topCard = pile.getTopCard();
            if (topCard != null && topCard.isFaceDown()) {
                topCard.flip();
            }
        }
    }

    /**
     *
     */
    public void restartGame() {

    }
}
