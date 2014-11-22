package com.raizlabs.android.request.volley;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.Volley;
import com.raizlabs.android.request.Request;
import com.raizlabs.android.request.RequestCallback;
import com.raizlabs.android.request.RequestConfig;
import com.raizlabs.android.request.RequestExecutor;
import com.raizlabs.android.request.RequestUtils;

/**
 * Description:
 */
public class VolleyExecutor implements RequestExecutor<String> {

    /**
     * Default timeout set to 15 seconds
     */
    static final int sSOCKET_TIMEOUT_MS = 15000;

    private RequestQueue mQueue;

    private HttpClientStack mStack;

    /**
     * This defines the retry policy for all requests on this executor. This is a default retry policy with
     * a timeout of 15 seconds (since 2.5 seconds for default is pretty quick).
     */
    private RetryPolicy mRetryPolicy = new DefaultRetryPolicy(
            sSOCKET_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

    /**
     * Sets a custom stack for us to use in order to manage cookies.
     * @param stack
     */
    public void setStack(HttpClientStack stack) {
        mStack = stack;
    }

    /**
     * Defines a custom {@link com.android.volley.RetryPolicy} for volley.
     * @param retryPolicy
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
    }

    @Override
    public void execute(final Request request) {
        if(mQueue == null) {
            if(mStack == null) {
                mQueue = Volley.newRequestQueue(RequestConfig.getContext());
            } else {
                mQueue = Volley.newRequestQueue(RequestConfig.getContext(), mStack);
            }
        }

        Response.Listener<String> stringListener = new Response.Listener<String>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onResponse(String s) {
                RequestCallback requestCallback = request.getCallback();
                if(requestCallback != null) {
                    requestCallback.onRequestDone(request.getResponseHandler().processResponse(s));
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String errStr = "";
                try {
                    errStr = new String(volleyError.networkResponse.data);
                } catch (Throwable ignored) {
                    ignored.printStackTrace();
                }
                RequestCallback callback = request.getCallback();
                if (callback != null) {
                    callback.onRequestError(volleyError, errStr);
                }
            }
        };

        String url = request.getUrl();
        Request.Method method = request.getMethod();
        if(method.equals(Request.Method.GET)) {
            url = RequestUtils.formatURL(url, request.getParams());
        }

        com.android.volley.Request volleyRequest = new VolleyRequest(request, method.ordinal(), url, stringListener, errorListener);
        volleyRequest.setRetryPolicy(mRetryPolicy);
        mQueue.add(volleyRequest);
    }

    @Override
    public void cancelRequest(String tag, final Request request) {
        if(mQueue != null) {
            if(tag != null && !tag.equals("")) {
                mQueue.cancelAll(tag);
            } else{
                mQueue.cancelAll(new RequestQueue.RequestFilter() {
                    @Override
                    public boolean apply(com.android.volley.Request<?> volleyRequest) {
                        String url = RequestUtils.formatURL(request.getUrl(), request.getParams());
                        String url2 = volleyRequest.getUrl();
                        return url.equalsIgnoreCase(url2);
                    }
                });
            }
        }
    }

    @Override
    public void cancelAllRequests() {
        if(mQueue != null) {
            mQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(com.android.volley.Request<?> request) {
                    return true;
                }
            });
        }
    }
}