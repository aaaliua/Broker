# Request

Provides a standard way to run and handle requests. It enables easy swapping out of request libraries using **RequestExecutors** 


## Getting Started

Including in your project: 

1. Clone this repo into /ProjectRoot/Libraries/
2. Add this line to your settings.gradle:

```groovy

  include ':Libraries:Request'

```

3. Add this line to your build.gradle:

```groovy

  dependencies {
    compile project(':Libraries:Request');
  }

```

## Usage

### Running a simple request

Requests "google.com" and expects to receive a JSON response.

```java

private RequestExecutor<String> mRequestExecutor = new VolleyExecutor();

private void someMethod(){
  new Request.Builder<String>(mRequestExecutor)
        .provider(new SimpleUrlProvider("http://www.google.com/")
        .responseHandler(new SimpleJsonResponseHandler())
        .execute(mRequestCallback);
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
1. **UrlProvider**
2. **RequestExecutor**
3. A predefined **ResponseType** that matches the **ResponseHandler**'s return type.

Supports:
1. Custom contentTypes
2. Adding a body to the request
3. Url Params
4. Request headers
5. Adding metadata to attach to the specific request


### UrlProvider
It enables enums and other classes to provide a url for the request in a standardized fashion.

#### Example

This is an example from Costco, where we define a base url that the other enum objects use in combination with its own defined endpoint. Works very well with REST APIs.

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
            return "https://mobilecontent.costco.com/";
        }

        @Override
        public Request.Method getMethod() {
            return Request.Method.GET;
        }


    }

```

### RequestExecutor

This the main interface by which a request is executed. Any library that we use for networking, we should create a **Request** executor to plug into this module.

Override the ```execute(Request request)``` method and handle the data that is passed in through the **Request** object.

