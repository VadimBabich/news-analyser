## News analyser

### Task description

The task involves the development of two separate Java SE 8 programs: a "mock news feed" and a "news analyser". Several instances of the mock news feed will be run simultaneously, each connecting to the same news analyser.

The mock news feed should periodically generate messages containing random news item.

The news analyser should handle the messages from the news feeds and periodically display a short summary about news items that are considered "interesting“.

### Mock news feed

* Creates a persistent TCP connection to the news analyser and periodically sends news items over that connection. Each news item should be comprised of a headline and a priority.
* The headline of a news item should be a random combination of three to five words from the following list: up, down, rise, fall, good, bad, success, failure, high, low, über, unter.
* The priority of a news item should be an integer within the range [0..9]. News messages with higher priority should be generated with less probability than those with lower priority.
* The frequency of news items being emitted by the feed should be configurable via a Java property.

### News analyser
* Listens on a TCP port for connections from mock news feeds and receives news item messages.
* Inspects the news item headlines and decides whether they are overall positive or negative. If more than 50% of words in the headline are positive ("up", "rise", "good", "success", "high" or "über"), the news item as a whole is considered positive. Negative news items are ignored by the analyser.
* Every 10 seconds, the news analyser should output to the console:
    - the count of positive news items seen during the last 10 seconds
    - the unique headlines of up to three of the highest-priority positive news items seen during the last 10 seconds
    
## Getting Start

This section contains instructions on running localy.

Only **Java 8** supported (ByteBuffer problem) 

This application is a "**mock news feed**" and "**news analyser**" depending on the application argument - **newsFeedMode**. By default it is "**news analyzer**".
1. Compile the Application.
    
    `mvn clean install -DskipTests`
    
1. Start the Application.

    `java -jar target/news-subscriber-1.0-SNAPSHOT-jar-with-dependencies.ja`
    
    usage available arguments:  
    ```  
    usage: java -jar news-subscriber-{version}.jar [-help][-newsFeedMode] [-port] [-host] [-analyserIntervalSec]
                [-frequencyNews]
     -analyserIntervalSec <arg>   Every {analyserIntervalSec} seconds, the news analyser should output to the console.
     -frequencyNews <arg>         The delay in ms between of news items being emitted by the feed. default - 100ms
     -help                        print this message
     -host <arg>                  Binding server host.
     -newsFeedMode                Launch the app as a new mock news feed
     -port <arg>                  Listener port, this port is used to establish connections by publishers.
    
    
    Shutting down ...
   ```
   how to pass arguments to the application:
   
   `java -jar target/tree-radius-1.0.0-SNAPSHOT-jar-with-dependencies.jar -port 1234 -analyserIntervalSec 10`
   
   example:
   
   1. start application as "news analyser" - `java -jar news-subscriber-1.0-SNAPSHOT-jar-with-dependencies.jar -port 1234`.
   1. start application as "mock news feed" - `java -jar news-subscriber-1.0-SNAPSHOT-jar-with-dependencies.jar -port 1234 -newsFeedMode`
   
           ``` 
            bash-3.2$ java -jar news-subscriber-1.0-SNAPSHOT-jar-with-dependencies.jar -port 1234
            initializing..
  
            2021-04-26T17:10:19.990314
            The count of positive news items seen during the last 10 seconds: 0
            the unique headlines of up to three of the highest-priority positive news items seen during the last 10 seconds:
	
            2021-04-26T17:10:29.994837
            The count of positive news items seen during the last 10 seconds: 44
            the unique headlines of up to three of the highest-priority positive news items seen during the last 10 seconds:
    	        9 - success high down
    	        8 - up good rise unter
    	        8 - unter rise success
   
            2021-04-26T17:10:39.993165
            The count of positive news items seen during the last 10 seconds: 59
            the unique headlines of up to three of the highest-priority positive news items seen during the last 10 seconds:
   	            9 - up success good success
   	            9 - up bad high über good
   	            9 - success failure up
            ``` 