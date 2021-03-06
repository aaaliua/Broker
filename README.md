[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Broker-red.svg?style=flat)](https://android-arsenal.com/details/1/1256) [![Raizlabs Repository](http://img.shields.io/badge/Raizlabs%20Repository-1.0.0-blue.svg?style=flat)](https://github.com/Raizlabs/maven-releases)

# Broker

A façade between executing requests and creating them. The library provides an interface for creating requests, but delegates the actual execution to ```RequestExecutors```. It also generates REST services for you using **annotation processing**.


## Getting Started

### Remotely

Add the repo as a maven url to your classpath:

```

buildscript {
      repositories {
        maven { url "https://raw.github.com/Raizlabs/maven-releases/master/releases" }
      }
}

```

Add this line to your build.gradle, using the [apt plugin](https://bitbucket.org/hvisser/android-apt) and the 
[AARLinkSources](https://github.com/xujiaao/AARLinkSources) plugin:

```groovy

dependencies {
  apt 'com.raizlabs.android:Broker-Compiler:1.0.0'
  aarLinkSources 'com.raizlabs.android:Broker-Compiler:1.0.0:sources@jar'
  compile 'com.raizlabs.android:Broker-Core:1.0.0'
  aarLinkSources 'com.raizlabs.android:Broker-Core:1.0.0:sources@jar'
  compile 'com.raizlabs.android:Broker: 1.0.0'
  aarLinkSources 'com.raizlabs.android:Broker:1.0.0:sources@jar'

}


```

### Locally: 

Add the ```request_project_prefix``` to your ```gradle.properties``` file, to elminate the need to fork and change the build.gradle of the project.

Add these lines to your build.gradle:

```groovy

  dependencies {
    apt project(request_project_prefix + "Broker-Compiler")
    compile project(request_project_prefix + "Broker-Core")
    compile project(request_project_prefix + "Broker")
  }

```

## Usage

### Running a simple request

Requests "google.com" and expects to receive a JSON response. The ```Request.Builder``` class when calling ```build()``` returns a read-only ```Request``` object. To run the request, call ```execute()```.

```java

private RequestExecutor<String> mRequestExecutor = new VolleyExecutor();

private void someMethod(){
  new Request.Builder<String>(mRequestExecutor)
        .provider(new SimpleUrlProvider("http://www.google.com/")
        .responseHandler(new SimpleJsonResponseHandler())
        .build(mRequestCallback).execute();
}

private RequestCallback<JSONObject> mRequestCallback = new RequestCallback<JSONObject>() {
        @Override
        public void onRequestDone(JSONObject data) {
           // Success
        }

        @Override
        public void onRequestError(Throwable error, String stringError) {
            // There was an error, stringError is string rep of the error
        }
    };

```

### Request and Request.Builder

This is the main object that we use to execute our requests. It requires the following:

1. ```UrlProvider``` : an interface that makes it easy to specify a method, base url, and end url. Simple implementation is the ```SimpleUrlProvider```. 
2. ```RequestExecutor``` : actually handles the request and is up to the executor how the request is run. 
3. A predefined ``ResponseType`` that must match the ```ResponseHandler```'s return type.

Supports:

1. Custom contentTypes
2. Adding a body to the request
3. Url Params
4. Request headers
5. Adding metadata to attach to the specific request

### REST Interfaces

This library supports annotation processing for generating ```$RestService``` classes that contain all of the code of constructing the intended request. This is similar to [Retrofit](http://square.github.io/retrofit/), however that library uses reflection to generate requests, causing it to be very __slow__. We use annotation processing since it is **transparent** (can read the code that is generated), and as fast as native when executed. 

Each REST interface must be an __interface__ since the generated code has a base class.

#### Simple example

For this example we are purely making up URLs and data. For the specified class to build correctly, we 
need to define 3 annotations:

```@RestService```: Specify either a baseUrl or baseUrlResId for the service
```@RequestExecutor```: Tells what ```RequestExecutor``` to create for this service to use. The default is a shared ```VolleyExecutor```. 
```@ResponseHandler```: Defines the default ```ResponseHandler``` class to use if none is specified for each ```@Method```.


```java

@RestService(baseUrl = "https://www.google.com")
@RequestExecutor(VolleyExecutor.class)
@ResponseHandler(SimpleJsonResponseHandler.class)
public interface SimpleRestService {

}

```

Next we define each ```@Method``` we want to generate from this interface. 

```@Method```: Defines a url (that's appended to the end of the base url), what HTTP method to use (use ```@Method``` constants), and static HTTP headers. Each must contain a ```RequestCallback``` and return void, or return ```Request``` without the ```RequestCallback``` to be valid.

Within the function parameters, we must use annotations to specify what parameter goes where:

```@Endpoint```: Marks the parameter as corresponding to a bracket-enclosed piece of the ```@Method``` url. This enables dynamic endpointing. The name of the variable MUST match the name in the URL. 

```@Header```: The parameter is a request header with a specified static key. 

```@Metadata```: passes the parameter as a tag or piece of information about the request. Can only be one per method.

```@Param```: Marks the parameter as a url parameter where the value is URL encoded by default. 

```@ResponseHandler```:  allows for a different response handler for this method than the default in the class. **Warning:** using non-default will create a new one for each specified using reflection. 

```@Body```: The parameter is the body to the request. It must be a String, ```InputStream```, or ```File```. 


```java

    @Method(url = "/users/{userName}/{password}",
            headers = {@Header(name = "MAC", value = "OS")})
    public void getUsers(@Endpoint String password, @Endpoint String userName, @Header("User-Agent") String userAgent, RequestCallback<JSONObject> requestCallback);

    @Method(url = "/users/{firstName}", method = Method.PUT)
    public void putFirstName(@Endpoint String firstName, @Body String requestBody, RequestCallback<JSONObject> requestCallback);

    @Method(url = "/hello/{goodBye}", method = Method.DELETE)
    public void deleteGoodbye(@Param("myNameIs") String what,
                              @Param("jasonSays") String yeah,
                              @Endpoint String goodBye,
                              @Metadata double flack,
                              RequestCallback<JSONObject> requestCallback);

    @Method(url = "/hello/yep")
    @ResponseHandler(SimpleJsonArrayResponseHandler.class)
    public void getYep(RequestCallback<JSONArray> requestCallback);
}

```

### UrlProvider
It enables enums and other classes to provide a url for the request in a standardized fashion.

#### Example

This is an example, where we define a base url that the other enum objects use in combination with its own defined endpoint. Works very well with REST APIs, or swapping between different endpoints.

```java

private enum AppUrlProvider implements UrlProvider {

        DEV {
            @Override
            public String getUrl() {
                return "dev/config/appConfig.json";
            }
        },
        LIVE {
            @Override
            public String getUrl() {
                return "live/config/appConfig.json";
            }
        };

        @Override
        public String getBaseUrl() {
            return "https://www.someurl.com/";
        }

        @Override
        public Request.Method getMethod() {
            return Request.Method.GET;
        }


    }

```

### RequestCallback

The main response interface that gets called when the request finishes. ```onRequestDone(ResponseType response)``` is for a success and ```onRequestError(Throwable error, String stringError)``` is meant for failures. The parameter of this callback **MUST** match the return type of the ```ResponseHandler```.

#### Example

```java

private RequestCallback<AppConfig> mRequestCallback = new RequestCallback<AppConfig>() {
        @Override
        public void onRequestDone(AppConfig appFeatureControl) {
            if(appFeatureControl != null) {
                Toast.makeText(MainActivity.this, "SUCCESS", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onRequestError(Throwable error, String stringError) {
            Toast.makeText(MainActivity.this, stringError, Toast.LENGTH_SHORT).show();
        }
    };

```

### RequestExecutor

This the main interface by which a request is executed. Any library that we use for networking, we should create a **Request** executor to plug into this module.

Override the ```execute(Request request)``` method and handle the data that is passed in through the **Request** object.

### ResponseHandlers

ResponseHandlers define how to convert a response into another. It's ```ResponseType``` should match the ```RequestCallback```'s type-param. 

#### Example

Using our other library, [Parser](https://github.com/Raizlabs/Parser) (which also uses annotation processing, for parsing data into objects) this response handler converts the string data into an ```AppConfig``` object. 

Example of converting a string into an ```AppConfig``` object. 

```java

private class ParserResponseHandler implements ResponseHandler<String, AppConfig> {

        @Override
        public AppConfig processResponse(String s) {
                return ParserHolder.parse(AppConfig.class, new JSONObject(s));
        }
    }

```
