import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class SimpleStream {

    public static void main(String[] args) {
        //Source<Integer, NotUsed> source = Source.range(1,10);

        List<String> names = List.of("James","Jill","Simon","Saly","Denise","David");

        Source<String, NotUsed> source = Source.from(names);

        Flow<Integer,String, NotUsed> flow = Flow.of(Integer.class).map(value -> "The next value is " + value);
        Flow<String,String, NotUsed> stringFlow = Flow.of(String.class).map(value -> "The next value is " + value);
        Sink<String, CompletionStage<Done>> sink = Sink.foreach(value -> {
            System.out.println(value);
        }) ;

        RunnableGraph<NotUsed> graph = source.via(stringFlow).to(sink);

        ActorSystem actorSystem = ActorSystem.create(Behaviors.empty(), "actorSystem");

        graph.run(actorSystem);
    }
}
