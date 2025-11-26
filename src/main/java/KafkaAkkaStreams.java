import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.SourceShape;
import akka.stream.UniformFanInShape;
import akka.stream.javadsl.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Comparator;


public class KafkaAkkaStreams {

    static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {

        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "my-system");
        // Settings
        ConsumerSettings<String, String> settings =
                ConsumerSettings.create(system, new StringDeserializer(), new StringDeserializer())
                        .withBootstrapServers("localhost:9092")
                        .withGroupId("sensor-group")
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // One source per sensor topic
        Source<ConsumerRecord<String, String>, Consumer.Control> sensorA =
                Consumer.plainSource(settings, Subscriptions.topics("sensorA-events"));
        Source<ConsumerRecord<String, String>, Consumer.Control> sensorB =
                Consumer.plainSource(settings, Subscriptions.topics("sensorB-events"));
        Source<ConsumerRecord<String, String>, Consumer.Control> sensorC =
                Consumer.plainSource(settings, Subscriptions.topics("sensorC-events"));
        // Convert JSON → SensorEvent
        Flow<ConsumerRecord<String, String>, SensorEvent, NotUsed> parseEvent =
                Flow.<ConsumerRecord<String, String>>create()
                        .map(record -> parseJson(record.value()));
        // Merge three sensor streams
        Source<SensorEvent, NotUsed> merged =
                Source.fromGraph(
                        GraphDSL.create(builder -> {

                            // Create merge shape for 3 input streams
                            final UniformFanInShape<SensorEvent, SensorEvent> merge =
                                    builder.add(Merge.create(3));

                            // Convert each source into a graph stage (shape)
                            final SourceShape<SensorEvent> a = builder.add(sensorA.via(parseEvent));
                            final SourceShape<SensorEvent> b = builder.add(sensorB.via(parseEvent));
                            final SourceShape<SensorEvent> c = builder.add(sensorC.via(parseEvent));

                            // Connect shapes to merge inlet
                            builder.from(a).toInlet(merge.in(0));
                            builder.from(b).toInlet(merge.in(1));
                            builder.from(c).toInlet(merge.in(2));

                            return SourceShape.of(merge.out());
                        })
                );

        // Windowing + ordering by timestamp
        Source<SensorEvent, NotUsed> ordered =
                merged
                        .groupedWithin(200, Duration.ofMillis(500))
                        .mapConcat(list -> {
                            list.sort(Comparator.comparingLong(e -> e.timestamp));
                            return list;
                        });
        // Output
        ordered.to(Sink.foreach(event ->
                System.out.println(event.sensorId + " → " + event.timestamp + " → " + event.value)
        )).run(system);
    }


    private static SensorEvent parseJson(String json) {
        try {
            return mapper.readValue(json, SensorEvent.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
