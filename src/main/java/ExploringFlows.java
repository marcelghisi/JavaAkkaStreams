import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.Int;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class ExploringFlows {
    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create(Behaviors.empty(),"actorSystem");
        Source<Integer, NotUsed> numbers = Source.range(1,200);

        Flow<Integer,Integer,NotUsed> filterFlow = Flow.of(Integer.class).filter(value -> value % 17 == 0);
        Flow<Integer, Integer, NotUsed> mapConcatFlow = Flow.of(Integer.class).mapConcat(value -> {
            List<Integer> results = List.of(value, value +1, value + 2);
            return results;
        });
        Sink<Integer, CompletionStage<Done>> printSink = Sink.foreach(System.out::println);
        numbers.via(filterFlow).via(mapConcatFlow).to(printSink).run(actorSystem);
    }
}
