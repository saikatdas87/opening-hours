# Opening Hours

A program that takes JSON-formatted opening hours of a restaurant
as an input and outputs hours in more human readable format.

### Input
    Input JSON consist of keys indicating days of a week and corresponding opening hours as
    values. One JSON file includes data for one restaurant.
    {
    <dayofweek>: <opening hours>
    <dayofweek>: <opening hours>
    ...
    }
    <dayofweek>: monday / tuesday / wednesday / thursday / friday / saturday / sunday
    <opening hours>: an array of objects containing opening hours. Each object consist of
    two keys:
    ● type: open or close
    ● value: opening / closing time as UNIX time (1.1.1970 as a date),
    e.g. 32400 = 9 AM, 37800 = 10.30 AM,
    max value is 86399 = 11.59:59 PM
    
### Example
    on Mondays a restaurant is open from 9 AM to 8 PM
    {
     "monday" : [
     {
     "type" : "open",
     "value" : 32400
     },
     {
     "type" : "close",
     "value" : 72000
     }
     ],
     ….
    }
    -------------------
      Output example in 12-hour clock format:
      Monday: 8 AM - 10 AM, 11 AM - 6 PM
      Tuesday: Closed
      Wednesday: 11 AM - 6 PM
      Thursday: 11 AM - 6 PM
      Friday: 11 AM - 9 PM
      Saturday: 11 AM - 9 PM
      Sunday: Closed

### Special cases
    ● If a restaurant is closed the whole day, an array of opening hours is empty.
    ○ “tuesday”: [] means a restaurant is closed on Tuesdays
    ● A restaurant can be opened and closed multiple times during the same day,
    ○ E.g. on Mondays from 9 AM - 11 AM and from 1 PM to 5 PM
    ● A restaurant might not be closed during the same day
    ○ E.g. a restaurant is opened on a Sunday evening and closed on early
    Monday morning. In that case sunday-object includes only the opening time.
    Closing time is part of the monday-object.
    ○ When printing opening hours which span between multiple days, closing time
    is always a part of the day when a restaurant was opened (e.g. Sunday 8 PM
    - 1 AM)
    {
     "friday" : [
     {
     "type" : "open",
     "value" : 64800
     }
     ],
     “saturday”: [
     {
     "type" : "close",
     "value" : 3600
     },
     {
     "type" : "open",
     "value" : 32400
     },
     {
     "type" : "close",
     "value" : 39600
     },
     {
     "type" : "open",
     "value" : 57600
     },                                                         
     {
     "type" : "close",
     "value" : 82800
     }
     ]
    }
    
    A restaurant is open:
      Friday: 6 PM - 1 AM
      Saturday: 9 AM -11 AM, 4 PM - 11 PM
    
    Please note it is assumed that a restaurant at most closes at very next day.
    If restaurant closes next to next day here it is not handled.  
## Used technologies

* [Play Framework: 2.7.2](https://www.playframework.com/documentation/2.7.x/Home)
* [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (recommend version 1.8 or higher)
* [scala](https://www.scala-lang.org/download/)

### Let's get started,

* clone this repository.
* let sbt install all dependencies
* configure the locale and output text format in application.conf file which you can find in below path
```
    ├── /conf/
    │     ├── application.conf
```
To access the application at http://localhost:9000/opening-hours/toHuman then please follow below commands 

* Use any of the following [SBT](http://www.scala-sbt.org/) commands.

```
    sbt clean           # Clean existing build artifacts

    sbt run             # Run the rest api

    sbt test            # Run both unit and integration tests
```
## Complete Directory Layout

```
├── /app/                                       # The backend source (controllers, models, services)
│     └── /controllers/                         # Backend controllers
│           └── OpeningHoursController.scala    # Controller to process data and send back in a readable format
│         /models/*                             # Contains all models used in application
│         /services/*                           # Contains all services including data presentaion and preparation logics
│         /validations/*                        # Contains all validation related functions
│         /exceptions/*                         # Application specific exception
│     ├── application.conf                      # Play application configuratiion file.
│     ├── logback.xml                           # Logging configuration
│     └── routes                                # Routes definition file
├── /logs/                                      # Log directory
│     └── opening-hours.log                     # Application log file
├── /project/                                   # Contains project build configuration and plugins
│     ├── build.properties                      # Marker for sbt project
│     └── plugins.sbt                           # SBT plugins declaration
├── /test/                                      # Contains unit tests of backend sources
├── /conf/                                      # Configurations files and other non-compiled resources (on classpath)
├── build.sbt                                   # Play application SBT configuration
├── LICENSE                                     # License Agreement file
├── README.md                                   # Application user guide
```      
## API Definitions

The api receives input (type and example explained in the beginning of readme) then formats to human readable text as configured.
For any invalid/malformed input application returns BadRequest. 

```
PUT     /opening-hours/toHuman      controllers.OpeningHoursController.convertToHumanReadableText()            

accepts      Content-Type: application/json
returns      Content-Type: text/plain
```                                                                                                

## A better Input format ?

The application is provided with integration test to test different input scenarios and test outputs.
There can be a better way to represent the input though considering a few assumptions.

1. to define multiple opening and closing pairs we can use arrays instead of multiple objects.
    ```
    {    
     "monday" :
         {
         "open" : [3600, 36000]
         "close" : [32400, 86399]
         },
     ..... .
    }
    ```  
2. Maybe this application is consumed in different locations.
 In that case possible locale as input(instead of config) will return the output formatted in local language.
 
 ```
     {    
      "monday" :
          {
          "open" : [3600, 36000]
          "close" : [32400, 86399]
          },
      ..... .,
      "locale": "en"  
     }
 ```

There are a lot of assumptions about what could be wrong with this data and on what cases we can recover instead of throwing an exception and returning bad request.
There are few edge cases which can be real complex such as when restaurant closes next to next day or later. This can be handled with maybe a different format easily.
```
    {    
     "monday" :
         {
         "open" : [3600, 36000]
         "close" : [32400, 86399]
         },
     "tuesday": 
        {
            "allday" : true
        },
     ..... .
    }
    ```
