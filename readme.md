#Scala Stomp client
   

##To use in java:
###MavenDependencies:
	<dependency>
	  <groupId>org.fusesource.stomp</groupId>
	  <artifactId>scomp</artifactId>
	  <version>1.0.0</version>
	</dependency>
	<dependency>
	  <groupId>com.typesafe.akka</groupId>
	  <artifactId>akka-actor_2.10</artifactId>
	  <version>2.1.2</version>
	</dependency>
 
###Example:

	import akka.dispatch.ExecutionContexts;
	import akka.dispatch.OnSuccess;
	import org.apache.activemq.apollo.stomp.Stomp;
	import org.apache.activemq.apollo.stomp.StompFrame;
	import org.fusesource.stomp.scomp.StompClient;
	import scala.Function1;
	import scala.Option;
	import scala.concurrent.ExecutionContext;
	import scala.concurrent.Future;
	import scala.runtime.AbstractFunction1;
	import scala.runtime.BoxedUnit;

	import java.util.concurrent.Executors;
	import java.util.concurrent.atomic.AtomicReference;


	public class MyStompClient {

	  private  StompClient client;

	  public MyStompClient() throws Exception {

	    System.out.println("Starting...");
	    ExecutionContext ec = ExecutionContexts.fromExecutorService(Executors.newFixedThreadPool(5));

	    client = new StompClient();
	    client.connect("localhost", 61613, "admin", "password");

	    // local responder required to simulate the server responding to requests
	    AtomicReference<Function1<StompFrame, BoxedUnit>> f = new AtomicReference<Function1<StompFrame, BoxedUnit>>(new AbstractFunction1<StompFrame, BoxedUnit>() {
	      public BoxedUnit apply(StompFrame frame) {
	        String replyTo = frame.header(Stomp.REPLY_TO()).toString();
	        client.send(replyTo, "Hello: " + frame.content().utf8(), Option.<String>empty(), false);
	        return BoxedUnit.UNIT;
	      }
	    });

	    client.subscribe("/queue/test", Option.<Function1<StompFrame, BoxedUnit>>apply(f.get()));

		  // requester, awaits a future response
	    for (int i = 1; i <= 100; i++) {
	      Future<StompFrame> future = client.sendReceive("/queue/test", "test message " + i);
	      future.onSuccess(new OnSuccess<StompFrame>() {
	        public final void onSuccess(StompFrame frame) {
	          System.out.println(frame.content().utf8() + " on " + Thread.currentThread());
	        }
	      }, ec);
	    }
	    Thread.sleep(500);

	    client.disconnect();
	    System.out.println("Done");
	    System.exit(0);
	  }

	  public static void main(String[] args) throws Exception {
	     new MyStompClient();
	  }
	}

## To use in Scala:
### SBT Dependencies
	libraryDependencies += "org.fusesource.stomp" % "scomp" % "1.0.0"
				
### Scala Code:
	import org.apache.activemq.apollo.stomp._;
	import org.fusesource.stomp.scomp.StompClient;
	import scala.concurrent.duration._
	import scala.concurrent.ExecutionContext.Implicits.global

	val timeOut: Duration = 100.milliseconds
	val client = new StompClient
	client.connect("localhost", 61613, "admin", "password")

	//test responder for the server side
	client.subscribe("/queue/test", listener = Some((frame) => {
	  val replyTo = frame.header(REPLY_TO)
	  client.send(destination = replyTo.toString, text = "Hello " + frame.content.utf8)
	}))

	// requester, awaits a future response
	for (i <- 1 to msgNum) {
	  val future = client.sendReceive(destination = "/queue/test", text = "test message" + i)
	  future onComplete {
	    case Success(frame) => {
	      println(frame.content().utf8() + " on " + Thread.currentThread());
	    }
	    case Failure(exception) =>
	  }
	  Await.result(future, timeOut)
	}
	client.disconnect()

