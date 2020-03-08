package com.redhat.developers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import com.redhat.developers.data.GameMessage;
import com.redhat.developers.util.Scorer;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.reactivex.Flowable;

@ApplicationScoped
public class LeaderBoardProducer {

  Logger logger = Logger.getLogger(LeaderBoardProducer.class.getName());

  @Inject
  Scorer scorer;

  @Inject
  Jsonb jsonb;

  private List<GameMessage> scoringMessages = new ArrayList<>();

  @PostConstruct
  public void init() {
    logger.info("Setting up test data");
    try {
      this.scoringMessages =
          scorer.fromFile(this.getClass().getResourceAsStream("/data.json"));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error loading test data", e);
    }
  }

  @Outgoing("demo2")
  public Flowable<String> leaderBoardProducerSg() {
    List<String> messages = this.scoringMessages.stream()
        .filter(skm -> skm.getPlayer().getCreationServer().equals("sg"))
        .map(skm -> jsonb.toJson(skm))
        .collect(Collectors.toList());
    return Flowable.fromIterable(messages);
  }

  @Outgoing("ny1")
  public Flowable<String> leaderBoardProducerLon() {
    List<String> messages = this.scoringMessages.stream()
        .filter(skm -> skm.getPlayer().getCreationServer().equals("nyc"))
        .map(skm -> jsonb.toJson(skm))
        .collect(Collectors.toList());
    return Flowable.fromIterable(messages);
  }
}
